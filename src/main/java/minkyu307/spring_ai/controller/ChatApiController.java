package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.ChatHistoryDetailDto;
import minkyu307.spring_ai.dto.ChatHistoryDto;
import minkyu307.spring_ai.service.ChatService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 채팅 API를 처리하는 REST 컨트롤러
 */
@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatApiController {

	private final ChatService chatService;

	public ChatApiController(ChatService chatService) {
		this.chatService = chatService;
	}

	/**
	 * AJAX로 AI 응답을 받아오는 API 엔드포인트
	 */
	@PostMapping("/message")
	public ResponseEntity<Map<String, String>> sendMessage(@RequestBody Map<String, String> request) {
		try {
			String conversationId = request.get("conversationId");
			String userMessage = request.get("message");
			
			// conversationId가 없으면 기본값 사용
			if (conversationId == null || conversationId.isEmpty()) {
				conversationId = "default";
			}
			
			String aiResponse = chatService.chat(conversationId, userMessage);
			
			return ResponseEntity.ok(Map.of(
				"success", "true",
				"response", aiResponse
			));
		} catch (Exception e) {
			return ResponseEntity.ok(Map.of(
				"success", "false",
				"error", e.getMessage()
			));
		}
	}

	/**
	 * 모든 채팅 히스토리 목록 조회
	 */
	@GetMapping("/histories")
	public ResponseEntity<List<ChatHistoryDto>> getAllHistories() {
		try {
			List<ChatHistoryDto> histories = chatService.findAllHistories();
			return ResponseEntity.ok(histories);
		} catch (Exception e) {
			log.error("Error getting all histories", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	/**
	 * 특정 대화의 메시지 목록 조회
	 */
	@GetMapping("/histories/{conversationId}")
	public ResponseEntity<ChatHistoryDetailDto> getHistoryMessages(@PathVariable String conversationId) {
		try {
			ChatHistoryDetailDto history = chatService.findHistoryMessages(conversationId);
			return ResponseEntity.ok(history);
		} catch (Exception e) {
			log.error("Error getting history messages", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
}
