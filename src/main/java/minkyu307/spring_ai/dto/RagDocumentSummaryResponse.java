package minkyu307.spring_ai.dto;

/**
 * 문서 요약 응답 DTO. // docId 단위 저장 요약 조회/생성 결과 전달
 */
public record RagDocumentSummaryResponse(
		String docId,
		String summary,
		boolean cached,
		String summarizedAt
) {
}

