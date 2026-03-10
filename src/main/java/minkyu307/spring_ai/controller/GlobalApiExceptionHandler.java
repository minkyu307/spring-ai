package minkyu307.spring_ai.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import minkyu307.spring_ai.dto.ApiErrorResponse;
import minkyu307.spring_ai.exception.ForbiddenOperationException;
import minkyu307.spring_ai.exception.ResourceNotFoundException;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST API 전역 예외를 일관된 형태로 변환한다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalApiExceptionHandler {

	private ResponseEntity<ApiErrorResponse> buildError(
		HttpStatus status,
		String code,
		String message,
		HttpServletRequest request
	) {
		return ResponseEntity.status(status).body(
			new ApiErrorResponse(
				code,
				status.value(),
				java.time.Instant.now(),
				request.getRequestURI(),
				message
			)
		);
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
			"AI_TRANSIENT_ERROR",
			"AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해 주세요.",
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
		String message = (e.getMessage() != null && !e.getMessage().isBlank())
			? e.getMessage()
			: "AI 요청 처리 중 오류가 발생했습니다.";
		return buildError(HttpStatus.BAD_REQUEST, "AI_NON_TRANSIENT_ERROR", message, request);
	}

	/**
	 * 잘못된 요청 파라미터 오류를 처리한다.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
		IllegalArgumentException e,
		HttpServletRequest request
	) {
		String message = (e.getMessage() != null && !e.getMessage().isBlank())
			? e.getMessage()
			: "잘못된 요청입니다.";
		return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request);
	}

	/**
	 * 인증/도메인 상태 오류를 처리한다.
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
		IllegalStateException e,
		HttpServletRequest request
	) {
		String message = (e.getMessage() != null && !e.getMessage().isBlank())
			? e.getMessage()
			: "요청을 처리할 수 없습니다.";
		return buildError(HttpStatus.FORBIDDEN, "ILLEGAL_STATE", message, request);
	}

	/**
	 * 게시판 등 리소스 미존재 오류를 처리한다.
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
		ResourceNotFoundException e,
		HttpServletRequest request
	) {
		String message = (e.getMessage() != null && !e.getMessage().isBlank())
			? e.getMessage()
			: "요청한 리소스를 찾을 수 없습니다.";
		return buildError(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message, request);
	}

	/**
	 * 권한 없는 작업 요청을 처리한다.
	 */
	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<ApiErrorResponse> handleForbiddenOperationException(
		ForbiddenOperationException e,
		HttpServletRequest request
	) {
		String message = (e.getMessage() != null && !e.getMessage().isBlank())
			? e.getMessage()
			: "허용되지 않은 요청입니다.";
		return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN_OPERATION", message, request);
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
			"INTERNAL_SERVER_ERROR",
			e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : "서버 내부 오류가 발생했습니다.",
			request
		);
	}
}
