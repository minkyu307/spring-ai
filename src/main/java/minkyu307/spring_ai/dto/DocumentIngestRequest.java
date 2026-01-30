package minkyu307.spring_ai.dto;

import java.util.Map;

/**
 * Vector DB에 적재할 문서 입력 DTO.
 */
public record DocumentIngestRequest(
		String content,
		Map<String, Object> metadata
) {
}

