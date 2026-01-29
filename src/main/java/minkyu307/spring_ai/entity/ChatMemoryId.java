package minkyu307.spring_ai.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * spring_ai_chat_memory 테이블 복합 PK (Spring AI 기본 테이블에는 id 컬럼이 없음)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatMemoryId implements Serializable {

	private String conversationId;
	private Instant timestamp;
	private String type;
}
