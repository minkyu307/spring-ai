package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.ChatHistoryDetailDto;
import minkyu307.spring_ai.dto.ChatHistoryDto;
import minkyu307.spring_ai.dto.ChatMessageDto;
import minkyu307.spring_ai.entity.ChatConversation;
import minkyu307.spring_ai.repository.ChatConversationRepository;
import minkyu307.spring_ai.repository.ChatMemoryJdbcQueryRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google Gemini AI와 상호작용하는 채팅 서비스 Spring AI 공식 ChatMemory API 사용
 * chat_conversation 으로 사용자별 대화 스코프 및 접근 제어.
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemoryJdbcQueryRepository chatMemoryJdbcQueryRepository;
    private final ChatConversationRepository chatConversationRepository;

    public ChatService(
        ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
        VectorStore vectorStore,
        ChatMemoryJdbcQueryRepository chatMemoryJdbcQueryRepository,
        ChatConversationRepository chatConversationRepository) {
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder()
                .topK(8)
                .similarityThresholdAll()
                .build())
            .build();

        this.chatClient = chatClientBuilder
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                qaAdvisor)
            .build();

        this.chatMemoryJdbcQueryRepository = chatMemoryJdbcQueryRepository;
        this.chatConversationRepository = chatConversationRepository;
    }

    /**
     * 대화 ID를 포함한 메시지를 AI에게 전달하고 응답을 받음. 현재 로그인 사용자(loginId) 기준으로 chat_conversation
     * 검증/생성 후 MessageChatMemoryAdvisor 가 기록 관리.
     */
    public String chat(String conversationId, String userMessage) {
        String loginId = SecurityUtils.getCurrentLoginId();

        chatConversationRepository.findById(conversationId)
            .ifPresentOrElse(
                conv -> {
                    if (!conv.getLoginId().equals(loginId)) {
                        throw new IllegalStateException("다른 사용자의 대화에 접근할 수 없습니다.");
                    }
                },
                () -> {
                    String title = userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage;
                    ChatConversation conv = new ChatConversation(conversationId, loginId, title);
                    chatConversationRepository.save(conv);
                });

        return chatClient.prompt()
            .user(userMessage)
            .advisors(a -> a
                .param(ChatMemory.CONVERSATION_ID, conversationId)
                .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "loginId == '" + loginId + "'"))
            .call()
            .content();
    }

    /**
     * 현재 로그인 사용자의 채팅 히스토리 목록만 조회 (chat_conversation + spring_ai_chat_memory).
     */
    public List<ChatHistoryDto> findAllHistories() {
        String loginId = SecurityUtils.getCurrentLoginId();

        return chatMemoryJdbcQueryRepository.findHistorySummariesByLoginId(loginId).stream()
            .map(summary -> {
                String conversationId = summary.conversationId();
                String firstMessage = summary.firstUserMessage();
                String title = firstMessage != null && firstMessage.length() > 50
                    ? firstMessage.substring(0, 50) + "..."
                    : (firstMessage != null ? firstMessage : "새 대화");
                Instant lastUpdated = summary.lastUpdated() != null
                    ? summary.lastUpdated()
                    : Instant.now();
                int messageCount = Math.toIntExact(summary.messageCount());

                return new ChatHistoryDto(conversationId, title, lastUpdated, messageCount);
            })
            .collect(Collectors.toList());
    }

    /**
     * 특정 대화의 메시지 목록 조회. 해당 대화가 현재 로그인 사용자 소유인지 검증 후 반환.
     * chat_conversation 에 행이 없으면(아직 메시지를 보내지 않은 새 대화) 빈 목록을 반환한다.
     */
    public ChatHistoryDetailDto findHistoryMessages(String conversationId) {
        String loginId = SecurityUtils.getCurrentLoginId();

        boolean isOwnConversation = chatConversationRepository.findByIdAndLoginId(conversationId, loginId).isPresent();
        if (!isOwnConversation) {
            return new ChatHistoryDetailDto(conversationId, List.of());
        }

        List<ChatMemoryJdbcQueryRepository.ChatMemoryMessage> rows = chatMemoryJdbcQueryRepository
            .findMessagesByConversationIdOrderByTimestampAsc(conversationId);

        List<ChatMessageDto> messages = rows.stream()
            .map(row -> {
                String type = row.type();
                String role = "USER".equals(type) ? "user"
                    : "ASSISTANT".equals(type) ? "assistant"
                        : "system";
                String content = row.content();
                Instant timestamp = row.timestamp() != null
                    ? row.timestamp()
                    : Instant.now();

                return new ChatMessageDto(role, content, timestamp);
            })
            .collect(Collectors.toList());

        return new ChatHistoryDetailDto(conversationId, messages);
    }
}
