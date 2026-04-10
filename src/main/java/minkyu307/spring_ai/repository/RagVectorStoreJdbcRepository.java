package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.dto.RagDocumentListItemDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PGvector vector_store 테이블 조회/관리용 JDBC Repository. // VectorStore에 "목록" API가 없어 직접 조회
 */
@Repository
public class RagVectorStoreJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public RagVectorStoreJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * vector_store(청크) 데이터를 docId 단위로 그룹핑하여 목록을 반환한다. // UI 목록용
	 * 현재 로그인 사용자(loginId) 기준으로만 조회.
	 */
	public List<RagDocumentListItemDto> findAllGroupedDocumentsByLoginId(String loginId) {
		String sql = """
				SELECT
					metadata->>'docId' AS doc_id,
					COALESCE(metadata->>'title', metadata->>'filename', 'untitled') AS title,
					MAX(metadata->>'filename') AS filename,
					MAX(metadata->>'source') AS source,
					MAX(metadata->>'ingestedAt') AS ingested_at,
					COUNT(*) AS chunk_count
				FROM vector_store
				WHERE metadata->>'docId' IS NOT NULL
				  AND metadata->>'loginId' = ?
				GROUP BY metadata->>'docId',
						 COALESCE(metadata->>'title', metadata->>'filename', 'untitled')
				ORDER BY MAX(metadata->>'ingestedAt') DESC NULLS LAST
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> new RagDocumentListItemDto(
				rs.getString("doc_id"),
				rs.getString("title"),
				rs.getString("filename"),
				rs.getString("source"),
				rs.getString("ingested_at"),
				rs.getLong("chunk_count")
		), loginId);
	}

	/**
	 * docId + loginId 메타데이터로 저장된 청크가 존재하는지 확인한다. // 삭제 전략 분기용
	 */
	public boolean existsByDocIdAndLoginId(String docId, String loginId) {
		String sql = "SELECT COUNT(*) FROM vector_store WHERE metadata->>'docId' = ? AND metadata->>'loginId' = ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, docId, loginId);
		return count != null && count > 0;
	}

	/**
	 * 문서 요약을 조회한다. // docId + loginId 기준으로 저장된 summary metadata 반환
	 */
	public Optional<StoredSummary> findStoredSummaryByDocIdAndLoginId(String docId, String loginId) {
		String sql = """
				SELECT
					metadata->>'summary' AS summary,
					metadata->>'summarizedAt' AS summarized_at
				FROM vector_store
				WHERE metadata->>'docId' = ?
				  AND metadata->>'loginId' = ?
				  AND COALESCE(metadata->>'summary', '') <> ''
				ORDER BY metadata->>'summarizedAt' DESC NULLS LAST
				LIMIT 1
				""";
		List<StoredSummary> rows = jdbcTemplate.query(
				sql,
				(rs, rowNum) -> new StoredSummary(
						rs.getString("summary"),
						rs.getString("summarized_at")
				),
				docId,
				loginId
		);
		return rows.stream().findFirst();
	}

	/**
	 * 문서 청크 본문 목록을 조회한다. // 요약 입력 생성용
	 */
	public List<String> findChunkContentsByDocIdAndLoginId(String docId, String loginId) {
		String sql = """
				SELECT content
				FROM vector_store
				WHERE metadata->>'docId' = ?
				  AND metadata->>'loginId' = ?
				ORDER BY id
				""";
		return jdbcTemplate.query(
				sql,
				(rs, rowNum) -> rs.getString("content"),
				docId,
				loginId
		);
	}

	/**
	 * 문서 요약을 저장한다. // docId 그룹 전체 청크 metadata에 summary/summarizedAt 반영
	 */
	public int updateSummaryByDocIdAndLoginId(String docId, String loginId, String summary, String summarizedAt) {
		String sql = """
				UPDATE vector_store
				SET metadata = (
					COALESCE(metadata::jsonb, '{}'::jsonb)
					|| jsonb_build_object('summary', ?, 'summarizedAt', ?)
				)::json
				WHERE metadata->>'docId' = ?
				  AND metadata->>'loginId' = ?
				""";
		return jdbcTemplate.update(sql, summary, summarizedAt, docId, loginId);
	}

	/**
	 * 지정한 사용자들의 vector_store 청크를 metadata.loginId 기준으로 삭제한다.
	 */
	public int deleteByLoginIds(List<String> loginIds) {
		if (loginIds == null || loginIds.isEmpty()) {
			return 0;
		}

		String placeholders = String.join(", ", java.util.Collections.nCopies(loginIds.size(), "?"));
		String sql = """
				DELETE FROM vector_store
				WHERE metadata->>'loginId' IN (%s)
				""".formatted(placeholders);
		return jdbcTemplate.update(sql, loginIds.toArray());
	}

	/**
	 * 저장된 문서 요약 표현.
	 */
	public record StoredSummary(
			String summary,
			String summarizedAt
	) {
	}
}

