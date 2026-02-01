package minkyu307.spring_ai.dto;

/**
 * RAG 문서 관리 화면에서 사용하는 문서 요약 DTO. // vector_store(청크) → docId 단위로 그룹핑
 */
public record RagDocumentListItemDto(
		String docId,
		String title,
		String filename,
		String source,
		String ingestedAt,
		long chunkCount
) {
}

