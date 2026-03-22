package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.RagUrlIngestRequest;
import minkyu307.spring_ai.dto.RagUrlIngestResponse;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
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
		DownloadedResource downloaded = downloadResource(uri);
		String filename = resolveFilename(uri, downloaded.contentDisposition(), downloaded.contentType());
		Resource resource = toByteArrayResource(downloaded.content(), filename);

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

	private record DownloadedResource(
			byte[] content,
			String contentType,
			String contentDisposition
	) {
	}

	private static DownloadedResource downloadResource(URI uri) {
		try {
			HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
			connection.setInstanceFollowRedirects(true);
			connection.setConnectTimeout(10_000);
			connection.setReadTimeout(30_000);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "spring-ai-rag-ingestion/1.0");
			connection.setRequestProperty("Accept", "*/*");

			int status = connection.getResponseCode();
			InputStream body = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
			if (body == null) {
				throw new IllegalArgumentException("URL 응답 본문이 없습니다.");
			}

			try (InputStream in = body) {
				byte[] bytes = in.readAllBytes();
				if (bytes.length == 0) {
					throw new IllegalArgumentException("URL 응답 본문이 비어 있습니다.");
				}
				return new DownloadedResource(
						bytes,
						connection.getContentType(),
						connection.getHeaderField("Content-Disposition")
				);
			}
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("URL 다운로드 실패");
		}
	}

	private static Resource toByteArrayResource(byte[] bytes, String filename) {
		return new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		};
	}

	private static String resolveFilename(URI uri, String contentDisposition, String contentType) {
		String fromHeader = filenameFromContentDisposition(contentDisposition);
		String candidate = (fromHeader == null || fromHeader.isBlank()) ? filenameFromUri(uri) : fromHeader;
		String ext = extensionFromContentType(contentType);
		if (!hasExtension(candidate) && ext != null) {
			return candidate + ext;
		}
		return candidate;
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

	private static String filenameFromContentDisposition(String contentDisposition) {
		if (contentDisposition == null || contentDisposition.isBlank()) {
			return null;
		}
		for (String part : contentDisposition.split(";")) {
			String token = part.trim();
			if (token.toLowerCase(Locale.ROOT).startsWith("filename*=")) {
				String value = token.substring("filename*=".length()).trim();
				int idx = value.indexOf("''");
				String encoded = idx >= 0 ? value.substring(idx + 2) : value;
				String decoded = URLDecoder.decode(trimQuotes(encoded), StandardCharsets.UTF_8);
				if (!decoded.isBlank()) {
					return decoded;
				}
			}
			if (token.toLowerCase(Locale.ROOT).startsWith("filename=")) {
				String value = trimQuotes(token.substring("filename=".length()).trim());
				if (!value.isBlank()) {
					return value;
				}
			}
		}
		return null;
	}

	private static String trimQuotes(String value) {
		if (value == null || value.length() < 2) {
			return value;
		}
		if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	private static String extensionFromContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return null;
		}
		String mime = contentType.toLowerCase(Locale.ROOT);
		if (mime.contains(";")) {
			mime = mime.substring(0, mime.indexOf(';')).trim();
		}
		return switch (mime) {
			case "application/pdf" -> ".pdf";
			case "text/markdown" -> ".md";
			case "text/plain" -> ".txt";
			case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
			case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
			default -> null;
		};
	}

	private static boolean hasExtension(String filename) {
		if (filename == null || filename.isBlank()) {
			return false;
		}
		int dotIndex = filename.lastIndexOf('.');
		return dotIndex > 0 && dotIndex < filename.length() - 1;
	}
}

