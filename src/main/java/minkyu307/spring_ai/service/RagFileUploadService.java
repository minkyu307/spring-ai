package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.RagFileIngestResultDto;
import minkyu307.spring_ai.dto.RagMultiFileIngestResponse;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 멀티 파일 업로드 → VectorStore 적재 결과를 담당하는 서비스.
 */
@Service
public class RagFileUploadService {

	/** 업로드·적재 허용 확장자(소문자, 점 포함). // RagResourceDocumentReaderService 지원 형식과 맞출 것 */
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
			".txt", ".md", ".markdown", ".pdf",
			".docx", ".pptx"
	);

	private final DocumentIngestionService ingestionService;
	private final RagResourceDocumentReaderService readerService;

	public RagFileUploadService(
			DocumentIngestionService ingestionService,
			RagResourceDocumentReaderService readerService) {
		this.ingestionService = ingestionService;
		this.readerService = readerService;
	}

	/**
	 * 업로드된 파일 목록을 읽어 VectorStore에 적재하고, 파일별 결과를 반환한다.
	 */
	public RagMultiFileIngestResponse upload(List<MultipartFile> files) {
		List<MultipartFile> safeFiles = files == null ? List.of() : files;
		String loginId = SecurityUtils.getCurrentLoginId();

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

			if (!isSupportedExtension(lower)) {
				failed++;
				results.add(new RagFileIngestResultDto(filename, "FAILED", 0, "지원하지 않는 파일 형식"));
				continue;
			}

			byte[] bytes;
			try {
				bytes = file.getBytes();
			} catch (Exception e) {
				failed++;
				results.add(new RagFileIngestResultDto(filename, "FAILED", 0, "파일 읽기 실패"));
				continue;
			}

			try {
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("source", "upload");
				metadata.put("filename", filename);
				metadata.put("contentType", file.getContentType());
				metadata.put("loginId", loginId);

				var resource = new ByteArrayResource(bytes) {
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
			} catch (Exception e) {
				failed++;
				String msg = (e.getMessage() == null || e.getMessage().isBlank()) ? "적재 실패" : e.getMessage();
				results.add(new RagFileIngestResultDto(filename, "FAILED", 0, msg));
			}
		}

		return new RagMultiFileIngestResponse(
				safeFiles.size(),
				processed,
				succeeded,
				failed,
				skipped,
				totalChunks,
				results
		);
	}

	/** 파일명(소문자)이 허용 확장자로 끝나는지 검사한다. */
	private static boolean isSupportedExtension(String filenameLower) {
		return ALLOWED_EXTENSIONS.stream().anyMatch(filenameLower::endsWith);
	}
}
