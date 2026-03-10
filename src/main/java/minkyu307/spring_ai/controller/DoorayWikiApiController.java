package minkyu307.spring_ai.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.service.DoorayWikiApiClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 두레이 Wiki API 프록시. 사용자 API 키로 두레이 서버에 요청 후 결과를 반환.
 */
@RestController
@RequestMapping("/api/dooray/wiki")
@RequiredArgsConstructor
public class DoorayWikiApiController {

    private static final String DOORAY_BASE = "https://api.dooray.com";

    private final DoorayWikiApiClient doorayWikiApiClient;

    /**
     * 접근 가능한 위키 목록 조회.
     */
    @GetMapping("/wikis")
    public ResponseEntity<Map<String, Object>> getWikis(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size) {
        String url = DOORAY_BASE + "/wiki/v1/wikis?page=" + page + "&size=" + size;
        return doorayWikiApiClient.getWithRetry(url);
    }

    /**
     * 특정 위키의 한 depth 페이지 목록 조회. parentPageId 미전달 시 최상위 페이지 반환.
     */
    @GetMapping("/wikis/{wikiId}/pages")
    public ResponseEntity<Map<String, Object>> getPages(
        @PathVariable String wikiId,
        @RequestParam(required = false) String parentPageId) {
        String url = DOORAY_BASE + "/wiki/v1/wikis/" + wikiId + "/pages"
            + (parentPageId != null ? "?parentPageId=" + parentPageId : "");
        return doorayWikiApiClient.getWithRetry(url);
    }

    /** 동시 병렬 요청 수 상한 — Dooray Rate Limit 대응 */
    private static final int PARALLEL_BATCH = 5;

    /**
     * 특정 페이지부터 모든 하위 페이지를 BFS 레벨 단위 병렬 조회하여 평탄화된 목록으로 반환.
     * 같은 레벨을 PARALLEL_BATCH 단위로 묶어 병렬 호출하고, 429 응답 시 재시도.
     */
    @GetMapping("/wikis/{wikiId}/pages/{pageId}/subtree")
    public ResponseEntity<Map<String, Object>> getSubtree(
        @PathVariable String wikiId,
        @PathVariable String pageId) {

        // 요청 스레드에서 인증 헤더를 미리 캡처해 비동기 스레드에서 SecurityContext 재조회가 발생하지 않도록 한다.
        HttpEntity<Void> authEntity = doorayWikiApiClient.createAuthenticatedEntity();
        List<Map<String, Object>> allPages = new ArrayList<>();
        List<String> currentLevel = List.of(pageId);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            while (!currentLevel.isEmpty()) {
                List<Map<String, Object>> levelPages = new ArrayList<>();

                // 같은 레벨을 PARALLEL_BATCH 단위로 나눠 병렬 처리 — Rate Limit 방지
                for (int i = 0; i < currentLevel.size(); i += PARALLEL_BATCH) {
                    List<String> batch = currentLevel.subList(i,
                        Math.min(i + PARALLEL_BATCH, currentLevel.size()));

                    List<CompletableFuture<List<Map<String, Object>>>> futures = batch.stream()
                        .map(parentId -> CompletableFuture.<List<Map<String, Object>>>supplyAsync(
                            () -> fetchPagesWithRetry(wikiId, parentId, authEntity), executor))
                        .collect(Collectors.toList());

                    futures.stream()
                        .map(CompletableFuture::join)
                        .forEach(levelPages::addAll);
                }

                allPages.addAll(levelPages);
                currentLevel = levelPages.stream()
                    .map(p -> String.valueOf(p.get("id")))
                    .collect(Collectors.toList());
            }
        } finally {
            executor.shutdown();
        }

        return ResponseEntity.ok(Map.of(
            "result", allPages,
            "totalCount", allPages.size()
        ));
    }

    /**
     * 특정 위키 페이지 단건 상세 조회 (본문 포함).
     */
    @GetMapping("/wikis/{wikiId}/pages/{pageId}")
    public ResponseEntity<Map<String, Object>> getPage(
        @PathVariable String wikiId,
        @PathVariable String pageId) {
        String url = DOORAY_BASE + "/wiki/v1/wikis/" + wikiId + "/pages/" + pageId;
        return doorayWikiApiClient.getWithRetry(url);
    }

    /**
     * 단일 parentPageId에 대한 페이지 목록 조회. 429 응답 시 RETRY_DELAY_MS 대기 후 재시도.
     */
    private List<Map<String, Object>> fetchPagesWithRetry(
        String wikiId,
        String parentId,
        HttpEntity<Void> authEntity) {

        String url = DOORAY_BASE + "/wiki/v1/wikis/" + wikiId
            + "/pages?parentPageId=" + parentId;
        Map<String, Object> resp = doorayWikiApiClient.getWithRetry(url, authEntity).getBody();
        if (resp == null) return List.of();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pages = (List<Map<String, Object>>) resp.get("result");
        return pages != null ? pages : List.of();
    }
}
