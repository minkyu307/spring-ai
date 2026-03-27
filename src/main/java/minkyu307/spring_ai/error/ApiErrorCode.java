package minkyu307.spring_ai.error;

/**
 * API 에러 코드 표준.
 */
public enum ApiErrorCode {
	BAD_REQUEST("잘못된 요청입니다."),
	VALIDATION_FAILED("입력값 검증에 실패했습니다."),
	RESOURCE_NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),
	FORBIDDEN_OPERATION("허용되지 않은 요청입니다."),
	UNAUTHORIZED("인증 정보가 없습니다."),
	SESSION_EXPIRED("세션이 만료되었습니다. 다시 로그인해 주세요."),
	AI_TRANSIENT_ERROR("AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해 주세요."),
	AI_NON_TRANSIENT_ERROR("AI 요청 처리 중 오류가 발생했습니다."),
	DUPLICATE_LOGIN_ID("이미 사용 중인 아이디입니다."),
	DOORAY_RATE_LIMIT("Dooray API 호출 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),
	DOORAY_UNAUTHORIZED("Dooray API 인증에 실패했습니다. API Key를 확인해 주세요."),
	DOORAY_FORBIDDEN("Dooray API 호출 권한이 없습니다."),
	DOORAY_RESPONSE_ERROR("Dooray API 호출에 실패했습니다."),
	DOORAY_SERVER_ERROR("Dooray API 서버 오류가 발생했습니다."),
	DOORAY_NETWORK_ERROR("Dooray API 네트워크 오류가 발생했습니다."),
	INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

	private final String defaultMessage;

	ApiErrorCode(String defaultMessage) {
		this.defaultMessage = defaultMessage;
	}

	public String defaultMessage() {
		return defaultMessage;
	}
}
