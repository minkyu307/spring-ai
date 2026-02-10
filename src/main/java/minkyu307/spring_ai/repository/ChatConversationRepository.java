package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {

	List<ChatConversation> findByLoginIdOrderByCreatedAtDesc(String loginId);

	Optional<ChatConversation> findByIdAndLoginId(String id, String loginId);
}
