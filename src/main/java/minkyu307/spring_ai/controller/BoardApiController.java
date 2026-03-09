package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.ApiErrorResponse;
import minkyu307.spring_ai.dto.CommentDto;
import minkyu307.spring_ai.dto.CreateCommentRequest;
import minkyu307.spring_ai.dto.CreatePostRequest;
import minkyu307.spring_ai.dto.PostListItemDto;
import minkyu307.spring_ai.service.BoardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 게시판 REST API. 게시글·댓글 목록/상세/생성/삭제.
 */
@RestController
@RequestMapping("/api/board")
@Slf4j
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
	public ResponseEntity<?> createPost(@RequestBody CreatePostRequest request) {
		try {
			if (request.title() == null || request.title().isBlank()) {
				return ResponseEntity.badRequest().body(new ApiErrorResponse("제목을 입력하세요."));
			}
			if (request.content() == null || request.content().isBlank()) {
				return ResponseEntity.badRequest().body(new ApiErrorResponse("내용을 입력하세요."));
			}
			Long postId = boardService.createPost(request.title().strip(), request.content().strip());
			return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("postId", postId));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Create post failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("글 저장 중 오류가 발생했습니다."));
		}
	}

	/** 게시글 수정 (본인만) */
	@PutMapping("/posts/{postId}")
	public ResponseEntity<?> updatePost(@PathVariable Long postId, @RequestBody CreatePostRequest request) {
		try {
			if (request.title() == null || request.title().isBlank()) {
				return ResponseEntity.badRequest().body(new ApiErrorResponse("제목을 입력하세요."));
			}
			if (request.content() == null || request.content().isBlank()) {
				return ResponseEntity.badRequest().body(new ApiErrorResponse("내용을 입력하세요."));
			}
			boardService.updatePost(postId, request.title().strip(), request.content().strip());
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Update post failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("글 수정 중 오류가 발생했습니다."));
		}
	}

	/** 게시글 삭제 (본인만) */
	@DeleteMapping("/posts/{postId}")
	public ResponseEntity<?> deletePost(@PathVariable Long postId) {
		try {
			boardService.deletePost(postId);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Delete post failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("글 삭제 중 오류가 발생했습니다."));
		}
	}

	/** 댓글 목록 조회 */
	@GetMapping("/posts/{postId}/comments")
	public ResponseEntity<List<CommentDto>> listComments(@PathVariable Long postId) {
		List<CommentDto> list = boardService.findCommentsByPostId(postId);
		return ResponseEntity.ok(list);
	}

	/** 댓글 작성 */
	@PostMapping("/posts/{postId}/comments")
	public ResponseEntity<?> createComment(@PathVariable Long postId, @RequestBody CreateCommentRequest request) {
		try {
			if (request.content() == null || request.content().isBlank()) {
				return ResponseEntity.badRequest().body(new ApiErrorResponse("댓글 내용을 입력하세요."));
			}
			Long commentId = boardService.addComment(postId, request.content().strip());
			return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("commentId", commentId));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Create comment failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("댓글 저장 중 오류가 발생했습니다."));
		}
	}

	/** 댓글 수정 (본인만) */
	@PutMapping("/comments/{commentId}")
	public ResponseEntity<?> updateComment(@PathVariable Long commentId, @RequestBody CreateCommentRequest request) {
		try {
			if (request.content() == null || request.content().isBlank()) {
				return ResponseEntity.badRequest().body(new ApiErrorResponse("댓글 내용을 입력하세요."));
			}
			boardService.updateComment(commentId, request.content().strip());
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Update comment failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("댓글 수정 중 오류가 발생했습니다."));
		}
	}

	/** 댓글 삭제 (본인만) */
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
		try {
			boardService.deleteComment(commentId);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(e.getMessage()));
		} catch (Exception e) {
			log.error("Delete comment failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse("댓글 삭제 중 오류가 발생했습니다."));
		}
	}
}
