package minkyu307.spring_ai.controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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

    /**
     * 특정 페이지부터 모든 하위 페이지를 BFS로 선조회하여 평탄화된 목록으로 반환.
     * 각 항목에 parentPageId가 포함되므로 프론트엔드에서 트리 재구성 가능.
     */
    @GetMapping("/wikis/{wikiId}/pages/{pageId}/subtree")
    public ResponseEntity<Map<String, Object>> getSubtree(
        @PathVariable String wikiId,
        @PathVariable String pageId) {

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        List<Map<String, Object>> allPages = new ArrayList<>();

        // BFS: 큐에 조회할 parentPageId를 넣고 순차 처리
        Queue<String> queue = new LinkedList<>();
        queue.add(pageId);

        while (!queue.isEmpty()) {
            String currentParentId = queue.poll();
            String url = DOORAY_BASE + "/wiki/v1/wikis/" + wikiId
                + "/pages?parentPageId=" + currentParentId;

            Map<String, Object> resp = restTemplate.exchange(url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();

            if (resp == null) continue;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pages = (List<Map<String, Object>>) resp.get("result");
            if (pages == null || pages.isEmpty()) continue;

            allPages.addAll(pages);
            // 각 페이지의 id를 다음 BFS 레벨로 추가
            pages.forEach(p -> queue.add(String.valueOf(p.get("id"))));
        }

        return ResponseEntity.ok(Map.of(
            "result", allPages,
            "totalCount", allPages.size()
        ));
    }
}
