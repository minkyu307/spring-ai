package minkyu307.spring_ai.dto;

import java.util.List;

/**
 * Dooray Wiki 페이지 적재 결과 DTO.
 */
public record RagWikiIngestResponse(
		int succeededPages,
		int failedPages,
		int totalChunksIngested,
		List<PageResult> results
) {
	/** 페이지 단위 적재 결과. */
	public record PageResult(
			String pageId,
			String title,
			String status,   // "SUCCESS" | "FAILED"
			int chunksIngested,
			String error
	) {}
}
