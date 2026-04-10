package minkyu307.spring_ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * assistant 메시지별 RAG 출처를 저장하는 엔티티.
 */
@Entity
@Table(
	name = "chat_answer_source",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_chat_answer_source_message_source",
		columnNames = {"conversation_id", "message_timestamp", "source_key"}),
	indexes = {
		@Index(name = "idx_chat_answer_source_conversation_ts", columnList = "conversation_id, message_timestamp")
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAnswerSource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "conversation_id", nullable = false, length = 36)
	private String conversationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "conversation_id",
		referencedColumnName = "id",
		insertable = false,
		updatable = false,
		foreignKey = @ForeignKey(
			name = "fk_chat_answer_source_conversation",
			foreignKeyDefinition = "FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id) ON UPDATE CASCADE ON DELETE CASCADE"))
	private ChatConversation conversation;

	@Column(name = "message_timestamp", nullable = false)
	private Instant messageTimestamp;

	@Column(name = "source_type", nullable = false, length = 64)
	private String sourceType;

	@Column(name = "label", nullable = false, columnDefinition = "text")
	private String label;

	@Column(name = "href", columnDefinition = "text")
	private String href;

	@Column(name = "source_key", nullable = false, columnDefinition = "text")
	private String sourceKey;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	/**
	 * conversation_id + assistant timestamp 기준 출처 레코드를 생성한다.
	 */
	public ChatAnswerSource(
		String conversationId,
		Instant messageTimestamp,
		String sourceType,
		String label,
		String href,
		String sourceKey) {
		this.conversationId = conversationId;
		this.messageTimestamp = messageTimestamp;
		this.sourceType = sourceType;
		this.label = label;
		this.href = href;
		this.sourceKey = sourceKey;
		this.createdAt = Instant.now();
	}
}
