package minkyu307.spring_ai.dto;

import java.util.List;

/**
 * 현재 로그인 사용자 정보 응답 DTO.
 */
public record AuthMeResponse(
    boolean authenticated,
    String loginId,
    String email,
    List<String> roles
) {
}
