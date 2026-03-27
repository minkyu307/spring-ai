package minkyu307.spring_ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import minkyu307.spring_ai.dto.ApiErrorResponse;
import minkyu307.spring_ai.error.ApiErrorCode;
import minkyu307.spring_ai.error.ApiErrorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 직접 가입 사용자 form 로그인 기반 보안 설정.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final RequestMatcher API_MATCHER = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

    /**
     * /api/** Ajax 요청의 세션 만료 시 302 대신 401 JSON 반환 — 프론트에서 감지 후 alert + 리다이렉트.
     */
    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint(
        ApiErrorFactory apiErrorFactory,
        ObjectMapper objectMapper
    ) {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) -> {
            ApiErrorResponse errorResponse = apiErrorFactory.create(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.SESSION_EXPIRED,
                null,
                null,
                request
            );
            writeErrorResponse(response, objectMapper, HttpStatus.UNAUTHORIZED.value(), errorResponse);
        };
    }

    /**
     * /api/** Ajax 요청의 인가 실패 시 403 JSON 반환.
     */
    @Bean
    public AccessDeniedHandler apiAccessDeniedHandler(
        ApiErrorFactory apiErrorFactory,
        ObjectMapper objectMapper
    ) {
        return (request, response, accessDeniedException) -> {
            ApiErrorResponse errorResponse = apiErrorFactory.create(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.FORBIDDEN_OPERATION,
                "접근 권한이 없습니다.",
                null,
                request
            );
            writeErrorResponse(response, objectMapper, HttpStatus.FORBIDDEN.value(), errorResponse);
        };
    }

    /**
     * 인증/인가 및 form 로그인 설정.
     */
    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        AuthenticationEntryPoint apiAuthenticationEntryPoint,
        AccessDeniedHandler apiAccessDeniedHandler,
        TraceIdFilter traceIdFilter
    ) throws Exception {
        http
            .addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/login/**", "/error",
                    "/.well-known/**").permitAll()
                .requestMatchers(
                    "/app", "/app/**",
                    "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/login-meta", "/api/auth/csrf", "/api/auth/signup").permitAll()
                .anyRequest().authenticated())
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**"))
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/app/note", true))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/app/login?logout")
                .invalidateHttpSession(true))
            // /api/** 경로는 세션 만료 시 302 리다이렉트 대신 401 JSON 반환
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    apiAuthenticationEntryPoint,
                    API_MATCHER
                )
                .defaultAccessDeniedHandlerFor(
                    apiAccessDeniedHandler,
                    API_MATCHER
                )
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeErrorResponse(
        HttpServletResponse response,
        ObjectMapper objectMapper,
        int status,
        ApiErrorResponse body
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
