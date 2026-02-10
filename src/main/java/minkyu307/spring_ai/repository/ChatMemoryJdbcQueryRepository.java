package minkyu307.spring_ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Spring AI의 JDBC ChatMemory 테이블(spring_ai_chat_memory) 조회 전용 Repository. // JPA DDL 영향 배제 목적
 */
@Repository
public class ChatMemoryJdbcQueryRepository {

	private final JdbcTemplate jdbcTemplate;

	public ChatMemoryJdbcQueryRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * conversation_id 단위로 히스토리 요약(제목/갱신시각/메시지수) 목록을 조회한다. // Postgres 전용(LATERAL) 쿼리
	 */
	public List<ChatHistorySummary> findHistorySummaries() {
		String sql = """
				SELECT
					s.conversation_id AS conversation_id,
					u.first_user_message AS first_user_message,
					s.last_updated AS last_updated,
					s.message_count AS message_count
				FROM (
					SELECT
						conversation_id,
						MAX("timestamp") AS last_updated,
						COUNT(*) AS message_count
					FROM spring_ai_chat_memory
					GROUP BY conversation_id
				) s
				LEFT JOIN LATERAL (
					SELECT content AS first_user_message
					FROM spring_ai_chat_memory
					WHERE conversation_id = s.conversation_id
					  AND type = 'USER'
					ORDER BY "timestamp" ASC
					LIMIT 1
				) u ON TRUE
				ORDER BY s.last_updated DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> new ChatHistorySummary(
				rs.getString("conversation_id"),
				rs.getString("first_user_message"),
				readInstant(rs, "last_updated"),
				rs.getLong("message_count")
		));
	}

	/**
	 * 특정 login_id 소유 대화만 필터링하여 히스토리 요약 목록을 조회한다. chat_conversation 과 JOIN.
	 */
	public List<ChatHistorySummary> findHistorySummariesByLoginId(String loginId) {
		String sql = """
				SELECT
					s.conversation_id AS conversation_id,
					u.first_user_message AS first_user_message,
					s.last_updated AS last_updated,
					s.message_count AS message_count
				FROM (
					SELECT
						m.conversation_id,
						MAX(m."timestamp") AS last_updated,
						COUNT(*) AS message_count
					FROM spring_ai_chat_memory m
					INNER JOIN chat_conversation c ON c.id = m.conversation_id
					WHERE c.login_id = ?
					GROUP BY m.conversation_id
				) s
				LEFT JOIN LATERAL (
					SELECT content AS first_user_message
					FROM spring_ai_chat_memory
					WHERE conversation_id = s.conversation_id
					  AND type = 'USER'
					ORDER BY "timestamp" ASC
					LIMIT 1
				) u ON TRUE
				ORDER BY s.last_updated DESC
				""";

		return jdbcTemplate.query(sql, ps -> ps.setString(1, loginId), (rs, rowNum) -> new ChatHistorySummary(
				rs.getString("conversation_id"),
				rs.getString("first_user_message"),
				readInstant(rs, "last_updated"),
				rs.getLong("message_count")
		));
	}

	/**
	 * 특정 conversation_id의 모든 메시지를 시간순으로 조회한다. // Spring AI 테이블을 직접 읽어 히스토리 UI/API에 사용
	 */
	public List<ChatMemoryMessage> findMessagesByConversationIdOrderByTimestampAsc(String conversationId) {
		String sql = """
				SELECT
					type AS type,
					content AS content,
					"timestamp" AS ts
				FROM spring_ai_chat_memory
				WHERE conversation_id = ?
				ORDER BY "timestamp" ASC
				""";

		return jdbcTemplate.query(sql, ps -> ps.setString(1, conversationId), (rs, rowNum) -> new ChatMemoryMessage(
				rs.getString("type"),
				rs.getString("content"),
				readInstant(rs, "ts")
		));
	}

	/**
	 * DB/드라이버별 timestamp 표현 차이를 흡수하여 Instant로 변환한다. // 스키마 초기화 방식(always/never)과 무관
	 */
	private static Instant readInstant(ResultSet rs, String columnLabel) throws SQLException {
		Object value = rs.getObject(columnLabel);
		if (value == null) {
			return null;
		}
		if (value instanceof Instant instant) {
			return instant;
		}
		if (value instanceof java.sql.Timestamp ts) {
			return ts.toInstant();
		}
		if (value instanceof OffsetDateTime odt) {
			return odt.toInstant();
		}
		if (value instanceof LocalDateTime ldt) {
			return ldt.toInstant(ZoneOffset.UTC);
		}
		if (value instanceof Long epochMillis) {
			return Instant.ofEpochMilli(epochMillis);
		}
		if (value instanceof Integer epochSeconds) {
			return Instant.ofEpochSecond(epochSeconds.longValue());
		}
		if (value instanceof String text) {
			return Instant.parse(text);
		}
		throw new SQLException("Unsupported timestamp column type: " + value.getClass().getName());
	}

	/**
	 * 히스토리 목록 요약 DTO. // Service에서 제목 트렁케이션 등 UI용 가공 수행
	 */
	public record ChatHistorySummary(
			String conversationId,
			String firstUserMessage,
			Instant lastUpdated,
			long messageCount
	) {}

	/**
	 * 히스토리 상세(메시지) 조회용 Row DTO. // Spring AI message type(USER/ASSISTANT/SYSTEM 등) 유지
	 */
	public record ChatMemoryMessage(
			String type,
			String content,
			Instant timestamp
	) {}
}

