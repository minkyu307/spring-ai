package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.entity.ChatMemoryEntity;
import minkyu307.spring_ai.entity.ChatMemoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring AI Chat Memory 기본 테이블용 JPA Repository
 * 테이블 컬럼: conversation_id, content, type, timestamp
 */
@Repository
public interface ChatMemoryRepository extends JpaRepository<ChatMemoryEntity, ChatMemoryId> {

	/**
	 * 특정 conversationId의 모든 메시지 조회 (시간순 정렬)
	 */
	List<ChatMemoryEntity> findByConversationIdOrderByTimestampAsc(String conversationId);

	/**
	 * conversationId별 그룹화하여 히스토리 정보 조회
	 * 제목은 첫 USER 메시지로 사용
	 */
	@Query("""
		SELECT 
			c.conversationId as conversationId,
			MIN(CASE WHEN c.type = 'USER' THEN c.content END) as firstUserMessage,
			MAX(c.timestamp) as lastUpdated,
			COUNT(c) as messageCount
		FROM ChatMemoryEntity c
		GROUP BY c.conversationId
		ORDER BY lastUpdated DESC
		""")
	List<ChatHistoryProjection> findHistorySummaries();

	/**
	 * 히스토리 요약 정보 Projection
	 */
	interface ChatHistoryProjection {
		String getConversationId();
		String getFirstUserMessage();
		Instant getLastUpdated();
		Long getMessageCount();
	}
}
