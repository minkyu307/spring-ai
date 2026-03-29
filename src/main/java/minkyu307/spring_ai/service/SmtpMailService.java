package minkyu307.spring_ai.service;

import java.util.concurrent.CompletableFuture;
import minkyu307.spring_ai.dto.MailSendRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;

/**
 * SMTP 기반 메일 발송 구현체.
 */
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpMailService(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * 요청 데이터를 SMTP 서버로 즉시 발송한다.
     */
    @Override
    public void send(MailSendRequest request) {
        validateRequest(request);
        SimpleMailMessage message = new SimpleMailMessage();
        String resolvedFromAddress = StringUtils.hasText(request.from())
            ? request.from().strip()
            : fromAddress;
        message.setFrom(resolvedFromAddress);
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

}
