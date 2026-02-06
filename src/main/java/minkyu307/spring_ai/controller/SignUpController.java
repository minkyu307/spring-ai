package minkyu307.spring_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.SignUpRequest;
import minkyu307.spring_ai.service.SignUpService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 회원가입 처리. POST /signup 후 성공 시 /login으로 리다이렉트.
 */
@Controller
@RequestMapping
@RequiredArgsConstructor
public class SignUpController {

    private final SignUpService signUpService;

    @PostMapping("/signup")
    public String signUp(
        @Valid SignUpRequest request,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("signUpRequest", request);
            model.addAttribute("showSignup", true);
            return "login";
        }
        try {
            signUpService.signUp(request);
        } catch (SignUpService.DuplicateLoginIdException e) {
            model.addAttribute("signUpRequest", request);
            model.addAttribute("showSignup", true);
            model.addAttribute("duplicateLoginId", e.getMessage());
            return "login";
        }
        redirectAttributes.addFlashAttribute("signupSuccess", true);
        return "redirect:/login";
    }
}
