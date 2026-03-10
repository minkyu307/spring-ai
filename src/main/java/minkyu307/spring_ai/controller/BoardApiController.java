package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.CommentDto;
import minkyu307.spring_ai.dto.CreateCommentRequest;
import minkyu307.spring_ai.dto.CreatePostRequest;
import minkyu307.spring_ai.dto.PostListItemDto;
import minkyu307.spring_ai.service.BoardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 게시판 REST API. 게시글·댓글 목록/상세/생성/삭제.
 */
@RestController
@RequestMapping("/api/board")
public class BoardApiController {

	private final BoardService boardService;

	public BoardApiController(BoardService boardService) {
		this.boardService = boardService;
	}

	/** 게시글 목록 조회 (최신순) */
	@GetMapping("/posts")
	public ResponseEntity<List<PostListItemDto>> listPosts() {
		List<PostListItemDto> list = boardService.findAllPosts();
		return ResponseEntity.ok(list);
	}

	/** 게시글 상세 조회 (댓글 포함) */
	@GetMapping("/posts/{postId}")
	public ResponseEntity<?> getPost(@PathVariable Long postId) {
		return boardService.findPostById(postId)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/** 게시글 작성 */
	@PostMapping("/posts")
	public ResponseEntity<Map<String, Long>> createPost(@RequestBody CreatePostRequest request) {
		if (request.title() == null || request.title().isBlank()) {
			throw new IllegalArgumentException("제목을 입력하세요.");
		}
		if (request.content() == null || request.content().isBlank()) {
			throw new IllegalArgumentException("내용을 입력하세요.");
		}
		Long postId = boardService.createPost(request.title().strip(), request.content().strip());
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("postId", postId));
	}

	/** 게시글 수정 (본인만) */
	@PutMapping("/posts/{postId}")
	public ResponseEntity<Void> updatePost(@PathVariable Long postId, @RequestBody CreatePostRequest request) {
		if (request.title() == null || request.title().isBlank()) {
			throw new IllegalArgumentException("제목을 입력하세요.");
		}
		if (request.content() == null || request.content().isBlank()) {
			throw new IllegalArgumentException("내용을 입력하세요.");
		}
		boardService.updatePost(postId, request.title().strip(), request.content().strip());
		return ResponseEntity.ok().build();
	}

	/** 게시글 삭제 (본인만) */
	@DeleteMapping("/posts/{postId}")
	public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
		boardService.deletePost(postId);
		return ResponseEntity.noContent().build();
	}

	/** 댓글 목록 조회 */
	@GetMapping("/posts/{postId}/comments")
	public ResponseEntity<List<CommentDto>> listComments(@PathVariable Long postId) {
		List<CommentDto> list = boardService.findCommentsByPostId(postId);
		return ResponseEntity.ok(list);
	}

	/** 댓글 작성 */
	@PostMapping("/posts/{postId}/comments")
	public ResponseEntity<Map<String, Long>> createComment(@PathVariable Long postId, @RequestBody CreateCommentRequest request) {
		if (request.content() == null || request.content().isBlank()) {
			throw new IllegalArgumentException("댓글 내용을 입력하세요.");
		}
		Long commentId = boardService.addComment(postId, request.content().strip());
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("commentId", commentId));
	}

	/** 댓글 수정 (본인만) */
	@PutMapping("/comments/{commentId}")
	public ResponseEntity<Void> updateComment(@PathVariable Long commentId, @RequestBody CreateCommentRequest request) {
		if (request.content() == null || request.content().isBlank()) {
			throw new IllegalArgumentException("댓글 내용을 입력하세요.");
		}
		boardService.updateComment(commentId, request.content().strip());
		return ResponseEntity.ok().build();
	}

	/** 댓글 삭제 (본인만) */
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
		boardService.deleteComment(commentId);
		return ResponseEntity.noContent().build();
	}
}
