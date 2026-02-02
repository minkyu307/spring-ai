package minkyu307.spring_ai.controller;

import minkyu307.spring_ai.dto.ApiErrorResponse;
import minkyu307.spring_ai.dto.RagDocumentListItemDto;
import minkyu307.spring_ai.dto.RagFileIngestResultDto;
import minkyu307.spring_ai.dto.RagMultiFileIngestResponse;
import minkyu307.spring_ai.dto.RagUrlIngestRequest;
import minkyu307.spring_ai.service.RagDocumentManagementService;
import minkyu307.spring_ai.service.DocumentIngestionService;
import minkyu307.spring_ai.service.RagUrlIngestionService;
import minkyu307.spring_ai.service.RagResourceDocumentReaderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG 문서 적재 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/rag/documents")
public class RagDocumentController {

	private final DocumentIngestionService ingestionService;
	private final RagDocumentManagementService managementService;
	private final RagUrlIngestionService urlIngestionService;
	private final RagResourceDocumentReaderService readerService;

	public RagDocumentController(
			DocumentIngestionService ingestionService,
			RagDocumentManagementService managementService,
			RagUrlIngestionService urlIngestionService,
			RagResourceDocumentReaderService readerService
	) {
		this.ingestionService = ingestionService;
		this.managementService = managementService;
		this.urlIngestionService = urlIngestionService;
		this.readerService = readerService;
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
	public ResponseEntity<?> upload(@RequestPart(value = "file", required = false) List<MultipartFile> files) {
		List<MultipartFile> safeFiles = files == null ? List.of() : files;

		List<RagFileIngestResultDto> results = new ArrayList<>();
		int processed = 0;
		int succeeded = 0;
		int failed = 0;
		int skipped = 0;
		int totalChunks = 0;

		for (MultipartFile file : safeFiles) {
			if (file == null || file.isEmpty()) {
				skipped++;
				results.add(new RagFileIngestResultDto("(empty)", "SKIPPED", 0, null));
				continue;
			}

			processed++;
			String filename = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
			String lower = filename.toLowerCase();

			if (!(lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".pdf"))) {
				failed++;
				results.add(new RagFileIngestResultDto(filename, "FAILED", 0, "지원하지 않는 파일 형식"));
				continue;
			}

			byte[] bytes;
			try {
				bytes = file.getBytes();
			}
			catch (Exception e) {
				failed++;
				results.add(new RagFileIngestResultDto(filename, "FAILED", 0, "파일 읽기 실패"));
				continue;
			}

			try {
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("source", "upload");
				metadata.put("filename", filename);
				metadata.put("contentType", file.getContentType());

				// 파일 타입별 Reader로 Document 생성 후 공통 ingestion 처리
				var resource = new org.springframework.core.io.ByteArrayResource(bytes) {
					@Override
					public String getFilename() {
						return filename;
					}
				};

				var readResult = readerService.read(resource, filename, null);

				Map<String, Object> baseMetadata = new HashMap<>(metadata);
				baseMetadata.put("detectedType", readResult.detectedType().name());

				int chunks = ingestionService.ingestDocuments(readResult.documents(), baseMetadata).chunksIngested();
				totalChunks += chunks;
				succeeded++;
				results.add(new RagFileIngestResultDto(filename, "SUCCEEDED", chunks, null));
			}
			catch (Exception e) {
				failed++;
				String msg = (e.getMessage() == null || e.getMessage().isBlank()) ? "적재 실패" : e.getMessage();
				results.add(new RagFileIngestResultDto(filename, "FAILED", 0, msg));
			}
		}

		// 멀티 업로드 결과를 상세 반환 // UI에서 파일별 성공/실패 표시용
		return ResponseEntity.ok(new RagMultiFileIngestResponse(
				safeFiles.size(),
				processed,
				succeeded,
				failed,
				skipped,
				totalChunks,
				results
		));
	}

	/**
	 * 단일 URL 문서를 읽어 VectorStore(PGvector)에 적재한다. // 크롤링/인증 제외
	 */
	@PostMapping("/url")
	public ResponseEntity<?> ingestUrl(@RequestBody RagUrlIngestRequest request) {
		try {
			return ResponseEntity.ok(urlIngestionService.ingest(request));
		}
		catch (IllegalArgumentException e) {
			String message = (e.getMessage() == null || e.getMessage().isBlank())
					? "잘못된 요청입니다."
					: e.getMessage();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorResponse(message));
		}
		catch (Exception e) {
			String message = (e.getMessage() == null || e.getMessage().isBlank())
					? "URL 적재 중 오류가 발생했습니다."
					: e.getMessage();
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiErrorResponse(message));
		}
	}

	/**
	 * 문서를 삭제한다. // docId(메타데이터) 또는 row id(uuid) 기반
	 */
	@DeleteMapping("/{docId}")
	public ResponseEntity<Void> delete(@PathVariable String docId) {
		managementService.deleteDocument(docId);
		return ResponseEntity.noContent().build();
	}
}

