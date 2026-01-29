package minkyu307.spring_ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Spring AI 기본 spring_ai_chat_memory 테이블과 매핑되는 Entity.
 * 테이블 구조: conversation_id, content, type, timestamp (복합 PK 사용)
 */
@Entity
@Table(name = "spring_ai_chat_memory")
@IdClass(ChatMemoryId.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatMemoryEntity {

	@Id
	@Column(name = "conversation_id", nullable = false, length = 36)
	private String conversationId;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Id
	@Column(name = "type", nullable = false, length = 10)
	private String type;

	@Id
	@Column(name = "timestamp", nullable = false)
	private Instant timestamp;
}
