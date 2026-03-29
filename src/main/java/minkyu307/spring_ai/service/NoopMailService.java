package minkyu307.spring_ai.service;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import minkyu307.spring_ai.dto.MailSendRequest;
import org.springframework.scheduling.annotation.Async;

/**
 * 메일 비활성 환경에서 로그만 남기는 구현체.
 */
@Slf4j
public class NoopMailService implements MailService {

    /**
     * 메일 발송 대신 요청 내용을 로그로 기록한다.
     */
    @Override
    public void send(MailSendRequest request) {
        if (request == null) {
            log.info("메일 비활성화 상태 - 요청 데이터가 null 입니다.");
            return;
        }
        int bodyLength = request.body() == null ? 0 : request.body().length();
        log.info(
            "메일 비활성화 상태 - from={}, to={}, subject={}, bodyLength={}",
            request.from(),
            request.to(),
            request.subject(),
            bodyLength
        );
    }

    /**
     * 비동기 호출에서도 동일하게 로그 스텁으로 동작한다.
     */
    @Override
    @Async("mailTaskExecutor")
    public CompletableFuture<Void> sendAsync(MailSendRequest request) {
        send(request);
        return CompletableFuture.completedFuture(null);
    }
}
