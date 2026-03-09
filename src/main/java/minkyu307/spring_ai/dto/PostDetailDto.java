package minkyu307.spring_ai.dto;

import java.time.Instant;
import java.util.List;

/** 게시글 상세(본문 + 댓글 목록) 응답 DTO */
public record PostDetailDto(
	Long postId,
	String title,
	String content,
	String writerLoginId,
	String writerUsername,
	Instant updatedAt,
	List<CommentDto> comments
) {}
