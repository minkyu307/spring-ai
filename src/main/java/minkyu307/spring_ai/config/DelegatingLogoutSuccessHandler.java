package minkyu307.spring_ai.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * Form 로그인: /login으로 리다이렉트. OIDC 로그인: IdP 로그아웃 후 /login으로 리다이렉트.
 */
public class DelegatingLogoutSuccessHandler implements LogoutSuccessHandler {

	private final ClientRegistrationRepository clientRegistrationRepository;

	public DelegatingLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
			OidcClientInitiatedLogoutSuccessHandler oidcHandler =
					new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
			oidcHandler.setPostLogoutRedirectUri(buildRedirectUri(request));
			oidcHandler.onLogoutSuccess(request, response, authentication);
		} else {
			SimpleUrlLogoutSuccessHandler formHandler = new SimpleUrlLogoutSuccessHandler();
			formHandler.setDefaultTargetUrl("/login");
			formHandler.onLogoutSuccess(request, response, authentication);
		}
	}

	private String buildRedirectUri(HttpServletRequest request) {
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int port = request.getServerPort();
		String contextPath = request.getContextPath();
		String path = contextPath + "/login";
		if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
			return scheme + "://" + serverName + path;
		}
		return scheme + "://" + serverName + ":" + port + path;
	}
}
