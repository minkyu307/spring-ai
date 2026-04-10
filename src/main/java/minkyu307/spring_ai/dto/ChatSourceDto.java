package minkyu307.spring_ai.dto;

/**
 * AI 답변 출처 정보를 표현하는 DTO.
 */
public record ChatSourceDto(
	String sourceType,
	String label,
	String href
) {
}
