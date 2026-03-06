package minkyu307.spring_ai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.nio.charset.StandardCharsets;

/**
 * Google OIDC 로그인 + 직접 가입 사용자 form 로그인.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * /api/** Ajax 요청의 세션 만료 시 302 대신 401 JSON 반환 — 프론트에서 감지 후 alert + 리다이렉트.
     */
    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"message\":\"세션이 만료되었습니다. 다시 로그인해 주세요.\"}");
        };
    }

    /**
     * 인증/인가, form 로그인, OAuth2 로그인 설정.
     */
    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
        ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/login/**", "/signup", "/oauth2/**", "/error",
                    "/.well-known/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated())
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**"))
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true))
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService)))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(new DelegatingLogoutSuccessHandler(clientRegistrationRepository))
                .invalidateHttpSession(true))
            // /api/** 경로는 세션 만료 시 302 리다이렉트 대신 401 JSON 반환
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    apiAuthenticationEntryPoint(),
                    new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")
                )
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
