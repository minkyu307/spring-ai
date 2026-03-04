package minkyu307.spring_ai.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.springframework.web.client.HttpClientErrorException;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.repository.UserDoorayApiKeyRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * 두레이 Wiki API 프록시. 사용자 API 키로 두레이 서버에 요청 후 결과를 반환.
 */
@RestController
@RequestMapping("/api/dooray/wiki")
@RequiredArgsConstructor
public class DoorayWikiApiController {

    private static final String DOORAY_BASE = "https://api.dooray.com";

    private final UserDoorayApiKeyRepository doorayApiKeyRepository;
    private final RestTemplate restTemplate;

    /**
     * 현재 사용자의 두레이 API 키로 Authorization 헤더 생성.
     */
    private HttpHeaders authHeaders() {
        String loginId = SecurityUtils.getCurrentLoginId();
        String apiKey = doorayApiKeyRepository.findById(loginId)
            .map(k -> k.getApiKey())
            .orElseThrow(() -> new IllegalStateException("두레이 API 키가 설정되지 않았습니다."));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "dooray-api " + apiKey);
        return headers;
    }

    /**
     * 접근 가능한 위키 목록 조회.
     */
    @GetMapping("/wikis")
    public ResponseEntity<Map<String, Object>> getWikis(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size) {
        String url = DOORAY_BASE + "/wiki/v1/wikis?page=" + page + "&size=" + size;
        return restTemplate.exchange(url, HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
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
        return restTemplate.exchange(url, HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /** 동시 병렬 요청 수 상한 — Dooray Rate Limit 대응 */
    private static final int PARALLEL_BATCH = 5;
    /** 429 수신 시 재시도 대기(ms) */
    private static final long RETRY_DELAY_MS = 500;
    /** 최대 재시도 횟수 */
    private static final int MAX_RETRY = 3;

    /**
     * 특정 페이지부터 모든 하위 페이지를 BFS 레벨 단위 병렬 조회하여 평탄화된 목록으로 반환.
     * 같은 레벨을 PARALLEL_BATCH 단위로 묶어 병렬 호출하고, 429 응답 시 재시도.
     */
    @GetMapping("/wikis/{wikiId}/pages/{pageId}/subtree")
    public ResponseEntity<Map<String, Object>> getSubtree(
        @PathVariable String wikiId,
        @PathVariable String pageId) {

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
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
                            () -> fetchPagesWithRetry(wikiId, parentId, entity), executor))
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
        return restTemplate.exchange(url, HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * 단일 parentPageId에 대한 페이지 목록 조회. 429 응답 시 RETRY_DELAY_MS 대기 후 재시도.
     */
    private List<Map<String, Object>> fetchPagesWithRetry(
        String wikiId, String parentId, HttpEntity<Void> entity) {

        String url = DOORAY_BASE + "/wiki/v1/wikis/" + wikiId
            + "/pages?parentPageId=" + parentId;

        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                Map<String, Object> resp = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                ).getBody();
                if (resp == null) return List.of();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pages = (List<Map<String, Object>>) resp.get("result");
                return pages != null ? pages : List.of();
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRY) throw e;
                try {
                    // 재시도마다 대기 시간을 지수적으로 증가
                    Thread.sleep(RETRY_DELAY_MS * (1L << attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
        return List.of();
    }
}
