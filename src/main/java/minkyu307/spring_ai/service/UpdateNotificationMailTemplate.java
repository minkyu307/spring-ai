package minkyu307.spring_ai.service;

import org.springframework.stereotype.Component;

/**
 * DocuSearch 업데이트 알림 메일 본문 템플릿을 생성한다.
 */
@Component
public class UpdateNotificationMailTemplate {

    /**
     * 발신자와 동적 내용을 바탕으로 메일 본문을 렌더링한다.
     */
    public String render(String senderEmail, String content) {
        String safeSenderEmail = senderEmail == null ? "" : senderEmail.strip();
        String safeContent = content == null ? "" : content.strip();
        return """
            보내는 사람: %s

            DocuSearch 업데이트 알림

            %s

            감사합니다.
            """.formatted(safeSenderEmail, safeContent);
    }
}
