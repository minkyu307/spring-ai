package minkyu307.spring_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 기존 게시판 경로를 React 경로로 리다이렉트한다.
 */
@Controller
@RequestMapping("/board")
public class BoardPageController {

	/**
	 * 게시판 페이지 표시.
	 */
	@GetMapping
	public String boardPage() {
		return "redirect:/app/board";
	}
}
