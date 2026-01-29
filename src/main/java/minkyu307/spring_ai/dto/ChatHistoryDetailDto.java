package minkyu307.spring_ai.dto;

import java.util.List;

/**
 * 특정 대화의 상세 정보를 담는 DTO
 */
public record ChatHistoryDetailDto(
	String conversationId,  // 대화 ID
	List<ChatMessageDto> messages  // 메시지 목록
) {
}
