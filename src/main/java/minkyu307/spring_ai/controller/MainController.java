package minkyu307.spring_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Hello 페이지를 위한 기본 컨트롤러
 */
@Controller
public class MainController {

	@GetMapping("/")
	public String index() {
		return "redirect:/chat";
	}
}
