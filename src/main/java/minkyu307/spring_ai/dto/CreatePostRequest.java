package minkyu307.spring_ai.dto;

/** 게시글 작성 요청 body */
public record CreatePostRequest(String title, String content) {}
