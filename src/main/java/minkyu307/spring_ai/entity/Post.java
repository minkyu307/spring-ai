package minkyu307.spring_ai.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 게시글 엔티티. 작성자(writer)는 app_user(login_id) FK, 댓글은 hard delete 시 글 삭제 시 함께 삭제.
 */
@Entity
@Table(name = "board_post")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "post_id", nullable = false, updatable = false)
	@EqualsAndHashCode.Include
	private Long postId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "writer_login_id",
			nullable = false,
			foreignKey = @ForeignKey(
					name = "fk_board_post_writer",
					foreignKeyDefinition = "FOREIGN KEY (writer_login_id) REFERENCES app_user(login_id) ON UPDATE CASCADE ON DELETE CASCADE"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User writer;

	@Column(nullable = false, columnDefinition = "text")
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	@OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	public Post(User writer, String title, String content) {
		this.writer = writer;
		this.title = title;
		this.content = content;
	}
}
