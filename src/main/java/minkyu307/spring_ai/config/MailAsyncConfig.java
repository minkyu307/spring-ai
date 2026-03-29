package minkyu307.spring_ai.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 메일 발송 비동기 실행기 설정.
 */
@Slf4j
@Configuration
@EnableAsync
public class MailAsyncConfig implements AsyncConfigurer {

    private static final int MAIL_CORE_POOL_SIZE = 2;
    private static final int MAIL_MAX_POOL_SIZE = 8;
    private static final int MAIL_QUEUE_CAPACITY = 200;

    /**
     * 메일 전송 전용 비동기 실행기를 등록한다.
     */
    @Bean(name = "mailTaskExecutor")
    public AsyncTaskExecutor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(MAIL_CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAIL_MAX_POOL_SIZE);
        executor.setQueueCapacity(MAIL_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * @Async 기본 실행기를 mailTaskExecutor로 지정한다.
     */
    @Override
    public Executor getAsyncExecutor() {
        return mailTaskExecutor();
    }

    /**
     * 반환 타입이 void인 비동기 작업 예외를 로깅한다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }

    /**
     * 비동기 미처리 예외를 공통 포맷으로 기록한다.
     */
    private static class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        /**
         * 비동기 예외 상세를 에러 로그로 남긴다.
         */
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error(
                "비동기 작업 예외 - method={}, params={}",
                method.getName(),
                Arrays.toString(params),
                ex
            );
        }
    }
}
