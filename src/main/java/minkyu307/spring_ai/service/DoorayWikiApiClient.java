package minkyu307.spring_ai.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.error.ApiErrorCode;
import minkyu307.spring_ai.error.ApiException;
import minkyu307.spring_ai.repository.UserDoorayApiKeyRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Dooray Wiki GET 호출과 429 재시도 정책을 공통으로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class DoorayWikiApiClient {

    /** 429 수신 시 재시도 대기(ms) */
    private static final long RETRY_DELAY_MS = 1000;
    /** 최대 재시도 횟수 */
    private static final int MAX_RETRY = 10;

    private final UserDoorayApiKeyRepository doorayApiKeyRepository;
    private final RestTemplate restTemplate;

    /**
     * 사용자 API 키를 적용한 Dooray GET 요청을 수행한다.
     */
    public ResponseEntity<Map<String, Object>> getWithRetry(String url) {
        return getWithRetry(url, createAuthenticatedEntity());
    }

    /**
     * 현재 요청 스레드의 인증 컨텍스트로 인증 헤더를 생성한다.
     * 비동기 작업에서는 이 메서드를 호출하지 말고, 요청 시작 시점에 생성한 엔티티를 재사용한다.
     */
    public HttpEntity<Void> createAuthenticatedEntity() {
        return new HttpEntity<>(authHeaders());
    }

    /**
     * 제공된 인증 엔티티로 Dooray GET 요청을 수행한다.
     */
    public ResponseEntity<Map<String, Object>> getWithRetry(String url, HttpEntity<?> entity) {
        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                return restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRY) {
                    throw new ApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        ApiErrorCode.DOORAY_RATE_LIMIT,
                        null
                    );
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ApiErrorCode.INTERNAL_SERVER_ERROR,
                        "Dooray API 재시도 처리 중 인터럽트가 발생했습니다."
                    );
                }
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCode.DOORAY_UNAUTHORIZED,
                    null
                );
            } catch (HttpClientErrorException.Forbidden e) {
                throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ApiErrorCode.DOORAY_FORBIDDEN,
                    null
                );
            } catch (HttpClientErrorException e) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    ApiErrorCode.DOORAY_RESPONSE_ERROR,
                    e.getStatusText(),
                    Map.of("doorayStatus", e.getStatusCode().value())
                );
            } catch (HttpServerErrorException e) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    ApiErrorCode.DOORAY_SERVER_ERROR,
                    e.getStatusText(),
                    Map.of("doorayStatus", e.getStatusCode().value())
                );
            } catch (ResourceAccessException e) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    ApiErrorCode.DOORAY_NETWORK_ERROR,
                    "Dooray API 네트워크 연결에 실패했습니다."
                );
            }
        }
        throw new ApiException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ApiErrorCode.INTERNAL_SERVER_ERROR,
            "Dooray API 재시도 처리 중 예기치 않은 상태입니다."
        );
    }

    /**
     * 현재 사용자의 두레이 API 키로 Authorization 헤더 생성.
     */
    private HttpHeaders authHeaders() {
        String loginId = SecurityUtils.getCurrentLoginId();
        String apiKey = doorayApiKeyRepository.findById(loginId)
            .map(k -> k.getApiKey())
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.DOORAY_UNAUTHORIZED,
                "두레이 API 키가 설정되지 않았습니다."
            ));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "dooray-api " + apiKey);
        return headers;
    }
}
