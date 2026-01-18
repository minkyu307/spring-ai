package minkyu307.spring_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 채팅 페이지 뷰를 처리하는 컨트롤러
 */
@Controller
@RequestMapping("/chat")
public class ChatController {

	/**
	 * 채팅 페이지 표시
	 */
	@GetMapping
	public String chatPage() {
		return "chat";
	}
}
