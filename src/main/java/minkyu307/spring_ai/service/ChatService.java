package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.ChatHistoryDetailDto;
import minkyu307.spring_ai.dto.ChatHistoryDto;
import minkyu307.spring_ai.dto.ChatMessageDto;
import minkyu307.spring_ai.dto.ChatSourceDto;
import minkyu307.spring_ai.entity.ChatAnswerSource;
import minkyu307.spring_ai.entity.ChatConversation;
import minkyu307.spring_ai.repository.ChatAnswerSourceRepository;
import minkyu307.spring_ai.repository.ChatConversationRepository;
import minkyu307.spring_ai.repository.ChatMemoryJdbcQueryRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Google Gemini AI와 상호작용하는 채팅 서비스 Spring AI 공식 ChatMemory API 사용
 * chat_conversation 으로 사용자별 대화 스코프 및 접근 제어.
 */
@Service
public class ChatService {

    private static final String DEFAULT_CONVERSATION_TITLE = "새 대화";
    private static final int TITLE_SUMMARY_MAX_CHARS = 30;
    private static final int TITLE_SUMMARY_INPUT_MAX_CHARS = 1200;

    private final ChatClient chatClient;
    private final ChatClient titleChatClient;
    private final ChatMemoryJdbcQueryRepository chatMemoryJdbcQueryRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatAnswerSourceRepository chatAnswerSourceRepository;

    public ChatService(
        ChatClient.Builder chatClientBuilder, ChatModel chatModel, ChatMemory chatMemory,
        VectorStore vectorStore,
        ChatMemoryJdbcQueryRepository chatMemoryJdbcQueryRepository,
        ChatConversationRepository chatConversationRepository,
        ChatAnswerSourceRepository chatAnswerSourceRepository) {
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder()
                .topK(5)
                .similarityThresholdAll()
                .build())
            .build();

