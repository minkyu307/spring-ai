package minkyu307.spring_ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG를 위한 문서 적재 파이프라인 서비스.
 */
@Service
public class DocumentIngestionService {

	private final VectorStore vectorStore;
	private final TokenTextSplitter splitter;
	private final ChatClient titleChatClient;

	public DocumentIngestionService(VectorStore vectorStore, TokenTextSplitter splitter, ChatClient.Builder chatClientBuilder) {
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

		// 한 번의 적재(한 파일)당 하나의 title만 사용. Reader가 섹션별로 넣은 title은 덮어써서 docId당 단일 행으로 표시되게 함.
		String title = findOrGenerateTitle(enriched, base);
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
	 * 문서 내용을 기반으로 20자 이내 제목을 생성한다. // UI 목록 표시용
	 */
	private String generateTitle(String content, String filename) {
		String fallback = (filename != null && !filename.isBlank())
				? filename
				: firstNCodePoints(content, 20);

		String sample = content == null ? "" : content.strip();
		if (sample.length() > 2000) {
			sample = sample.substring(0, 2000);
		}

		try {
			String prompt = """
					Create a short Korean title (max 20 characters) for the following document.
					Output ONLY the title text, no quotes, no extra words.

					Document:
					%s
					""".formatted(sample);

			String title = titleChatClient.prompt()
					.user(prompt)
					.call()
					.content();

			title = title == null ? "" : title.strip();
			title = stripQuotes(title);
			title = title.replaceAll("\\s+", " ").strip();

			if (title.isBlank()) {
				return fallback;
			}
			return firstNCodePoints(title, 20);
		}
		catch (Exception e) {
			return fallback;
		}
	}

	/**
	 * 유니코드 코드포인트 기준으로 최대 N 글자까지 자른다. // 한글/이모지 안전성 고려
	 */
	private static String firstNCodePoints(String text, int maxCodePoints) {
		if (text == null) {
			return "";
		}
		int[] cps = text.codePoints().toArray();
		if (cps.length <= maxCodePoints) {
			return text.strip();
		}
		return new String(cps, 0, maxCodePoints).strip();
	}

	/**
	 * 모델 출력에서 따옴표를 제거한다. // 제목 단일 라인 보장
	 */
	private static String stripQuotes(String text) {
		String t = text == null ? "" : text.strip();
		if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
			return t.substring(1, t.length() - 1).strip();
		}
		return t;
	}

	/** Reader가 넣은 섹션 제목(예: [1. 개요]) 대신, 첫 청크로 생성하거나 파일명을 fallback으로 사용. */
	private String findOrGenerateTitle(List<Document> documents, Map<String, Object> baseMetadata) {
		String filename = baseMetadata != null ? (String) baseMetadata.get("filename") : null;
		String sample = documents.get(0).getText();
		return generateTitle(sample, filename);
	}
}

