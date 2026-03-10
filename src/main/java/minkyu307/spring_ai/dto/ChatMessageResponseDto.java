package minkyu307.spring_ai.dto;

/**
 * 채팅 메시지 전송 성공 응답 DTO.
 */
public record ChatMessageResponseDto(
	String conversationId,
	String response
) {
}
