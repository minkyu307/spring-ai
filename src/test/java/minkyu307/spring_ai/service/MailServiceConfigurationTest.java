package minkyu307.spring_ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import minkyu307.spring_ai.config.MailAsyncConfig;
import minkyu307.spring_ai.config.MailServiceConfig;
import minkyu307.spring_ai.dto.MailSendRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.aop.framework.AopProxyUtils;

class MailServiceConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MailAsyncConfig.class, MailServiceConfig.class);

    @Test
    void mailDisabledThenNoopServiceIsSelected() throws Exception {
        contextRunner.run(context -> {
            MailService mailService = context.getBean(MailService.class);
            assertTargetClass(context, NoopMailService.class);
            mailService.send(new MailSendRequest("user@test.com", "subject", "body"));
            mailService.sendAsync(new MailSendRequest("user@test.com", "subject", "body"))
                .get(2, TimeUnit.SECONDS);
        });
    }

    @Test
    void mailEnabledAndSenderExistsThenSmtpServiceIsSelected() throws Exception {
        contextRunner
            .withUserConfiguration(MockMailSenderConfig.class)
            .withPropertyValues(
                "app.mail.enabled=true",
                "app.mail.from=from@test.com"
            )
            .run(context -> {
                MailService mailService = context.getBean(MailService.class);
                assertTargetClass(context, SmtpMailService.class);
                JavaMailSender sender = context.getBean(JavaMailSender.class);
                mailService.send(new MailSendRequest("user@test.com", "subject", "body"));
                mailService.sendAsync(new MailSendRequest("user@test.com", "subject", "body"))
                    .get(2, TimeUnit.SECONDS);

                verify(sender, times(2)).send((SimpleMailMessage) any());
            });
    }

    /**
     * 프록시를 포함한 MailService 실제 구현 클래스를 검증한다.
     */
    private void assertTargetClass(ApplicationContext context, Class<?> expectedType) {
        MailService mailService = context.getBean(MailService.class);
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(mailService);
        assertThat(targetClass).isEqualTo(expectedType);
    }

    /**
     * 테스트용 JavaMailSender 목 빈을 제공한다.
     */
    @Configuration(proxyBeanMethods = false)
    static class MockMailSenderConfig {

        /**
         * JavaMailSender 호출 여부를 검증하기 위한 Mockito 목을 생성한다.
         */
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
