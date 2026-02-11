package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.RagDocumentListItemDto;
import minkyu307.spring_ai.repository.RagVectorStoreJdbcRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 문서 관리(목록/삭제) 서비스. // vector_store를 "문서 단위"로 다루기 위한 얇은 계층
 */
@Service
public class RagDocumentManagementService {

	private final RagVectorStoreJdbcRepository repository;
	private final VectorStore vectorStore;

	public RagDocumentManagementService(RagVectorStoreJdbcRepository repository, VectorStore vectorStore) {
		this.repository = repository;
		this.vectorStore = vectorStore;
	}

	/**
	 * docId 단위로 그룹핑된 문서 목록을 조회한다. // UI 목록용
	 * 현재 로그인 사용자(loginId) 기준으로만 조회.
	 */
	public List<RagDocumentListItemDto> listDocuments() {
		String loginId = SecurityUtils.getCurrentLoginId();
		return repository.findAllGroupedDocumentsByLoginId(loginId);
	}

	/**
	 * 문서를 삭제한다. // docId + loginId 메타데이터 기준으로 해당 문서 청크 전체 삭제. 존재하지 않으면 false.
	 */
	public boolean deleteDocument(String docId) {
		if (docId == null || docId.isBlank()) {
			return false;
		}

		String loginId = SecurityUtils.getCurrentLoginId();

		if (!repository.existsByDocIdAndLoginId(docId, loginId)) {
			return false;
		}

		FilterExpressionBuilder b = new FilterExpressionBuilder();
		// docId는 업로드 시 UUID로 생성되어 사실상 충돌 가능성이 매우 낮지만,
		// 방어적으로 loginId까지 함께 필터링한다.
		vectorStore.delete(
				b.and(
						b.eq("docId", docId),
						b.eq("loginId", loginId)
				).build()
		);
		return true;
	}
}

