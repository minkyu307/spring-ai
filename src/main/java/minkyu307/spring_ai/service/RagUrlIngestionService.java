package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.RagUrlIngestRequest;
import minkyu307.spring_ai.dto.RagUrlIngestResponse;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 단일 URL 문서 적재 전용 서비스. // 파일 업로드 ingest와 분리
 */
@Service
public class RagUrlIngestionService {

	private final DocumentIngestionService ingestionService;
	private final RagResourceDocumentReaderService readerService;

	public RagUrlIngestionService(DocumentIngestionService ingestionService, RagResourceDocumentReaderService readerService) {
		this.ingestionService = ingestionService;
		this.readerService = readerService;
	}

	public RagUrlIngestResponse ingest(RagUrlIngestRequest request) {
		if (request == null || request.url() == null || request.url().isBlank()) {
			throw new IllegalArgumentException("url is required");
		}

		URI uri = URI.create(request.url().trim());
		String loginId = SecurityUtils.getCurrentLoginId();
		String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
		if (!scheme.equals("http") && !scheme.equals("https")) {
			throw new IllegalArgumentException("Only http/https URL is supported");
		}

		String docId = UUID.randomUUID().toString();
		String filename = filenameFromUri(uri);

		Resource resource;
		try {
			resource = new UrlResource(uri);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid URL");
		}
		RagResourceDocumentReaderService.ReadResult readResult =
				readerService.read(resource, filename, uri.toString());
		List<Document> documents = readResult.documents();

		Map<String, Object> baseMetadata = new HashMap<>();
		baseMetadata.put("source", "url");
		baseMetadata.put("url", uri.toString());
		baseMetadata.put("filename", filename);
		baseMetadata.put("docId", docId);
		baseMetadata.put("ingestedAt", Instant.now().toString());
		baseMetadata.put("detectedType", readResult.detectedType().name());
		baseMetadata.put("loginId", loginId);

		DocumentIngestionService.IngestionResult result =
				ingestionService.ingestDocuments(documents, baseMetadata);

		return new RagUrlIngestResponse(
				result.docId(),
				uri.toString(),
				readResult.detectedType().name(),
				result.documentsRead(),
				result.chunksIngested(),
				result.title()
		);
	}

	private static String filenameFromUri(URI uri) {
		String path = uri.getPath();
		if (path == null || path.isBlank() || path.endsWith("/")) {
			return "url";
		}
		int idx = path.lastIndexOf('/');
		String name = idx >= 0 ? path.substring(idx + 1) : path;
		return name.isBlank() ? "url" : name;
	}
}

