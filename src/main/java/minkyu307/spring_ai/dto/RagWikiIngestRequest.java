package minkyu307.spring_ai.dto;

import java.util.List;

/**
 * Dooray Wiki 페이지 목록을 Vector DB로 적재하기 위한 요청 DTO.
 */
public record RagWikiIngestRequest(
		List<WikiPageRef> pages
) {
	/** 단일 Wiki 페이지 참조 (wikiId + pageId 쌍). */
	public record WikiPageRef(
			String wikiId,
			String pageId
	) {}
}
