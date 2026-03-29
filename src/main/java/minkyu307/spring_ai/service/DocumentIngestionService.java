package minkyu307.spring_ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RAG를 위한 문서 적재 파이프라인 서비스.
 */
@Service
public class DocumentIngestionService {

	private static final int TITLE_SUMMARY_MAX_CHARS = 20;
	private static final int TITLE_SUMMARY_INPUT_MAX_CHARS = 1600;
	private static final int TITLE_SUMMARY_SOURCE_DOC_LIMIT = 3;
	private static final int TITLE_SUMMARY_PER_DOC_MAX_CHARS = 600;
	private static final String DEFAULT_TITLE = "제목 없음";

	private final VectorStore vectorStore;
	private final TokenTextSplitter splitter;
	private final ChatClient titleChatClient;

	public DocumentIngestionService(
			VectorStore vectorStore,
			TokenTextSplitter splitter,
			ChatClient.Builder chatClientBuilder) {
		this.vectorStore = vectorStore;
		this.splitter = splitter;
		this.titleChatClient = chatClientBuilder.build();
	}

	/**
	 * DocumentReader 결과(List&lt;Document&gt;)를 그대로 받아 VectorStore에 적재한다. // URL/PDF/HTML 등 공용
	 */
	public IngestionResult ingestDocuments(List<Document> documents, Map<String, Object> baseMetadata) {
		if (documents == null || documents.isEmpty()) {
			return new IngestionResult(null, null, 0, 0);
		}

		Map<String, Object> base = baseMetadata == null ? Map.of() : baseMetadata;
		String docId = base.get("docId") instanceof String s && !s.isBlank()
				? s
				: UUID.randomUUID().toString();

		String ingestedAt = Instant.now().toString();

		List<Document> enriched = documents.stream()
				.filter(d -> d != null && d.getText() != null && !d.getText().isBlank())
				.map(d -> {
					Map<String, Object> merged = new HashMap<>(d.getMetadata() == null ? Map.of() : d.getMetadata());
					merged.putAll(base);
					merged.putIfAbsent("source", "api");
					merged.putIfAbsent("ingestedAt", ingestedAt);
					merged.putIfAbsent("docId", docId);
					return new Document(d.getText(), merged);
				})
				.toList();

		if (enriched.isEmpty()) {
			return new IngestionResult(docId, null, 0, 0);
		}

		// 한 번의 적재(한 소스)당 하나의 title만 사용한다.
		String title = findOrGenerateTitle(enriched);
		enriched = enriched.stream()
				.map(d -> {
					Map<String, Object> merged = new HashMap<>(d.getMetadata());
					merged.put("title", title);
					return new Document(d.getText(), merged);
				})
				.toList();

		List<Document> chunks = splitter.apply(enriched);
		vectorStore.add(chunks);

		return new IngestionResult(docId, title, enriched.size(), chunks.size());
	}

	/**
	 * 적재 결과(문서 그룹 단위) 반환값.
	 */
	public record IngestionResult(
			String docId,
			String title,
			int documentsRead,
			int chunksIngested
	) {
	}

	/**
	 * 유니코드 코드포인트 기준으로 최대 N 글자까지 자른다.
	 */
	private static String limitCodePoints(String text, int maxCodePoints) {
		if (text == null || text.isBlank()) {
			return "";
		}
		int length = text.codePointCount(0, text.length());
		if (length <= maxCodePoints) {
			return text.strip();
		}
		int end = text.offsetByCodePoints(0, maxCodePoints);
		return text.substring(0, end).strip();
	}

	/**
	 * 줄바꿈/탭을 포함한 공백을 단일 공백으로 정규화한다.
	 */
	private static String normalizeSingleLine(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		return text.replaceAll("\\s+", " ").strip();
	}

	/**
	 * 제목 문자열의 양쪽 따옴표를 제거하고 공백을 정리한다.
	 */
	private static String normalizeTitle(String text) {
		String normalized = normalizeSingleLine(text);
		if (normalized.isBlank()) {
			return "";
		}
		return normalized.replaceAll("^[\"'`“”‘’]+|[\"'`“”‘’]+$", "").strip();
	}

	/**
	 * 제목 생성용 입력 텍스트를 문서 본문에서 구성한다.
	 */
	private static String buildTitleInput(List<Document> documents) {
		return documents.stream()
				.map(Document::getText)
				.filter(Objects::nonNull)
				.map(String::strip)
				.filter(s -> !s.isBlank())
				.limit(TITLE_SUMMARY_SOURCE_DOC_LIMIT)
				.map(s -> limitCodePoints(s, TITLE_SUMMARY_PER_DOC_MAX_CHARS))
				.collect(Collectors.joining("\n\n"));
	}

	/**
	 * AI에 문서 핵심 제목 생성을 요청하고 20자로 강제 절단한다.
	 */
	private String summarizeTitleWithAi(String titleInput) {
		if (titleInput == null || titleInput.isBlank()) {
			return "";
		}
		try {
			String summary = titleChatClient.prompt()
					.system("""
						너는 문서 제목 생성기다.
						제목은 반드시 한 줄만 출력한다.
						제목은 문서 핵심을 반영해 20자 이내로 작성한다.
						따옴표, 마침표, 줄바꿈, 불릿, 접두어를 넣지 않는다.
						""")
					.user("""
						아래 문서를 대표하는 제목을 20자 이내로 생성해줘.
						문서:
						%s
						""".formatted(limitCodePoints(titleInput, TITLE_SUMMARY_INPUT_MAX_CHARS)))
					.call()
					.content();
			String normalized = normalizeTitle(summary);
			return normalized.isBlank() ? "" : limitCodePoints(normalized, TITLE_SUMMARY_MAX_CHARS);
		}
		catch (Exception ignored) {
			// 제목 생성 실패 시 본문 기반 폴백을 사용한다.
			return "";
		}
	}

	/**
	 * 업로드 소스 종류와 무관하게 AI 요약 제목을 생성하고, 실패 시 본문 앞 20자로 폴백한다.
	 */
	private String findOrGenerateTitle(List<Document> documents) {
		String titleInput = buildTitleInput(documents);
		String summarized = summarizeTitleWithAi(titleInput);
		if (!summarized.isBlank()) {
			return summarized;
		}
		String fallback = limitCodePoints(normalizeTitle(titleInput), TITLE_SUMMARY_MAX_CHARS);
		return fallback.isBlank() ? DEFAULT_TITLE : fallback;
	}
}

