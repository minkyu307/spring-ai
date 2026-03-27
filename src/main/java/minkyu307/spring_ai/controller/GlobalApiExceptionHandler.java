package minkyu307.spring_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minkyu307.spring_ai.dto.ApiErrorResponse;
import minkyu307.spring_ai.error.ApiErrorCode;
import minkyu307.spring_ai.error.ApiErrorFactory;
import minkyu307.spring_ai.error.ApiException;
import minkyu307.spring_ai.exception.ForbiddenOperationException;
import minkyu307.spring_ai.exception.ResourceNotFoundException;
import minkyu307.spring_ai.service.SignUpService;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * REST API 전역 예외를 일관된 형태로 변환한다.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalApiExceptionHandler {

	private final ApiErrorFactory apiErrorFactory;

	private ResponseEntity<ApiErrorResponse> buildError(
		HttpStatus status,
		ApiErrorCode code,
		String message,
		Object details,
		HttpServletRequest request
	) {
		return ResponseEntity
			.status(status)
			.body(apiErrorFactory.create(status, code, message, details, request));
	}

	/**
	 * 공통 API 예외를 처리한다.
	 */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(
		ApiException e,
		HttpServletRequest request
	) {
		HttpStatus status = e.getStatus();
		if (status.is5xxServerError()) {
			log.error("API error", e);
		} else {
			log.warn("API error: {}", e.getErrorCode(), e);
		}
		return buildError(status, e.getErrorCode(), e.getMessage(), e.getDetails(), request);
	}

	/**
	 * 재시도 가능한 AI 오류를 처리한다.
	 */
	@ExceptionHandler(TransientAiException.class)
	public ResponseEntity<ApiErrorResponse> handleTransientAiException(
		TransientAiException e,
		HttpServletRequest request
	) {
		log.error("Transient AI error", e);
		return buildError(
			HttpStatus.SERVICE_UNAVAILABLE,
			ApiErrorCode.AI_TRANSIENT_ERROR,
			null,
			null,
			request
		);
	}

	/**
	 * 재시도 비권장 AI 오류를 처리한다.
	 */
	@ExceptionHandler(NonTransientAiException.class)
	public ResponseEntity<ApiErrorResponse> handleNonTransientAiException(
		NonTransientAiException e,
		HttpServletRequest request
	) {
		log.warn("Non-transient AI error", e);
		return buildError(
			HttpStatus.BAD_REQUEST,
			ApiErrorCode.AI_NON_TRANSIENT_ERROR,
			e.getMessage(),
			null,
			request
		);
	}

	/**
	 * 잘못된 요청 파라미터 오류를 처리한다.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
		IllegalArgumentException e,
		HttpServletRequest request
	) {
		return buildError(HttpStatus.BAD_REQUEST, ApiErrorCode.BAD_REQUEST, e.getMessage(), null, request);
	}

	/**
	 * 요청 본문 파싱 오류를 처리한다.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
		HttpMessageNotReadableException e,
		HttpServletRequest request
	) {
		return buildError(
			HttpStatus.BAD_REQUEST,
			ApiErrorCode.BAD_REQUEST,
			"요청 본문 형식이 올바르지 않습니다.",
			null,
			request
		);
	}

	/**
	 * @Valid 검증 실패 오류를 처리한다.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException e,
		HttpServletRequest request
	) {
		Map<String, String> fieldErrors = toFieldErrors(e.getBindingResult().getFieldErrors());
		String message = fieldErrors.values().stream().findFirst().orElse(ApiErrorCode.VALIDATION_FAILED.defaultMessage());
		return buildError(
			HttpStatus.BAD_REQUEST,
			ApiErrorCode.VALIDATION_FAILED,
			message,
			Map.of("fieldErrors", fieldErrors),
			request
		);
	}

	/**
	 * 바인딩 검증 실패 오류를 처리한다.
	 */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiErrorResponse> handleBindException(
		BindException e,
		HttpServletRequest request
	) {
		Map<String, String> fieldErrors = toFieldErrors(e.getBindingResult().getFieldErrors());
		String message = fieldErrors.values().stream().findFirst().orElse(ApiErrorCode.VALIDATION_FAILED.defaultMessage());
		return buildError(
			HttpStatus.BAD_REQUEST,
			ApiErrorCode.VALIDATION_FAILED,
			message,
			Map.of("fieldErrors", fieldErrors),
			request
		);
	}

	/**
	 * 제약조건 검증 실패 오류를 처리한다.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
		ConstraintViolationException e,
		HttpServletRequest request
	) {
		List<Map<String, String>> violations = e.getConstraintViolations().stream()
			.map(v -> Map.of(
				"field", v.getPropertyPath().toString(),
				"message", v.getMessage()
			))
			.toList();
		String message = violations.isEmpty()
			? ApiErrorCode.VALIDATION_FAILED.defaultMessage()
			: violations.getFirst().get("message");
		return buildError(
			HttpStatus.BAD_REQUEST,
			ApiErrorCode.VALIDATION_FAILED,
			message,
			Map.of("violations", violations),
			request
		);
	}

	/**
	 * 회원가입 중복 아이디 오류를 처리한다.
	 */
	@ExceptionHandler(SignUpService.DuplicateLoginIdException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateLoginIdException(
		SignUpService.DuplicateLoginIdException e,
		HttpServletRequest request
	) {
		return buildError(
			HttpStatus.BAD_REQUEST,
			ApiErrorCode.DUPLICATE_LOGIN_ID,
			e.getMessage(),
			null,
			request
		);
	}

	/**
	 * 게시판 등 리소스 미존재 오류를 처리한다.
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
		ResourceNotFoundException e,
		HttpServletRequest request
	) {
		return buildError(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, e.getMessage(), null, request);
	}

	/**
	 * 권한 없는 작업 요청을 처리한다.
	 */
	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<ApiErrorResponse> handleForbiddenOperationException(
		ForbiddenOperationException e,
		HttpServletRequest request
	) {
		return buildError(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN_OPERATION, e.getMessage(), null, request);
	}

	/**
	 * 처리 흐름 상태 오류를 처리한다.
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
		IllegalStateException e,
		HttpServletRequest request
	) {
		log.warn("Illegal state", e);
		return buildError(
			HttpStatus.BAD_REQUEST,
			ApiErrorCode.BAD_REQUEST,
			e.getMessage(),
			null,
			request
		);
	}

	/**
	 * 처리되지 않은 서버 내부 오류를 처리한다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleException(
		Exception e,
		HttpServletRequest request
	) {
		log.error("Unhandled server error", e);
		return buildError(
			HttpStatus.INTERNAL_SERVER_ERROR,
			ApiErrorCode.INTERNAL_SERVER_ERROR,
			null,
			null,
			request
		);
	}

	private Map<String, String> toFieldErrors(List<org.springframework.validation.FieldError> fieldErrors) {
		Map<String, String> result = new LinkedHashMap<>();
		for (org.springframework.validation.FieldError error : fieldErrors) {
			if (result.containsKey(error.getField())) {
				continue;
			}
			result.put(error.getField(), error.getDefaultMessage());
		}
		return result;
	}
}
