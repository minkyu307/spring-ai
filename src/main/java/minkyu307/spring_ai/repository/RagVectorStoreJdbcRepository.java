package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.dto.RagDocumentListItemDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}

