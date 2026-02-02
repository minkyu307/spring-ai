package minkyu307.spring_ai.dto;

/**
 * 단일 URL 문서 Vector DB 적재 결과 DTO.
 */
public record RagUrlIngestResponse(
		String docId,
		String url,
		String detectedType,
		int documentsRead,
		int chunksIngested,
		String title
) {
}

