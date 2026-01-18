package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
