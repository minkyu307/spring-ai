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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 댓글 엔티티. 글(Post)과 작성자(User) FK, hard delete.
 */
@Entity
@Table(name = "board_comment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_id", nullable = false, updatable = false)
	@EqualsAndHashCode.Include
	private Long commentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "post_id",
			nullable = false,
			foreignKey = @ForeignKey(
					name = "fk_board_comment_post",
					foreignKeyDefinition = "FOREIGN KEY (post_id) REFERENCES board_post(post_id) ON UPDATE CASCADE ON DELETE CASCADE"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "writer_login_id",
			nullable = false,
			foreignKey = @ForeignKey(
					name = "fk_board_comment_writer",
					foreignKeyDefinition = "FOREIGN KEY (writer_login_id) REFERENCES app_user(login_id) ON UPDATE CASCADE ON DELETE CASCADE"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User writer;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public Comment(Post post, User writer, String content) {
		this.post = post;
		this.writer = writer;
		this.content = content;
	}
}
