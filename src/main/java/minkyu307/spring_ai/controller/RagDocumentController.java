package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.RagDocumentListItemDto;
import minkyu307.spring_ai.dto.RagMultiFileIngestResponse;
import minkyu307.spring_ai.dto.RagUrlIngestRequest;
import minkyu307.spring_ai.dto.RagWikiIngestRequest;
import minkyu307.spring_ai.exception.ResourceNotFoundException;
import minkyu307.spring_ai.service.RagDocumentManagementService;
import minkyu307.spring_ai.service.RagFileUploadService;
import minkyu307.spring_ai.service.RagUrlIngestionService;
import minkyu307.spring_ai.service.RagWikiIngestionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG 문서 적재 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/rag/documents")
public class RagDocumentController {

	private final RagDocumentManagementService managementService;
	private final RagFileUploadService fileUploadService;
	private final RagUrlIngestionService urlIngestionService;
	private final RagWikiIngestionService wikiIngestionService;

	public RagDocumentController(
			RagDocumentManagementService managementService,
			RagFileUploadService fileUploadService,
			RagUrlIngestionService urlIngestionService,
			RagWikiIngestionService wikiIngestionService
	) {
		this.managementService = managementService;
		this.fileUploadService = fileUploadService;
		this.urlIngestionService = urlIngestionService;
		this.wikiIngestionService = wikiIngestionService;
	}

	/**
	 * Vector DB에 적재된 문서 목록을 조회한다. // docId 단위로 그룹핑
	 */
	@GetMapping
	public ResponseEntity<List<RagDocumentListItemDto>> list() {
		return ResponseEntity.ok(managementService.listDocuments());
	}

	/**
	 * 파일(txt/md 등)을 업로드 받아 VectorStore(PGvector)에 적재한다.
	 */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<RagMultiFileIngestResponse> upload(@RequestPart(value = "file", required = false) List<MultipartFile> files) {
		return ResponseEntity.ok(fileUploadService.upload(files));
	}

	/**
	 * 단일 URL 문서를 읽어 VectorStore(PGvector)에 적재한다. // 크롤링/인증 제외
	 */
	@PostMapping("/url")
	public ResponseEntity<?> ingestUrl(@RequestBody RagUrlIngestRequest request) {
		return ResponseEntity.ok(urlIngestionService.ingest(request));
	}

	/**
	 * Dooray Wiki 페이지 목록을 VectorStore(PGvector)에 적재한다.
	 */
	@PostMapping("/wiki")
	public ResponseEntity<?> ingestWiki(@RequestBody RagWikiIngestRequest request) {
		return ResponseEntity.ok(wikiIngestionService.ingest(request));
	}

	/**
	 * 문서를 삭제한다. // docId(메타데이터) 또는 row id(uuid) 기반. 미존재 시 404.
	 */
	@DeleteMapping("/{docId}")
	public ResponseEntity<Void> delete(@PathVariable String docId) {
		if (!managementService.deleteDocument(docId)) {
			throw new ResourceNotFoundException("문서를 찾을 수 없습니다: " + docId);
		}
		return ResponseEntity.noContent().build();
	}
}

