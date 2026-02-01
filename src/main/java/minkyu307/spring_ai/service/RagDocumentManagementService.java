package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.RagDocumentListItemDto;
import minkyu307.spring_ai.repository.RagVectorStoreJdbcRepository;
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
	 */
	public List<RagDocumentListItemDto> listDocuments() {
		return repository.findAllGroupedDocuments();
	}

	/**
	 * 문서를 삭제한다. // docId 메타데이터 기준으로 해당 문서 청크 전체 삭제
	 */
	public void deleteDocument(String docId) {
		if (docId == null || docId.isBlank()) {
			return;
		}

		if (!repository.existsByDocIdMetadata(docId)) {
			return;
		}

		FilterExpressionBuilder b = new FilterExpressionBuilder();
		vectorStore.delete(b.eq("docId", docId).build());
	}
}

