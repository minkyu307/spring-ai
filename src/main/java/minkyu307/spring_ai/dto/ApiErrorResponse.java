package minkyu307.spring_ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * API 에러 응답 DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
		String code,
		String message,
		Object details,
		String traceId,
		Instant timestamp
) {
}

