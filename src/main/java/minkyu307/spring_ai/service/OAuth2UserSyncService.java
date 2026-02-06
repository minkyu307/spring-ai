package minkyu307.spring_ai.service;

import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.entity.Role;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.repository.RoleRepository;
import minkyu307.spring_ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Google 등 OIDC 로그인 시 userInfo 클레임으로 app_user 레코드 생성/갱신.
 */
@Service
@RequiredArgsConstructor
public class OAuth2UserSyncService {

	private static final String DEFAULT_ROLE_NAME = "ROLE_USER";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	/**
	 * OidcUser의 sub를 loginId로 사용. 있으면 username/email 갱신, 없으면 신규 생성(기본 역할 ROLE_USER).
	 */
	@Transactional
	public User syncFromOidc(OidcUser oidcUser) {
		String loginId = oidcUser.getSubject();
		String email = oidcUser.getEmail();
		String fullName = oidcUser.getFullName();
		String username = (fullName != null && !fullName.isBlank()) ? fullName : email;

		return userRepository.findById(loginId)
				.map(existing -> updateExisting(existing, username, email))
				.orElseGet(() -> createNew(loginId, username, email));
	}

	private User updateExisting(User user, String username, String email) {
		user.setUsername(username);
		user.setEmail(email);
		return userRepository.save(user);
	}

	private User createNew(String loginId, String username, String email) {
		Role roleUser = roleRepository.findByName(DEFAULT_ROLE_NAME)
				.orElseThrow(() -> new IllegalStateException("Role " + DEFAULT_ROLE_NAME + " not found. Run RoleSeeder."));
		User user = new User(loginId, username, null, roleUser);
		user.setEmail(email);
		return userRepository.save(user);
	}
}
