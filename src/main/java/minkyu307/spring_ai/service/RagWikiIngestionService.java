package minkyu307.spring_ai.service;

import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.RagWikiIngestRequest;
import minkyu307.spring_ai.dto.RagWikiIngestResponse;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dooray Wiki 페이지를 조회하여 VectorStore에 적재하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class RagWikiIngestionService {

    private static final String DOORAY_BASE = "https://api.dooray.com";

    private final DoorayWikiApiClient doorayWikiApiClient;
    private final DocumentIngestionService ingestionService;

    /**
     * 요청된 Wiki 페이지 목록을 순차 적재하고 결과를 반환한다.
     */
    public RagWikiIngestResponse ingest(RagWikiIngestRequest request) {
        if (request == null || request.pages() == null || request.pages().isEmpty()) {
            throw new IllegalArgumentException("적재할 페이지가 없습니다.");
        }

        String loginId = SecurityUtils.getCurrentLoginId();

        List<RagWikiIngestResponse.PageResult> results = new ArrayList<>();
        int totalChunks = 0;
        int succeeded = 0;
        int failed = 0;

        for (RagWikiIngestRequest.WikiPageRef ref : request.pages()) {
            try {
                RagWikiIngestResponse.PageResult result = ingestPage(ref, loginId);
                results.add(result);
                if ("SUCCESS".equals(result.status())) {
                    succeeded++;
                    totalChunks += result.chunksIngested();
                } else {
                    failed++;
                }
            } catch (Exception e) {
                results.add(new RagWikiIngestResponse.PageResult(
                        ref.pageId(), null, "FAILED", 0, e.getMessage()
                ));
                failed++;
            }
        }

        return new RagWikiIngestResponse(succeeded, failed, totalChunks, results);
    }

    /**
     * 단일 Wiki 페이지를 조회하여 VectorStore에 적재한다.
     */
    @SuppressWarnings("unchecked")
    private RagWikiIngestResponse.PageResult ingestPage(
            RagWikiIngestRequest.WikiPageRef ref,
            String loginId) {

        String url = DOORAY_BASE + "/wiki/v1/wikis/" + ref.wikiId() + "/pages/" + ref.pageId();
        Map<String, Object> resp = doorayWikiApiClient.getWithRetry(url).getBody();

        if (resp == null) throw new IllegalStateException("두레이 API 응답이 없습니다.");

        Map<String, Object> result = (Map<String, Object>) resp.get("result");
        if (result == null) throw new IllegalStateException("두레이 API result 필드가 없습니다.");

        String subject = (String) result.getOrDefault("subject", ref.pageId());
        Map<String, Object> body = (Map<String, Object>) result.getOrDefault("body", Map.of());
        String content = (String) body.getOrDefault("content", "");

        if (content == null || content.isBlank()) {
            return new RagWikiIngestResponse.PageResult(ref.pageId(), subject, "FAILED", 0, "본문 내용이 없습니다.");
        }

        String docId = UUID.randomUUID().toString();
        String updatedAt = (String) result.getOrDefault("updatedAt", Instant.now().toString());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "dooray-wiki");
        metadata.put("wikiId", ref.wikiId());
        metadata.put("pageId", ref.pageId());
        metadata.put("filename", subject);
        metadata.put("docId", docId);
        metadata.put("ingestedAt", Instant.now().toString());
        metadata.put("updatedAt", updatedAt);
        metadata.put("loginId", loginId);

        List<Document> documents = List.of(new Document(content, metadata));
        DocumentIngestionService.IngestionResult ingestionResult =
                ingestionService.ingestDocuments(documents, metadata);

        return new RagWikiIngestResponse.PageResult(
                ref.pageId(),
                ingestionResult.title() != null ? ingestionResult.title() : subject,
                "SUCCESS",
                ingestionResult.chunksIngested(),
                null
        );
    }

}
