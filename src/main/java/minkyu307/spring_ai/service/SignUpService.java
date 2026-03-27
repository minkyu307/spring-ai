package minkyu307.spring_ai.service;

import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.SignUpRequest;
import minkyu307.spring_ai.entity.Role;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.error.ApiErrorCode;
import minkyu307.spring_ai.error.ApiException;
import minkyu307.spring_ai.repository.RoleRepository;
import minkyu307.spring_ai.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 처리. 직접 가입 시 ROLE_USER로 저장.
 */
@Service
@RequiredArgsConstructor
public class SignUpService {

    private static final String DEFAULT_ROLE_NAME = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 가입 처리. loginId 중복 시 DuplicateLoginIdException.
     */
    @Transactional
    public User signUp(SignUpRequest request) {
        if (userRepository.existsById(request.getLoginId())) {
            throw new DuplicateLoginIdException("이미 사용 중인 아이디입니다.");
        }
        Role role = roleRepository.findByName(DEFAULT_ROLE_NAME)
			.orElseThrow(() -> new ApiException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				ApiErrorCode.INTERNAL_SERVER_ERROR,
				"회원가입 기본 권한 구성이 올바르지 않습니다."
			));
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
            request.getLoginId(),
            request.getUsername(),
            encodedPassword,
            role);
        user.setEmail(request.getEmail());
        return userRepository.save(user);
    }

    /** loginId 중복 시 사용. */
	public static class DuplicateLoginIdException extends ApiException {
        public DuplicateLoginIdException(String message) {
			super(HttpStatus.BAD_REQUEST, ApiErrorCode.DUPLICATE_LOGIN_ID, message);
        }
    }
}
