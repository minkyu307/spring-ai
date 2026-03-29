package minkyu307.spring_ai.config;

import minkyu307.spring_ai.service.MailService;
import minkyu307.spring_ai.service.NoopMailService;
import minkyu307.spring_ai.service.SmtpMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

/**
 * MailService 구현체 선택 규칙을 정의한다.
 */
@Configuration
public class MailServiceConfig {

    private static final String DEFAULT_FROM_ADDRESS = "no-reply@spring-ai.local";

    /**
     * 설정/의존성 상태에 따라 SMTP 또는 Noop 구현체를 선택한다.
     */
    @Bean
    public MailService mailService(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${app.mail.enabled:false}") boolean mailEnabled,
        @Value("${app.mail.from:no-reply@spring-ai.local}") String fromAddress
    ) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            return new NoopMailService();
        }
        String resolvedFromAddress = StringUtils.hasText(fromAddress) ? fromAddress : DEFAULT_FROM_ADDRESS;
        return new SmtpMailService(mailSender, resolvedFromAddress);
    }
}
