package minkyu307.spring_ai.dto;

import java.time.Instant;
import java.util.List;

/**
 * 채팅 메시지 상세와 assistant 출처 목록을 전달하는 DTO.
 */
public record ChatMessageDto(
	String role,
	String content,
	Instant timestamp,
	List<ChatSourceDto> sources
) {
}
