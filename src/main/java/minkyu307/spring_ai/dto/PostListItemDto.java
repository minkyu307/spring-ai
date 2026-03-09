package minkyu307.spring_ai.dto;

import java.time.Instant;

/** 게시판 목록(사이드바 등)용 게시글 요약 DTO */
public record PostListItemDto(
	Long postId,
	String title,
	String writerLoginId,
	String writerUsername,
	Instant updatedAt
) {}
