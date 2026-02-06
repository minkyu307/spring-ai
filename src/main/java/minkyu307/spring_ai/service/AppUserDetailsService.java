package minkyu307.spring_ai.service;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직접 가입 사용자 로그인용. username 파라미터는 loginId로 조회.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findById(username)
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
		if (user.getPassword() == null || user.getPassword().isEmpty()) {
			throw new UsernameNotFoundException("비밀번호 로그인을 지원하지 않는 계정입니다.");
		}
		var authorities = user.getRole() != null
				? Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName()))
				: Collections.<SimpleGrantedAuthority>emptyList();
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getLoginId())
				.password(user.getPassword())
				.authorities(authorities)
				.build();
	}
}
