package minkyu307.spring_ai.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 주요 엔트리 경로를 인증 상태에 따라 리다이렉트한다.
 */
@Controller
public class MainController {

    /**
     * 루트 및 앱 엔트리 요청을 로그인/노트 경로로 분기한다.
     */
    @GetMapping({"/", "/app", "/app/"})
    public String index(Authentication authentication) {
        return isAuthenticated(authentication) ? "redirect:/app/note" : "redirect:/app/login";
    }

    /**
     * 로그인 엔트리 요청을 인증 상태에 따라 분기한다.
     */
    @GetMapping("/app/login")
    public String appLogin(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/app/note";
        }
        return "forward:/app/index.html";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
