package minkyu307.spring_ai.service;

import lombok.RequiredArgsConstructor;
import minkyu307.spring_ai.dto.SignUpRequest;
import minkyu307.spring_ai.entity.Role;
import minkyu307.spring_ai.entity.User;
import minkyu307.spring_ai.repository.RoleRepository;
import minkyu307.spring_ai.repository.UserRepository;
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
            .orElseThrow(() -> new IllegalStateException("Role " + DEFAULT_ROLE_NAME + " not found."));
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
    public static class DuplicateLoginIdException extends RuntimeException {
        public DuplicateLoginIdException(String message) {
            super(message);
        }
    }
}
