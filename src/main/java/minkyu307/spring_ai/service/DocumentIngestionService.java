package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.DocumentIngestRequest;
import minkyu307.spring_ai.dto.DocumentIngestResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RAG를 위한 문서 적재 파이프라인 서비스.
 */
@Service
public class DocumentIngestionService {

	private final VectorStore vectorStore;
	private final TokenTextSplitter splitter;

	public DocumentIngestionService(VectorStore vectorStore, TokenTextSplitter splitter) {
		this.vectorStore = vectorStore;
		this.splitter = splitter;
	}

	/**
	 * 입력 문서를 청크로 분할한 뒤 VectorStore(PGvector)에 저장한다.
	 */
	public DocumentIngestResponse ingest(DocumentIngestRequest request) {
		if (request == null || request.content() == null || request.content().isBlank()) {
			return new DocumentIngestResponse(0);
		}

		Map<String, Object> metadata = request.metadata() == null ? Map.of() : request.metadata();
		metadata = new java.util.HashMap<>(metadata);
		metadata.putIfAbsent("source", "api");
		metadata.putIfAbsent("ingestedAt", Instant.now().toString());

		Document document = new Document(request.content(), metadata);
		List<Document> chunks = splitter.apply(List.of(document));

		vectorStore.add(chunks);
		return new DocumentIngestResponse(chunks.size());
	}
}

