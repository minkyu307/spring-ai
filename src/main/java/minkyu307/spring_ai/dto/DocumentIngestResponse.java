package minkyu307.spring_ai.dto;

/**
 * Vector DB 문서 적재 결과 DTO.
 */
public record DocumentIngestResponse(
		int chunksIngested
) {
}