        // 제목 생성은 대화 메모리와 완전히 분리해 conversation_id=default 저장을 방지한다.
        this.titleChatClient = ChatClient.builder(chatModel).build();
        this.chatClient = chatClientBuilder
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                qaAdvisor)
            .build();

        this.chatMemoryJdbcQueryRepository = chatMemoryJdbcQueryRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.chatAnswerSourceRepository = chatAnswerSourceRepository;
    }

    /**
     * 대화 ID를 포함한 메시지를 AI에게 전달하고 응답을 받음. 현재 로그인 사용자(loginId) 기준으로 chat_conversation
     * 검증/생성 후 MessageChatMemoryAdvisor 가 기록 관리.
     */
    public ChatResult chat(String conversationId, String userMessage) {
        String loginId = SecurityUtils.getCurrentLoginId();
        String normalizedConversationId = normalizeConversationId(conversationId);

        // 이미 존재하는 대화가 다른 사용자 소유이면 새 대화로 대체한다.
        boolean isOtherOwner = chatConversationRepository.findById(normalizedConversationId)
            .map(conv -> !conv.getLoginId().equals(loginId))
            .orElse(false);

        final String resolvedId = isOtherOwner ? java.util.UUID.randomUUID().toString() : normalizedConversationId;

        boolean isNewConversation = chatConversationRepository.findById(resolvedId).isEmpty();
        if (isNewConversation) {
            chatConversationRepository.save(new ChatConversation(resolvedId, loginId, DEFAULT_CONVERSATION_TITLE));
        }

        ChatResponse chatResponse = chatClient.prompt()
            .user(userMessage)
            .advisors(a -> a
                .param(ChatMemory.CONVERSATION_ID, resolvedId)
                .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "loginId == '" + loginId + "'"))
            .call()
            .chatResponse();
        String response = extractResponseText(chatResponse);
        List<ChatSourceDto> sources = extractSourcesFromResponse(chatResponse);
        persistAssistantSources(resolvedId, response, sources);

        if (isNewConversation) {
            updateConversationTitle(resolvedId, loginId, response);
        }

        return new ChatResult(resolvedId, response, List.copyOf(sources));
    }

    public record ChatResult(String conversationId, String response, List<ChatSourceDto> sources) {}

    /**
     * 현재 로그인 사용자 소유의 대화를 삭제한다. chat_conversation 및 spring_ai_chat_memory 메시지를 함께 제거.
     * 타인 소유 대화는 삭제하지 않는다.
     */
    public void deleteConversation(String conversationId) {
        String loginId = SecurityUtils.getCurrentLoginId();
        chatConversationRepository.findByIdAndLoginId(conversationId, loginId)
            .ifPresent(conv -> {
                chatMemoryJdbcQueryRepository.deleteByConversationId(conversationId);
                chatAnswerSourceRepository.deleteByConversationId(conversationId);
                chatConversationRepository.delete(conv);
            });
    }

    /**
     * 현재 로그인 사용자의 채팅 히스토리 목록만 조회 (chat_conversation + spring_ai_chat_memory).
     */
    public List<ChatHistoryDto> findAllHistories() {
        String loginId = SecurityUtils.getCurrentLoginId();

        return chatMemoryJdbcQueryRepository.findHistorySummariesByLoginId(loginId).stream()
            .map(summary -> {
                String conversationId = summary.conversationId();
                String conversationTitle = summary.conversationTitle();
                String title;
                if (conversationTitle != null && !conversationTitle.isBlank()) {
                    title = conversationTitle;
                } else {
                    String firstMessage = summary.firstUserMessage();
                    title = firstMessage != null && firstMessage.length() > 50
                        ? firstMessage.substring(0, 50) + "..."
                        : (firstMessage != null ? firstMessage : DEFAULT_CONVERSATION_TITLE);
                }
                Instant lastUpdated = summary.lastUpdated() != null
                    ? summary.lastUpdated()
                    : Instant.now();
                int messageCount = Math.toIntExact(summary.messageCount());

                return new ChatHistoryDto(conversationId, title, lastUpdated, messageCount);
            })
            .collect(Collectors.toList());
    }

    /**
     * 첫 AI 답변을 기반으로 30자 이내 제목을 생성해 대화 메타 정보에 저장한다.
     */
    private void updateConversationTitle(String conversationId, String loginId, String assistantResponse) {
        String title = summarizeConversationTitle(assistantResponse);
        chatConversationRepository.findByIdAndLoginId(conversationId, loginId)
            .ifPresent(conversation -> {
                conversation.setTitle(title);
                chatConversationRepository.save(conversation);
            });
    }

    /**
     * 첫 AI 답변 텍스트를 요약 모델로 압축하고, 실패 시 응답 원문을 30자로 절단해 폴백한다.
     */
    private String summarizeConversationTitle(String assistantResponse) {
        String normalizedResponse = normalizeSingleLine(assistantResponse);
        if (normalizedResponse.isBlank()) {
            return DEFAULT_CONVERSATION_TITLE;
        }

        try {
            String summary = titleChatClient.prompt()
                .system("""
                    너는 채팅 제목 생성기다.
                    항상 한국어 제목 한 줄만 출력한다.
                    제목은 30자 이내로 작성하고 따옴표/마침표/줄바꿈은 넣지 않는다.
                    """)
                .user("""
                    다음 AI 답변의 핵심을 30자 이내 제목으로 요약해줘.
                    답변:
                    %s
                    """.formatted(limitCodePoints(normalizedResponse, TITLE_SUMMARY_INPUT_MAX_CHARS)))
                .call()
                .content();

            String normalizedSummary = normalizeTitle(summary);
            if (!normalizedSummary.isBlank()) {
                return limitCodePoints(normalizedSummary, TITLE_SUMMARY_MAX_CHARS);
            }
        } catch (Exception ignored) {
            // 제목 요약 실패 시 아래 폴백 제목을 사용한다.
        }

        return limitCodePoints(normalizedResponse, TITLE_SUMMARY_MAX_CHARS);
    }

    /**
     * conversation_id는 공백/기본값(default)일 때 항상 UUID로 재생성한다.
     */
    private static String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        String normalized = conversationId.strip();
        if (ChatMemory.DEFAULT_CONVERSATION_ID.equals(normalized)) {
            return java.util.UUID.randomUUID().toString();
        }
        return normalized;
    }

    /**
     * 줄바꿈/탭을 포함한 공백을 단일 공백으로 정규화하고 앞뒤 공백을 제거한다.
     */
    private static String normalizeSingleLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replaceAll("\\s+", " ").strip();
    }

    /**
     * 제목에 포함될 수 있는 감싸는 따옴표를 제거하고 공백을 정리한다.
     */
    private static String normalizeTitle(String text) {
        String normalized = normalizeSingleLine(text);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.replaceAll("^[\"'`“”‘’]+|[\"'`“”‘’]+$", "").strip();
    }

    /**
     * 문자열을 코드포인트 기준으로 안전하게 절단한다.
     */
    private static String limitCodePoints(String text, int maxCodePoints) {
        if (text == null || text.isBlank()) {
            return DEFAULT_CONVERSATION_TITLE;
        }
        int length = text.codePointCount(0, text.length());
        if (length <= maxCodePoints) {
            return text;
        }
        int end = text.offsetByCodePoints(0, maxCodePoints);
        return text.substring(0, end);
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
        Map<Instant, List<ChatSourceDto>> sourcesByTimestamp = chatAnswerSourceRepository
            .findByConversationIdOrderByMessageTimestampAscIdAsc(conversationId).stream()
            .collect(Collectors.groupingBy(
                ChatAnswerSource::getMessageTimestamp,
                LinkedHashMap::new,
                Collectors.mapping(
                    row -> new ChatSourceDto(row.getSourceType(), row.getLabel(), row.getHref()),
                    Collectors.toList()
                )));

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
                List<ChatSourceDto> sources = "assistant".equals(role)
                    ? sourcesByTimestamp.getOrDefault(timestamp, List.of())
                    : List.of();

                return new ChatMessageDto(role, content, timestamp, sources);
            })
            .collect(Collectors.toList());

        return new ChatHistoryDetailDto(conversationId, messages);
    }

    /**
     * ChatResponse 결과 본문 텍스트를 안전하게 추출한다.
     */
    private static String extractResponseText(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        String text = chatResponse.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    /**
     * QuestionAnswerAdvisor가 반환한 문맥 문서 메타데이터에서 출처 목록을 생성한다.
     */
    private static List<ChatSourceDto> extractSourcesFromResponse(ChatResponse chatResponse) {
        Object rawContext = null;
        if (chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
            rawContext = chatResponse.getResult().getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        }
        if (rawContext == null && chatResponse != null && chatResponse.getMetadata() != null) {
            rawContext = chatResponse.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        }
        if (!(rawContext instanceof List<?> contexts) || contexts.isEmpty()) {
            return List.of();
        }

        Map<String, ChatSourceDto> deduplicated = new LinkedHashMap<>();
        for (Object context : contexts) {
            Map<String, Object> metadata = extractDocumentMetadata(context);
            SourceProjection projection = mapToSource(metadata);
            if (projection == null) {
                continue;
            }
            deduplicated.putIfAbsent(
                projection.sourceKey(),
                new ChatSourceDto(projection.sourceType(), projection.label(), projection.href()));
        }
        return List.copyOf(deduplicated.values());
    }

    /**
     * assistant 최신 메시지 timestamp를 찾아 출처를 별도 테이블에 저장한다.
     */
    private void persistAssistantSources(String conversationId, String assistantResponse, List<ChatSourceDto> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }
        Instant resolvedTimestamp = chatMemoryJdbcQueryRepository
            .findLatestAssistantMessageTimestampByConversationIdAndContent(conversationId, assistantResponse);
        if (resolvedTimestamp == null) {
            resolvedTimestamp = chatMemoryJdbcQueryRepository
                .findLatestAssistantMessageTimestampByConversationId(conversationId);
        }
        if (resolvedTimestamp == null) {
            return;
        }
        Instant messageTimestamp = resolvedTimestamp;

        chatAnswerSourceRepository.deleteByConversationIdAndMessageTimestamp(conversationId, messageTimestamp);
        List<ChatAnswerSource> entities = sources.stream()
            .map(source -> new ChatAnswerSource(
                conversationId,
                messageTimestamp,
                normalizeSourceType(source.sourceType()),
                source.label(),
                source.href(),
                buildStoredSourceKey(source)))
            .collect(Collectors.toList());
        chatAnswerSourceRepository.saveAll(entities);
    }

    /**
     * 후보 문맥 항목에서 Document metadata를 추출한다.
     */
    private static Map<String, Object> extractDocumentMetadata(Object context) {
        if (context instanceof Document document) {
            return document.getMetadata() == null ? Map.of() : document.getMetadata();
        }
        if (context instanceof Map<?, ?> rawMap) {
            Object nestedMetadata = rawMap.get("metadata");
            if (nestedMetadata instanceof Map<?, ?> nestedMap) {
                return castMetadataMap(nestedMap);
            }
            return castMetadataMap(rawMap);
        }
        return Map.of();
    }

    /**
     * Object 기반 맵을 문자열 키의 메타데이터 맵으로 변환한다.
     */
    private static Map<String, Object> castMetadataMap(Map<?, ?> rawMap) {
        if (rawMap == null || rawMap.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> metadata = new HashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null) {
                metadata.put(String.valueOf(key), value);
            }
        });
        return metadata;
    }

    /**
     * 문서 메타데이터를 규칙에 맞는 출처 형식으로 변환한다.
     */
    private static SourceProjection mapToSource(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        String sourceType = normalizeSourceType(metadataString(metadata, "source"));
        if (sourceType.isBlank()) {
            return null;
        }
        if ("upload".equals(sourceType)) {
            String filename = metadataString(metadata, "filename");
            if (filename.isBlank()) {
                return null;
            }
            return new SourceProjection("upload", filename, null, "upload:" + filename.toLowerCase(Locale.ROOT));
        }
        if ("url".equals(sourceType)) {
            String url = firstNonBlank(
                metadataString(metadata, "url"),
                metadataString(metadata, "source"));
            if (!isHttpUrl(url)) {
                return null;
            }
            return new SourceProjection("url", url, url, "url:" + url);
        }
        if ("dooray-wiki".equals(sourceType)) {
            String wikiId = metadataString(metadata, "wikiId");
            String pageId = metadataString(metadata, "pageId");
            if (wikiId.isBlank() || pageId.isBlank()) {
                return null;
            }
            String doorayUrl = "https://ligaccuver.dooray.com/wiki/" + wikiId + "/" + pageId;
            return new SourceProjection(
                "dooray-wiki",
                doorayUrl,
                doorayUrl,
                "dooray-wiki:" + wikiId + ":" + pageId);
        }
        return null;
    }

    /**
     * 출처 저장용 식별 키를 구성한다.
     */
    private static String buildStoredSourceKey(ChatSourceDto source) {
        String sourceType = normalizeSourceType(source.sourceType());
        if ("upload".equals(sourceType)) {
            return "upload:" + firstNonBlank(source.label()).toLowerCase(Locale.ROOT);
        }
        if ("url".equals(sourceType)) {
            return "url:" + firstNonBlank(source.href(), source.label());
        }
        if ("dooray-wiki".equals(sourceType)) {
            return "dooray-wiki:" + firstNonBlank(source.href(), source.label());
        }
        return sourceType + ":" + firstNonBlank(source.label(), source.href());
    }

    /**
     * 소스 타입 문자열을 저장용 표준 값으로 정규화한다.
     */
    private static String normalizeSourceType(String value) {
        if (value == null) {
            return "";
        }
        return value.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * 메타데이터 값을 문자열로 안전하게 꺼낸다.
     */
    private static String metadataString(Map<String, Object> metadata, String key) {
        if (metadata == null || key == null || key.isBlank()) {
            return "";
        }
        Object value = metadata.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value).strip();
    }

    /**
     * 여러 후보 문자열 중 첫 번째 비어있지 않은 값을 반환한다.
     */
    private static String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "";
    }

    /**
     * URL 문자열이 http/https 형식인지 판별한다.
     */
    private static boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    /**
     * 출처 정규화 중간 결과 표현.
     */
    private record SourceProjection(
        String sourceType,
        String label,
        String href,
        String sourceKey
    ) {}
}
