package minkyu307.spring_ai.config;

import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.service.OAuth2UserSyncService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Google OIDC 로그인 시 기본 OidcUser 로드 후 app_user 생성/갱신을 수행하는 OidcUserService 등록.
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2LoginConfig {

	private final OAuth2UserSyncService oauth2UserSyncService;

	@Bean
	public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
		OidcUserService delegate = new OidcUserService();
		return userRequest -> { 
			OidcUser oidcUser = delegate.loadUser(userRequest);
			oauth2UserSyncService.syncFromOidc(oidcUser);
			return oidcUser;
		};
	}
}
