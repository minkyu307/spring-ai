package minkyu307.spring_ai.security;

import minkyu307.spring_ai.error.ApiErrorCode;
import minkyu307.spring_ai.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * form 로그인 기준으로 현재 사용자 식별자를 반환한다.
 */
public final class SecurityUtils {

	private SecurityUtils() {}

	public static String getCurrentLoginId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "인증 정보가 없습니다.");
		}

		Object principal = auth.getPrincipal();

		// id/pw 로그인 (AppUserDetailsService) → username == User.loginId
		if (principal instanceof UserDetails userDetails) {
			return userDetails.getUsername();
		}

		throw new ApiException(
			HttpStatus.UNAUTHORIZED,
			ApiErrorCode.UNAUTHORIZED,
			"지원하지 않는 인증 principal 타입입니다."
		);
	}
}
