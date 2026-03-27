package minkyu307.spring_ai.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import minkyu307.spring_ai.config.TraceIdFilter;
import minkyu307.spring_ai.dto.ApiErrorResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 표준 API 에러 응답을 생성한다.
 */
@Component
public class ApiErrorFactory {

	/**
	 * 공통 에러 응답을 생성한다.
	 */
	public ApiErrorResponse create(
		HttpStatus status,
		ApiErrorCode errorCode,
		String message,
		Object details,
		HttpServletRequest request
	) {
		String resolvedMessage = (message != null && !message.isBlank())
			? message
			: errorCode.defaultMessage();
		return new ApiErrorResponse(
			errorCode.name(),
			resolvedMessage,
			enrichDetails(details, status, request),
			resolveTraceId(request),
			Instant.now()
		);
	}

	private Object enrichDetails(Object details, HttpStatus status, HttpServletRequest request) {
		if (details != null) {
			return details;
		}
		return java.util.Map.of(
			"status", status.value(),
			"path", request.getRequestURI()
		);
	}

	private String resolveTraceId(HttpServletRequest request) {
		Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
		if (traceId instanceof String value && !value.isBlank()) {
			return value;
		}
		String fromMdc = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		if (fromMdc != null && !fromMdc.isBlank()) {
			return fromMdc;
		}
		return null;
	}
}
