package minkyu307.spring_ai.repository;

import java.util.Optional;
import minkyu307.spring_ai.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

/** app_role 테이블 접근. */
public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByName(String name);
}
