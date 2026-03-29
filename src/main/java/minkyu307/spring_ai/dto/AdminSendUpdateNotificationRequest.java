package minkyu307.spring_ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 관리자 업데이트 알림 메일 발송 요청 DTO.
 */
public record AdminSendUpdateNotificationRequest(
    @NotNull(message = "메일 발송 대상 사용자 목록은 필수입니다.")
    @NotEmpty(message = "메일을 보낼 사용자를 한 명 이상 선택하세요.")
    List<@NotNull(message = "사용자 식별자는 null일 수 없습니다.") String> loginIds,
    @NotBlank(message = "업데이트 메일 내용을 입력하세요.")
    String content
) {
}
