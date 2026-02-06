package minkyu307.spring_ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minkyu307.spring_ai.entity.Role;
import minkyu307.spring_ai.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기동 시 기본 Role(ROLE_USER, ROLE_ADMIN)이 없으면 DB에 삽입.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RoleSeeder implements CommandLineRunner {

	private static final String ROLE_USER = "ROLE_USER";
	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	private final RoleRepository roleRepository;

	@Override
	@Transactional
	public void run(String... args) {
		insertIfAbsent(ROLE_USER);
		insertIfAbsent(ROLE_ADMIN);
	}

	private void insertIfAbsent(String name) {
		if (roleRepository.findByName(name).isEmpty()) {
			roleRepository.save(new Role(name));
			log.info("Role seeded: {}", name);
		}
	}
}
