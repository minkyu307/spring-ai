package minkyu307.spring_ai.dto;

/**
 * 단일 URL 문서를 Vector DB로 적재하기 위한 요청 DTO.
 */
public record RagUrlIngestRequest(
		String url
) {
}

