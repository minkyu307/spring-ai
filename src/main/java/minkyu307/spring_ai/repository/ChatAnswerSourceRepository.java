package minkyu307.spring_ai.repository;

import java.time.Instant;
import java.util.List;
import minkyu307.spring_ai.entity.ChatAnswerSource;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * assistant 답변 출처 저장/조회 리포지토리.
 */
public interface ChatAnswerSourceRepository extends JpaRepository<ChatAnswerSource, Long> {

	/**
	 * 대화의 출처 레코드를 assistant 메시지 시간 순으로 조회한다.
	 */
	List<ChatAnswerSource> findByConversationIdOrderByMessageTimestampAscIdAsc(String conversationId);

	/**
	 * 특정 대화의 출처 레코드를 모두 삭제한다.
	 */
	void deleteByConversationId(String conversationId);

	/**
	 * 특정 assistant 메시지에 연결된 출처 레코드를 삭제한다.
	 */
	void deleteByConversationIdAndMessageTimestamp(String conversationId, Instant messageTimestamp);
}
