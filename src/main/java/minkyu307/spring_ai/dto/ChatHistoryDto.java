package minkyu307.spring_ai.dto;

import java.time.Instant;

/**
 * 채팅 히스토리 정보를 담는 DTO
 */
public record ChatHistoryDto(
	String conversationId,  // 대화 ID
	String title,  // 히스토리 제목 (첫 메시지 미리보기)
	Instant lastUpdated,  // 마지막 업데이트 시간
	int messageCount  // 메시지 개수
) {
}
