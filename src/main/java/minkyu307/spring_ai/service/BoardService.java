package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.CommentDto;
import minkyu307.spring_ai.dto.PostDetailDto;
import minkyu307.spring_ai.dto.PostListItemDto;
import minkyu307.spring_ai.entity.Comment;
import minkyu307.spring_ai.entity.Post;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.repository.CommentRepository;
import minkyu307.spring_ai.repository.PostRepository;
import minkyu307.spring_ai.repository.UserRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 게시판 서비스. 게시글·댓글 생성/조회/삭제, 작성자 본인만 삭제 가능. ROLE_USER 권한은 Anon 표시.
 */
@Service
public class BoardService {

	private static final String ROLE_USER = "ROLE_USER";
	private static final String ANON = "Anon";

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;

	public BoardService(PostRepository postRepository, CommentRepository commentRepository,
			UserRepository userRepository) {
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
		this.userRepository = userRepository;
	}

	/** 현재 로그인 사용자 엔티티 조회 */
	private User getCurrentUser() {
		String loginId = SecurityUtils.getCurrentLoginId();
		return userRepository.findById(loginId)
			.orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + loginId));
	}

	@Transactional
	public Long createPost(String title, String content) {
		User writer = getCurrentUser();
		Post post = new Post(writer, title, content);
		return postRepository.save(post).getPostId();
	}

	@Transactional(readOnly = true)
	public List<PostListItemDto> findAllPosts() {
		return postRepository.findAllByOrderByUpdatedAtDesc().stream()
			.map(this::toListItemDto)
			.toList();
	}

	@Transactional(readOnly = true)
	public Optional<PostDetailDto> findPostById(Long postId) {
		return postRepository.findById(postId).map(this::toDetailDto);
	}

	@Transactional
	public void updatePost(Long postId, String title, String content) {
		String loginId = SecurityUtils.getCurrentLoginId();
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("글이 없습니다: " + postId));
		if (!post.getWriter().getLoginId().equals(loginId)) {
			throw new IllegalArgumentException("본인 글만 수정할 수 있습니다.");
		}
		post.setTitle(title != null ? title.strip() : "");
		post.setContent(content != null ? content.strip() : "");
		post.setUpdatedAt(Instant.now());
		postRepository.save(post);
	}

	@Transactional
	public void deletePost(Long postId) {
		String loginId = SecurityUtils.getCurrentLoginId();
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("글이 없습니다: " + postId));
		if (!post.getWriter().getLoginId().equals(loginId)) {
			throw new IllegalArgumentException("본인 글만 삭제할 수 있습니다.");
		}
		postRepository.delete(post);
	}

	@Transactional
	public Long addComment(Long postId, String content) {
		User writer = getCurrentUser();
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("글이 없습니다: " + postId));
		Comment comment = new Comment(post, writer, content);
		return commentRepository.save(comment).getCommentId();
	}

	@Transactional(readOnly = true)
	public List<CommentDto> findCommentsByPostId(Long postId) {
		return commentRepository.findByPostPostIdOrderByUpdatedAtAsc(postId).stream()
			.map(this::toCommentDto)
			.toList();
	}

	@Transactional
	public void updateComment(Long commentId, String content) {
		String loginId = SecurityUtils.getCurrentLoginId();
		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다: " + commentId));
		if (!comment.getWriter().getLoginId().equals(loginId)) {
			throw new IllegalArgumentException("본인 댓글만 수정할 수 있습니다.");
		}
		comment.setContent(content != null ? content.strip() : "");
		comment.setUpdatedAt(Instant.now());
		commentRepository.save(comment);
	}

	@Transactional
	public void deleteComment(Long commentId) {
		String loginId = SecurityUtils.getCurrentLoginId();
		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다: " + commentId));
		if (!comment.getWriter().getLoginId().equals(loginId)) {
			throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
		}
		commentRepository.delete(comment);
	}

	/** ROLE_USER 권한이면 Anon, 그 외에는 username 반환 */
	private String displayName(User user) {
		if (user == null) return ANON;
		if (user.getRole() != null && ROLE_USER.equals(user.getRole().getName())) return ANON;
		return user.getUsername() != null ? user.getUsername() : ANON;
	}

	private PostListItemDto toListItemDto(Post p) {
		return new PostListItemDto(
			p.getPostId(),
			p.getTitle(),
			p.getWriter().getLoginId(),
			displayName(p.getWriter()),
			p.getUpdatedAt()
		);
	}

	private PostDetailDto toDetailDto(Post p) {
		List<CommentDto> comments = commentRepository.findByPostPostIdOrderByUpdatedAtAsc(p.getPostId()).stream()
			.map(this::toCommentDto)
			.toList();
		return new PostDetailDto(
			p.getPostId(),
			p.getTitle(),
			p.getContent(),
			p.getWriter().getLoginId(),
			displayName(p.getWriter()),
			p.getUpdatedAt(),
			comments
		);
	}

	private CommentDto toCommentDto(Comment c) {
		return new CommentDto(
			c.getCommentId(),
			c.getWriter().getLoginId(),
			displayName(c.getWriter()),
			c.getContent(),
			c.getUpdatedAt()
		);
	}
}
