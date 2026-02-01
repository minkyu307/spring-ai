package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.DocumentIngestRequest;
import minkyu307.spring_ai.dto.DocumentIngestResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
		metadata.putIfAbsent("docId", UUID.randomUUID().toString());

		if (!metadata.containsKey("title")) {
			String generatedTitle = generateTitle(request.content(), (String) metadata.get("filename"));
			metadata.put("title", generatedTitle);
		}

		Document document = new Document(request.content(), metadata);
		List<Document> chunks = splitter.apply(List.of(document));

		vectorStore.add(chunks);
		return new DocumentIngestResponse(chunks.size());
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
}

