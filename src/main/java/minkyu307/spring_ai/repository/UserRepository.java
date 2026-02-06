package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/** app_user 테이블 접근. PK는 loginId(문자열). */
public interface UserRepository extends JpaRepository<User, String> {
}
