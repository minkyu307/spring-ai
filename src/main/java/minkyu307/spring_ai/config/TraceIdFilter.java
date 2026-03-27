package minkyu307.spring_ai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 단위 traceId를 생성/전파한다.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String TRACE_ID_HEADER = "X-Trace-Id";
	public static final String TRACE_ID_ATTRIBUTE = "traceId";
	public static final String TRACE_ID_MDC_KEY = "traceId";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String traceId = resolveTraceId(request);
		request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);
		MDC.put(TRACE_ID_MDC_KEY, traceId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID_MDC_KEY);
		}
	}

	private String resolveTraceId(HttpServletRequest request) {
		String fromHeader = request.getHeader(TRACE_ID_HEADER);
		if (fromHeader != null && !fromHeader.isBlank()) {
			return fromHeader.strip();
		}
		return UUID.randomUUID().toString();
	}
}
