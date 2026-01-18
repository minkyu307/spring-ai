package minkyu307.spring_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Hello 페이지를 위한 기본 컨트롤러
 */
@Controller
public class HelloController {

	@GetMapping("/")
	public String index() {
		return "redirect:/chat";
	}

	@GetMapping("/hello")
	public String hello(
			@RequestParam(value = "name", defaultValue = "World") String name,
			Model model) {
		model.addAttribute("name", name);
		model.addAttribute("message", "Welcome to Spring AI!");
		return "hello";
	}
}
