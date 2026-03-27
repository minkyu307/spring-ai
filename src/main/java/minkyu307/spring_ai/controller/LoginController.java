package minkyu307.spring_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * /login 요청을 SPA 로그인 경로로 리다이렉트한다.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "logout", required = false) String logout) {
        StringBuilder redirect = new StringBuilder("redirect:/app/login");
        if (error != null) {
            redirect.append("?error");
        } else if (logout != null) {
            redirect.append("?logout");
        }
        return redirect.toString();
    }
}
