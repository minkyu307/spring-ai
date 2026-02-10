package minkyu307.spring_ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 채팅 대화 메타 정보. spring_ai_chat_memory.conversation_id 와 1:1 대응하며,
 * 사용자(login_id)별 접근 제어 및 히스토리 목록 조회에 사용.
 */
@Entity
@Table(name = "chat_conversation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatConversation {

	@Id
	@Column(name = "id", nullable = false, length = 36)
	@EqualsAndHashCode.Include
	private String id;

	@Column(name = "login_id", nullable = false, columnDefinition = "text")
	private String loginId;

	@Column(name = "title", columnDefinition = "text")
	private String title;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	public ChatConversation(String id, String loginId, String title) {
		this.id = id;
		this.loginId = loginId;
		this.title = title;
	}
}
