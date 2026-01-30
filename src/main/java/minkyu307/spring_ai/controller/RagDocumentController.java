package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.DocumentIngestRequest;
import minkyu307.spring_ai.dto.DocumentIngestResponse;
import minkyu307.spring_ai.service.DocumentIngestionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG 문서 적재 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/rag/documents")
public class RagDocumentController {

	private final DocumentIngestionService ingestionService;

	public RagDocumentController(DocumentIngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	/**
	 * 텍스트 문서를 VectorStore(PGvector)에 적재한다.
	 */
	@PostMapping
	public ResponseEntity<DocumentIngestResponse> ingest(@RequestBody DocumentIngestRequest request) {
		return ResponseEntity.ok(ingestionService.ingest(request));
	}

	/**
	 * 파일(txt/md 등)을 업로드 받아 VectorStore(PGvector)에 적재한다.
	 */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DocumentIngestResponse> upload(@RequestPart("file") MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.ok(new DocumentIngestResponse(0));
		}

		String filename = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
		String lower = filename.toLowerCase();
		if (!(lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown"))) {
			return ResponseEntity.badRequest().body(new DocumentIngestResponse(0));
		}

		String content;
		try {
			content = new String(file.getBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(new DocumentIngestResponse(0));
		}

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("source", "upload");
		metadata.put("filename", filename);
		metadata.put("contentType", file.getContentType());

		return ResponseEntity.ok(ingestionService.ingest(new DocumentIngestRequest(content, metadata)));
	}
}

