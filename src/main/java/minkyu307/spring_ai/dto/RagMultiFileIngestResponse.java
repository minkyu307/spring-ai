package minkyu307.spring_ai.dto;

import java.util.List;

/**
 * 멀티 파일 업로드 기반 RAG 문서 적재 결과(요청 단위).
 */
public record RagMultiFileIngestResponse(
		int receivedFiles,
		int processedFiles,
		int succeededFiles,
		int failedFiles,
		int skippedFiles,
		int totalChunksIngested,
		List<RagFileIngestResultDto> results
) {
}

