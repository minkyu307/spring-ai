package minkyu307.spring_ai.controller;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.SignUpRequest;
import minkyu307.spring_ai.security.SecurityUtils;
import minkyu307.spring_ai.service.SignUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React 웹앱 로그인/회원가입용 인증 보조 API.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final SignUpService signUpService;

    /**
     * 현재 로그인 사용자 식별 정보를 반환한다.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        String loginId = SecurityUtils.getCurrentLoginId();
        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "loginId", loginId
        ));
    }

    /**
     * 폼 전송용 CSRF 토큰을 반환한다.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of(
            "parameterName", csrfToken.getParameterName(),
            "headerName", csrfToken.getHeaderName(),
            "token", csrfToken.getToken()
        ));
    }

    /**
     * 로그인 화면 초기 메타 정보를 반환한다.
     */
    @GetMapping("/login-meta")
    public ResponseEntity<Map<String, Object>> loginMeta(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of(
            "csrfParameterName", csrfToken.getParameterName(),
            "csrfToken", csrfToken.getToken()
        ));
    }

    /**
     * 회원가입을 처리한다.
     */
    @PostMapping("/signup")
	public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignUpRequest request) {
		signUpService.signUp(request);
        return ResponseEntity.ok(Map.of("message", "회원가입이 완료되었습니다."));
    }
}
