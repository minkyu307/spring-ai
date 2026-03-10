package minkyu307.spring_ai.dto;

import java.time.Instant;

/**
 * API 에러 응답 DTO.
 */
public record ApiErrorResponse(
		String code,
		int status,
		Instant timestamp,
		String path,
		String error
) {
}

