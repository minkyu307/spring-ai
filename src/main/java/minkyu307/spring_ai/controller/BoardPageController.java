package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 피드백 게시판 페이지 뷰를 처리하는 컨트롤러. 댓글 삭제 버튼 노출 여부용 loginId 전달.
 */
@Controller
@RequestMapping("/board")
public class BoardPageController {

	/**
	 * 게시판 페이지 표시. 인증된 사용자 loginId를 모델에 담아 댓글 삭제 버튼 작성자만 노출에 사용.
	 */
	@GetMapping
	public String boardPage(Model model) {
		try {
			model.addAttribute("loginId", SecurityUtils.getCurrentLoginId());
		} catch (Exception e) {
			model.addAttribute("loginId", "");
		}
		return "board";
	}
}
