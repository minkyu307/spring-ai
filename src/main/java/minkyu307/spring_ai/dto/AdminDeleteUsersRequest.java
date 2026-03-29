package minkyu307.spring_ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 관리자 사용자 삭제 요청 DTO.
 */
public record AdminDeleteUsersRequest(
    @NotNull(message = "삭제 대상 사용자 목록은 필수입니다.")
    @NotEmpty(message = "삭제할 사용자를 한 명 이상 선택하세요.")
    List<@NotNull(message = "사용자 식별자는 null일 수 없습니다.") String> loginIds
) {
}
