package minkyu307.spring_ai.dto;

/**
 * 파일 업로드 기반 RAG 문서 적재 결과(파일 단위).
 */
public record RagFileIngestResultDto(
		String filename,
		String status,
		int chunksIngested,
		String error
) {
}

