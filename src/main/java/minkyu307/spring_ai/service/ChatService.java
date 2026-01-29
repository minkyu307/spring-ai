package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.ChatHistoryDetailDto;
import minkyu307.spring_ai.dto.ChatHistoryDto;
import minkyu307.spring_ai.dto.ChatMessageDto;
import minkyu307.spring_ai.entity.ChatMemoryEntity;
import minkyu307.spring_ai.repository.ChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google Gemini AI와 상호작용하는 채팅 서비스
 * Spring AI 공식 ChatMemory API 사용
 * Spring Data JPA를 사용하여 히스토리 조회
 */
@Service
public class ChatService {

	private final ChatClient chatClient;
	private final ChatMemoryRepository chatMemoryRepository;

	public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
			ChatMemoryRepository chatMemoryRepository) {
		// MessageChatMemoryAdvisor를 사용하여 메모리 자동 관리
		this.chatClient = chatClientBuilder
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.build();
		this.chatMemoryRepository = chatMemoryRepository;
	}

	/**
	 * 대화 ID를 포함한 메시지를 AI에게 전달하고 응답을 받음
	 * MessageChatMemoryAdvisor가 자동으로 대화 기록 관리
	 */
	public String chat(String conversationId, String userMessage) {
		return chatClient.prompt()
				.user(userMessage)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.content();
	}

	/**
	 * 모든 채팅 히스토리 목록 조회
	 * Spring Data JPA를 사용하여 조회
	 */
	public List<ChatHistoryDto> findAllHistories() {
		return chatMemoryRepository.findHistorySummaries().stream()
				.map(projection -> {
					String conversationId = projection.getConversationId();
					String firstMessage = projection.getFirstUserMessage();
					String title = firstMessage != null && firstMessage.length() > 50 
							? firstMessage.substring(0, 50) + "..." 
							: (firstMessage != null ? firstMessage : "새 대화");
					Instant lastUpdated = projection.getLastUpdated() != null 
							? projection.getLastUpdated() 
							: Instant.now();
					int messageCount = projection.getMessageCount().intValue();
					
					return new ChatHistoryDto(conversationId, title, lastUpdated, messageCount);
				})
				.collect(Collectors.toList());
	}

	/**
	 * 특정 대화의 메시지 목록 조회
	 * Spring Data JPA를 사용하여 조회
	 */
	public ChatHistoryDetailDto findHistoryMessages(String conversationId) {
		List<ChatMemoryEntity> entities = chatMemoryRepository
				.findByConversationIdOrderByTimestampAsc(conversationId);
		
		List<ChatMessageDto> messages = entities.stream()
				.map(entity -> {
					String type = entity.getType();
					String role = "USER".equals(type) ? "user" 
							: "ASSISTANT".equals(type) ? "assistant" 
							: "system";
					String content = entity.getContent();
					Instant timestamp = entity.getTimestamp() != null 
							? entity.getTimestamp() 
							: Instant.now();
					
					return new ChatMessageDto(role, content, timestamp);
				})
				.collect(Collectors.toList());
		
		return new ChatHistoryDetailDto(conversationId, messages);
	}
}
