package minkyu307.spring_ai.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.AuthMeResponse;
import minkyu307.spring_ai.dto.SignUpRequest;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.error.ApiErrorCode;
import minkyu307.spring_ai.error.ApiException;
import minkyu307.spring_ai.repository.UserRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import minkyu307.spring_ai.service.SignUpService;
import org.springframework.http.HttpStatus;
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
    private final UserRepository userRepository;

    /**
     * 현재 로그인 사용자 식별 정보를 반환한다.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me() {
        String loginId = SecurityUtils.getCurrentLoginId();
        User user = userRepository.findById(loginId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.UNAUTHORIZED,
                "사용자를 찾을 수 없습니다: " + loginId
            ));

        List<String> roles = user.getRole() == null
            ? List.of()
            : List.of(user.getRole().getName());

        return ResponseEntity.ok(new AuthMeResponse(
            true,
            user.getLoginId(),
            user.getEmail(),
            roles
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
