package minkyu307.spring_ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Google Gemini AI와 상호작용하는 채팅 서비스
 */
@Service
public class ChatService {

	private final ChatClient chatClient;
	private final InMemoryChatMemoryService chatMemory;

	public ChatService(ChatClient.Builder chatClientBuilder, InMemoryChatMemoryService chatMemory) {
		this.chatClient = chatClientBuilder.build();
		this.chatMemory = chatMemory;
	}

	/**
	 * 대화 ID를 포함한 메시지를 AI에게 전달하고 응답을 받음
	 * 이전 대화 기록을 함께 전달하여 컨텍스트 유지
	 */
	public String chat(String conversationId, String userMessage) {
		// 1. 사용자 메시지를 메모리에 저장
		chatMemory.addUserMessage(conversationId, userMessage);
		
		// 2. 이전 대화 기록 가져오기
		List<Message> history = chatMemory.getMessages(conversationId);
		
		// 3. 전체 대화 기록과 함께 AI에게 요청
		String response = chatClient.prompt()
				.messages(history)
				.call()
				.content();
		
		// 4. AI 응답을 메모리에 저장
		chatMemory.addAssistantMessage(conversationId, response);
		
		return response;
	}
}
