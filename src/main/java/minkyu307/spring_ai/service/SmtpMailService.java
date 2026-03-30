package minkyu307.spring_ai.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import minkyu307.spring_ai.dto.MailSendRequest;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

/**
 * SMTP 기반 메일 발송 구현체.
 */
public class SmtpMailService implements MailService {

    private static final String DEFAULT_FROM_ADDRESS = "no-reply@spring-ai.local";

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public SmtpMailService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    /**
     * 요청 데이터를 SMTP 서버로 즉시 발송한다.
     */
    @Override
    public void send(MailSendRequest request) {
        validateRequest(request);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress(request));
        message.setTo(request.to());
        message.setSubject(request.subject());
        message.setText(request.body());
        mailSender.send(message);
    }

    /**
     * 요청 데이터를 메일 전용 스레드 풀에서 발송한다.
     */
    @Override
    @Async("mailTaskExecutor")
    public CompletableFuture<Void> sendAsync(MailSendRequest request) {
        send(request);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 필수 발송 항목 유효성을 검증한다.
     */
    private void validateRequest(MailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("메일 요청 데이터가 비어 있습니다.");
        }
        if (!StringUtils.hasText(request.to())) {
            throw new IllegalArgumentException("수신자 이메일이 비어 있습니다.");
        }
        if (!StringUtils.hasText(request.subject())) {
            throw new IllegalArgumentException("메일 제목이 비어 있습니다.");
        }
        if (!StringUtils.hasText(request.body())) {
            throw new IllegalArgumentException("메일 본문이 비어 있습니다.");
        }
    }

    private String resolveFromAddress(MailSendRequest request) {
        return resolveCurrentUserEmail()
            .or(() -> Optional.ofNullable(request.from())
                .filter(StringUtils::hasText)
                .map(String::strip))
            .orElse(DEFAULT_FROM_ADDRESS);
    }

    private Optional<String> resolveCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails userDetails) || !StringUtils.hasText(userDetails.getUsername())) {
            return Optional.empty();
        }

        return userRepository.findById(userDetails.getUsername().strip())
            .map(User::getEmail)
            .filter(StringUtils::hasText)
            .map(String::strip);
    }

}
