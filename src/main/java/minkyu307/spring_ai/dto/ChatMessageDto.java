package minkyu307.spring_ai.dto;

import java.time.Instant;

/**
 * 채팅 메시지 정보를 담는 DTO
 */
public record ChatMessageDto(
	String role,  // 메시지 역할 (user, assistant, system)
	String content,  // 메시지 내용
	Instant timestamp  // 메시지 시간
) {
}
