package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.ChatHistoryDetailDto;
import minkyu307.spring_ai.dto.ChatHistoryDto;
import minkyu307.spring_ai.dto.ChatMessageResponseDto;
import minkyu307.spring_ai.service.ChatService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 채팅 API를 처리하는 REST 컨트롤러
 */
@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

	private final ChatService chatService;

	public ChatApiController(ChatService chatService) {
		this.chatService = chatService;
	}

	/**
	 * AJAX로 AI 응답을 받아오는 API 엔드포인트
	 */
	@PostMapping("/message")
	public ResponseEntity<ChatMessageResponseDto> sendMessage(@RequestBody Map<String, String> request) {
		String conversationId = request.get("conversationId");
		String userMessage = request.get("message");

		if (userMessage == null || userMessage.isBlank()) {
			throw new IllegalArgumentException("message는 필수입니다.");
		}

		if (conversationId == null || conversationId.isBlank()) {
			conversationId = java.util.UUID.randomUUID().toString();
		}

		ChatService.ChatResult result = chatService.chat(conversationId, userMessage);

		return ResponseEntity.ok(new ChatMessageResponseDto(result.conversationId(), result.response()));
	}

	/**
	 * 모든 채팅 히스토리 목록 조회
	 */
	@GetMapping("/histories")
	public ResponseEntity<List<ChatHistoryDto>> getAllHistories() {
		List<ChatHistoryDto> histories = chatService.findAllHistories();
		return ResponseEntity.ok(histories);
	}

	/**
	 * 특정 대화의 메시지 목록 조회
	 */
	@GetMapping("/histories/{conversationId}")
	public ResponseEntity<ChatHistoryDetailDto> getHistoryMessages(@PathVariable String conversationId) {
		ChatHistoryDetailDto history = chatService.findHistoryMessages(conversationId);
		return ResponseEntity.ok(history);
	}

	/**
	 * 특정 대화 삭제 (chat_conversation + spring_ai_chat_memory 메시지 함께 제거)
	 */
	@DeleteMapping("/histories/{conversationId}")
	public ResponseEntity<Void> deleteHistory(@PathVariable String conversationId) {
		chatService.deleteConversation(conversationId);
		return ResponseEntity.noContent().build();
	}
}
