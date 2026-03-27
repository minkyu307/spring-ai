package minkyu307.spring_ai.error;

import org.springframework.http.HttpStatus;

/**
 * HTTP 상태/에러 코드를 함께 전달하는 공통 API 예외.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final ApiErrorCode errorCode;
	private final Object details;

	public ApiException(HttpStatus status, ApiErrorCode errorCode) {
		this(status, errorCode, null, null);
	}

	public ApiException(HttpStatus status, ApiErrorCode errorCode, String message) {
		this(status, errorCode, message, null);
	}

	public ApiException(HttpStatus status, ApiErrorCode errorCode, String message, Object details) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
		this.details = details;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public ApiErrorCode getErrorCode() {
		return errorCode;
	}

	public Object getDetails() {
		return details;
	}
}
