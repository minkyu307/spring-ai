package minkyu307.spring_ai.controller;

import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.entity.UserDoorayApiKey;
import minkyu307.spring_ai.repository.UserDoorayApiKeyRepository;
import minkyu307.spring_ai.repository.UserRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 개인 설정 API. 두레이 API 키 조회/저장.
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class UserSettingsApiController {

    private final UserDoorayApiKeyRepository doorayApiKeyRepository;
    private final UserRepository userRepository;

    /**
     * 현재 사용자의 두레이 API 키 조회. 미설정 시 빈 문자열 반환.
     */
    @GetMapping("/dooray-apikey")
    public ResponseEntity<Map<String, String>> getDoorayApiKey() {
        String loginId = SecurityUtils.getCurrentLoginId();
        String apiKey = doorayApiKeyRepository.findById(loginId)
            .map(UserDoorayApiKey::getApiKey)
            .orElse("");
        return ResponseEntity.ok(Map.of("apiKey", apiKey));
    }

    /**
     * 현재 사용자의 두레이 API 키 저장/갱신.
     */
    @PutMapping("/dooray-apikey")
    public ResponseEntity<Map<String, String>> saveDoorayApiKey(
        @RequestBody Map<String, String> body) {
        String loginId = SecurityUtils.getCurrentLoginId();
        String apiKey = body.getOrDefault("apiKey", "").trim();

        // findById로 영속 상태 엔티티를 가져와 수정 — 없으면 신규 생성
        // @MapsId 엔티티에 PK를 직접 세팅한 비영속 객체를 save하면 Hibernate가
        // merge 경로로 처리하다 StaleObjectStateException을 던지므로 반드시 이 패턴을 사용
        UserDoorayApiKey entity = doorayApiKeyRepository.findById(loginId)
            .orElseGet(() -> {
                User user = userRepository.findById(loginId)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + loginId));
                return new UserDoorayApiKey(user, apiKey);
            });
        entity.setApiKey(apiKey);
        entity.setUpdatedAt(Instant.now());
        doorayApiKeyRepository.save(entity);

        return ResponseEntity.ok(Map.of("apiKey", apiKey));
    }
}
