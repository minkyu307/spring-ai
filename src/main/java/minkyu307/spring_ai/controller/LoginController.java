package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.SignUpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 커스텀 로그인 페이지 제공. GET /login 시 Thymeleaf 로그인 템플릿 반환.
 */
@Controller
public class LoginController {

    @Value("${app.security.oauth2.google.visible:true}")
    private boolean oauth2GoogleVisible;

    @GetMapping("/login")
    public String loginPage(Model model) {
        if (!model.containsAttribute("signUpRequest")) {
            model.addAttribute("signUpRequest", new SignUpRequest());
        }
        if (!model.containsAttribute("showSignup")) {
            model.addAttribute("showSignup", false);
        }
        model.addAttribute("oauth2GoogleVisible", oauth2GoogleVisible);
        return "login";
    }
}
