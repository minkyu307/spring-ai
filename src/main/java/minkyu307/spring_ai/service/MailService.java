package minkyu307.spring_ai.service;

import java.util.concurrent.CompletableFuture;
import minkyu307.spring_ai.dto.MailSendRequest;

/**
 * 메일 발송 공통 인터페이스.
 */
public interface MailService {

    /**
     * 메일을 동기 방식으로 발송한다.
     */
    void send(MailSendRequest request);

    /**
     * 메일을 비동기 방식으로 발송한다.
     */
    CompletableFuture<Void> sendAsync(MailSendRequest request);
}
