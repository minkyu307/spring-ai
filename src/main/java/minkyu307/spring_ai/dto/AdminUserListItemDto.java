package minkyu307.spring_ai.dto;

import java.time.Instant;

/**
 * 관리자 화면 사용자 목록 항목 DTO.
 */
public record AdminUserListItemDto(
    String loginId,
    String username,
    String email,
    String roleName,
    Instant createdAt
) {
}
