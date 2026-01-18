package minkyu307.spring_ai.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 방식의 채팅 메모리 서비스
 * 애플리케이션 재시작 시 데이터 손실
 */
@Service
public class InMemoryChatMemoryService {

	// 대화 ID별로 메시지 목록 저장
	private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();
	
	// 최대 메시지 보관 개수
	private static final int MAX_MESSAGES = 20;

	/**
	 * 사용자 메시지 추가
	 */
	public void addUserMessage(String conversationId, String content) {
		List<Message> messages = conversations.computeIfAbsent(conversationId, k -> new ArrayList<>());
		messages.add(new UserMessage(content));
		trimMessages(conversationId);
	}

	/**
	 * AI 응답 메시지 추가
	 */
	public void addAssistantMessage(String conversationId, String content) {
		List<Message> messages = conversations.computeIfAbsent(conversationId, k -> new ArrayList<>());
		messages.add(new AssistantMessage(content));
		trimMessages(conversationId);
	}

	/**
	 * 대화 기록 조회
	 */
	public List<Message> getMessages(String conversationId) {
		return new ArrayList<>(conversations.getOrDefault(conversationId, new ArrayList<>()));
	}

	/**
	 * 특정 대화 삭제
	 */
	public void clearConversation(String conversationId) {
		conversations.remove(conversationId);
	}

	/**
	 * 모든 대화 삭제
	 */
	public void clearAll() {
		conversations.clear();
	}

	/**
	 * 메시지 수가 최대치를 넘으면 오래된 메시지 삭제
	 */
	private void trimMessages(String conversationId) {
		List<Message> messages = conversations.get(conversationId);
		if (messages != null && messages.size() > MAX_MESSAGES) {
			// 오래된 메시지 삭제 (최근 MAX_MESSAGES개만 유지)
			int toRemove = messages.size() - MAX_MESSAGES;
			messages.subList(0, toRemove).clear();
		}
	}

	/**
	 * 현재 저장된 대화 수 조회
	 */
	public int getConversationCount() {
		return conversations.size();
	}

	/**
	 * 특정 대화의 메시지 수 조회
	 */
	public int getMessageCount(String conversationId) {
		return conversations.getOrDefault(conversationId, new ArrayList<>()).size();
	}
}
