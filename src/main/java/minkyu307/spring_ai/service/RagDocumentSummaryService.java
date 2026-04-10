package minkyu307.spring_ai.service;

import minkyu307.spring_ai.dto.RagDocumentSummaryResponse;
import minkyu307.spring_ai.exception.ResourceNotFoundException;
import minkyu307.spring_ai.repository.RagVectorStoreJdbcRepository;
import minkyu307.spring_ai.security.SecurityUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * RAG 문서 요약 서비스. // docId 단위 요약 생성 후 vector_store metadata에 저장/재사용
 */
@Service
public class RagDocumentSummaryService {

	private static final int SUMMARY_SOURCE_CHUNK_LIMIT = 30;
	private static final int SUMMARY_PER_CHUNK_MAX_CHARS = 700;
	private static final int SUMMARY_INPUT_MAX_CHARS = 7000;
	private static final int SUMMARY_OUTPUT_MAX_CHARS = 3000;

	private final RagVectorStoreJdbcRepository repository;
	private final ChatClient summaryChatClient;

	public RagDocumentSummaryService(
			RagVectorStoreJdbcRepository repository,
			ChatClient.Builder chatClientBuilder
	) {
		this.repository = repository;
		this.summaryChatClient = chatClientBuilder.build();
	}

	/**
	 * 문서 요약을 반환한다. // 기존 저장 요약이 있으면 재사용하고, 없으면 생성 후 저장한다.
	 */
	public RagDocumentSummaryResponse summarizeDocument(String docId) {
		if (docId == null || docId.isBlank()) {
			throw new IllegalArgumentException("문서 ID가 올바르지 않습니다.");
		}

		String normalizedDocId = docId.strip();
		String loginId = SecurityUtils.getCurrentLoginId();

		if (!repository.existsByDocIdAndLoginId(normalizedDocId, loginId)) {
			throw new ResourceNotFoundException("문서를 찾을 수 없습니다: " + normalizedDocId);
		}

		var storedSummary = repository.findStoredSummaryByDocIdAndLoginId(normalizedDocId, loginId);
		if (storedSummary.isPresent()) {
			return new RagDocumentSummaryResponse(
					normalizedDocId,
					storedSummary.get().summary(),
					true,
					storedSummary.get().summarizedAt()
			);
		}

		List<String> chunkContents = repository.findChunkContentsByDocIdAndLoginId(normalizedDocId, loginId);
		String summaryInput = buildSummaryInput(chunkContents);
		if (summaryInput.isBlank()) {
			throw new IllegalStateException("요약할 문서 본문이 없습니다.");
		}

		String summary = summarizeWithAi(summaryInput);
		String summarizedAt = Instant.now().toString();
		int updatedRows = repository.updateSummaryByDocIdAndLoginId(normalizedDocId, loginId, summary, summarizedAt);
		if (updatedRows <= 0) {
			throw new ResourceNotFoundException("문서를 찾을 수 없습니다: " + normalizedDocId);
		}

		return new RagDocumentSummaryResponse(normalizedDocId, summary, false, summarizedAt);
	}

	/**
	 * 요약 입력을 생성한다. // 문서 청크 본문 일부를 길이 제한과 함께 결합
	 */
	private static String buildSummaryInput(List<String> chunkContents) {
		String combined = chunkContents.stream()
				.filter(text -> text != null && !text.isBlank())
				.map(String::strip)
				.limit(SUMMARY_SOURCE_CHUNK_LIMIT)
				.map(text -> limitCodePoints(text, SUMMARY_PER_CHUNK_MAX_CHARS))
				.filter(text -> !text.isBlank())
				.reduce((left, right) -> left + "\n\n" + right)
				.orElse("");
		return limitCodePoints(combined, SUMMARY_INPUT_MAX_CHARS);
	}

	/**
	 * AI에 문서 요약을 요청한다. // 실패 시 입력 기반 폴백 요약을 반환
	 */
	private String summarizeWithAi(String summaryInput) {
		String summary;
		try {
			summary = summaryChatClient.prompt()
					.system("""
						너는 문서 요약기다.
						항상 한국어로만 답한다.
						요약 결과는 마크다운 형식으로 작성한다.
						전체 출력은 3000자 이내로 작성한다.
						사용자가 소스의 개요를 빠르게 파악할 수 있게 핵심 주제, 주요 내용, 활용 포인트 중심으로 정리한다.
						""")
					.user("""
						이 소스에 대해 사용자가 개요를 파악할 수 있도록 요약해줘.
						반드시 마크다운 형식으로 작성하고 전체 길이는 3000자 이내로 제한해줘.
						소스:
						%s
						""".formatted(summaryInput))
					.call()
					.content();
		}
		catch (Exception ignored) {
			return buildFallbackMarkdownSummary(summaryInput);
		}

		String normalized = normalizeSummary(summary);
		if (!normalized.isBlank()) {
			return limitCodePoints(normalized, SUMMARY_OUTPUT_MAX_CHARS);
		}
		return buildFallbackMarkdownSummary(summaryInput);
	}

	/**
	 * 요약 텍스트를 정규화한다. // 불필요한 공백과 과도한 줄바꿈을 정리
	 */
	private static String normalizeSummary(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		String normalized = text
				.replace("\r\n", "\n")
				.replace('\r', '\n')
				.replaceAll("\n{4,}", "\n\n\n")
				.strip();
		return normalized.replaceAll("^[\"'`“”‘’]+|[\"'`“”‘’]+$", "").strip();
	}

	/**
	 * 요약 생성 실패 시 마크다운 폴백을 만든다. // 요약 UI가 Markdown 렌더링 가능한 형태 유지
	 */
	private static String buildFallbackMarkdownSummary(String summaryInput) {
		String fallback = normalizeSummary(summaryInput);
		if (fallback.isBlank()) {
			return "## 소스 개요\n\n요약 결과가 없습니다.";
		}
		String clipped = limitCodePoints(fallback, Math.max(0, SUMMARY_OUTPUT_MAX_CHARS - 20));
		return limitCodePoints("## 소스 개요\n\n" + clipped, SUMMARY_OUTPUT_MAX_CHARS);
	}

	/**
	 * 유니코드 코드포인트 기준으로 최대 길이를 제한한다.
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
}

