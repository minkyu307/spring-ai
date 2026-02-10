package minkyu307.spring_ai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * id/pw 로그인 및 Google OIDC 로그인 모두 {@link minkyu307.spring_ai.entity.User#loginId} 기준으로
 * 현재 사용자 식별자를 반환한다.
 */
public final class SecurityUtils {

	private SecurityUtils() {}

	public static String getCurrentLoginId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null) {
			throw new IllegalStateException("인증 정보가 없습니다.");
		}

		Object principal = auth.getPrincipal();

		// id/pw 로그인 (AppUserDetailsService) → username == User.loginId
		if (principal instanceof UserDetails userDetails) {
			return userDetails.getUsername();
		}

		// Google OIDC 로그인 → subject == User.loginId (OAuth2UserSyncService 에서 그렇게 사용)
		if (principal instanceof OidcUser oidcUser) {
			return oidcUser.getSubject();
		}

		throw new IllegalStateException("지원하지 않는 principal 타입: " + principal.getClass());
	}
}
