package minkyu307.spring_ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Google Gemini AI와 상호작용하는 채팅 서비스
 * Spring AI 공식 ChatMemory API 사용
 */
@Service
public class ChatService {

	private final ChatClient chatClient;

	public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
		// MessageChatMemoryAdvisor를 사용하여 메모리 자동 관리
		this.chatClient = chatClientBuilder
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.build();
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
}
