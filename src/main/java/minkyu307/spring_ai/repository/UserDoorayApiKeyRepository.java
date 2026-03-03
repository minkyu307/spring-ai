package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.entity.UserDoorayApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자별 두레이 API 키 JPA 레포지토리.
 */
public interface UserDoorayApiKeyRepository extends JpaRepository<UserDoorayApiKey, String> {
}
