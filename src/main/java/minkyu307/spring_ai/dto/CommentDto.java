package minkyu307.spring_ai.dto;

import java.time.Instant;

/** 댓글 응답 DTO */
public record CommentDto(
	Long commentId,
	String writerLoginId,
	String writerUsername,
	String content,
	Instant updatedAt
) {}
