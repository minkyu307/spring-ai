package minkyu307.spring_ai.dto;

/**
 * 메일 발송 요청 데이터.
 */
public record MailSendRequest(
    String from,
    String to,
    String subject,
    String body
) {

    /**
     * 발신자 지정이 필요 없는 기존 호출을 위한 생성자.
     */
    public MailSendRequest(String to, String subject, String body) {
        this(null, to, subject, body);
    }
}
