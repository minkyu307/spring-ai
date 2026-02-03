package minkyu307.spring_ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.extractor.POITextExtractor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 다양한 소스(Resource)에서 Document를 생성하는 공통 Reader 서비스.
 * - HTML: JsoupDocumentReader (본문 selector 우선, 실패 시 body 전체)
 * - Markdown: MarkdownDocumentReader
 * - Text: TextReader
 * - PDF: 페이지 단위 텍스트 추출 + (텍스트 부족 시) 이미지 기반 OCR(멀티모달 LLM) fallback
 * - DOCX/PPTX: Apache POI(poi-ooxml)로 텍스트 추출
 */
@Service
public class RagResourceDocumentReaderService {

	private static final String HTML_AUTO_SELECTOR =
			"article, main, [role=main], #content, .content, .markdown-body, .wiki-content, .doc-content, .document-content";

	private static final int PDF_OCR_MIN_TEXT_LENGTH = 20;
	private static final int PDF_OCR_RENDER_DPI = 160;

	private final ChatClient ocrChatClient;

	public RagResourceDocumentReaderService(ChatClient.Builder chatClientBuilder) {
		this.ocrChatClient = chatClientBuilder.build();
	}

	public enum DetectedType {
		HTML, PDF, MARKDOWN, TEXT, DOCX, PPTX
	}

	public record ReadResult(
			DetectedType detectedType,
			List<Document> documents
	) {
	}

	/**
	 * 파일명/URL 경로 기반으로 타입을 판별하고 Document를 생성한다.
	 */
	public ReadResult read(Resource resource, String filenameOrPath, String sourceUrlOrNull) {
		DetectedType type = detectType(filenameOrPath, sourceUrlOrNull);
		List<Document> docs = switch (type) {
			case PDF -> readPdfPagesWithOcr(resource, filenameOrPath);
			case MARKDOWN -> readMarkdown(resource, filenameOrPath, sourceUrlOrNull);
			case TEXT -> readText(resource, filenameOrPath, sourceUrlOrNull);
			case HTML -> readHtml(resource, sourceUrlOrNull);
			case DOCX -> readDocx(resource, filenameOrPath, sourceUrlOrNull);
			case PPTX -> readPptx(resource, filenameOrPath, sourceUrlOrNull);
		};
		return new ReadResult(type, docs);
	}

	private static DetectedType detectType(String filenameOrPath, String sourceUrlOrNull) {
		String base = filenameOrPath == null ? "" : filenameOrPath.toLowerCase();
		String url = sourceUrlOrNull == null ? "" : sourceUrlOrNull.toLowerCase();
		String target = !base.isBlank() ? base : url;

		if (target.endsWith(".pdf")) {
			return DetectedType.PDF;
		}
		if (target.endsWith(".docx")) {
			return DetectedType.DOCX;
		}
		if (target.endsWith(".pptx")) {
			return DetectedType.PPTX;
		}
		if (target.endsWith(".md") || target.endsWith(".markdown")) {
			return DetectedType.MARKDOWN;
		}
		if (target.endsWith(".txt")) {
			return DetectedType.TEXT;
		}
		return DetectedType.HTML;
	}

	private static List<Document> readMarkdown(Resource resource, String filename, String sourceUrlOrNull) {
		MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
				.withHorizontalRuleCreateDocument(true)
				.withIncludeCodeBlock(false)
				.withIncludeBlockquote(false)
				.withAdditionalMetadata("filename", filename == null ? "unknown" : filename)
				.withAdditionalMetadata("source", sourceUrlOrNull == null ? "upload" : sourceUrlOrNull)
				.build();
		return new MarkdownDocumentReader(resource, config).read();
	}

	private static List<Document> readText(Resource resource, String filename, String sourceUrlOrNull) {
		TextReader reader = new TextReader(resource);
		reader.setCharset(StandardCharsets.UTF_8);
		reader.getCustomMetadata().put("filename", filename == null ? "unknown" : filename);
		reader.getCustomMetadata().put("source", sourceUrlOrNull == null ? "upload" : sourceUrlOrNull);
		return reader.read();
	}

	private static List<Document> readHtml(Resource resource, String sourceUrlOrNull) {
		String source = sourceUrlOrNull == null ? "upload" : sourceUrlOrNull;

		JsoupDocumentReaderConfig primary = JsoupDocumentReaderConfig.builder()
				.selector(HTML_AUTO_SELECTOR)
				.charset(StandardCharsets.UTF_8.name())
				.includeLinkUrls(false)
				.additionalMetadata("source", source)
				.build();

		List<Document> docs = new JsoupDocumentReader(resource, primary).read();
		int totalChars = docs.stream()
				.map(Document::getText)
				.mapToInt(s -> s == null ? 0 : s.length())
				.sum();

		if (docs.isEmpty() || totalChars < 400) {
			JsoupDocumentReaderConfig fallback = JsoupDocumentReaderConfig.builder()
					.allElements(true)
					.charset(StandardCharsets.UTF_8.name())
					.includeLinkUrls(false)
					.additionalMetadata("source", source)
					.build();
			return new JsoupDocumentReader(resource, fallback).read();
		}
		return docs;
	}

	/**
	 * Word(.docx) 문서에서 텍스트를 추출하여 단일 Document로 반환. // Apache POI ExtractorFactory
	 */
	private static List<Document> readDocx(Resource resource, String filename, String sourceUrlOrNull) {
		return extractTextWithPoi(resource, filename, sourceUrlOrNull, "DOCX");
	}

	/**
	 * PowerPoint(.pptx) 문서에서 텍스트를 추출하여 단일 Document로 반환. // Apache POI ExtractorFactory
	 */
	private static List<Document> readPptx(Resource resource, String filename, String sourceUrlOrNull) {
		return extractTextWithPoi(resource, filename, sourceUrlOrNull, "PPTX");
	}

	/** Apache POI ExtractorFactory로 Office 문서 텍스트 추출. // docx/pptx 공통 */
	private static List<Document> extractTextWithPoi(Resource resource, String filename, String sourceUrlOrNull, String formatLabel) {
		try (InputStream in = resource.getInputStream();
				POITextExtractor extractor = ExtractorFactory.createExtractor(in)) {
			String text = extractor.getText();
			if (text == null || text.isBlank()) {
				return List.of();
			}
			Map<String, Object> meta = new HashMap<>();
			meta.put("filename", filename == null ? "unknown" : filename);
			meta.put("source", sourceUrlOrNull == null ? "upload" : sourceUrlOrNull);
			meta.put("extractedAt", Instant.now().toString());
			return List.of(new Document(text.strip(), meta));
		} catch (Exception e) {
			String msg = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
			throw new IllegalArgumentException(formatLabel + " 읽기 실패: " + msg);
		}
	}

	/**
	 * PDF를 페이지 단위 Document로 생성한다.
	 * - 텍스트가 충분하면 PDFBox 텍스트 추출 결과 사용
	 * - 텍스트가 부족하면 해당 페이지를 이미지로 렌더링 후 멀티모달 LLM로 OCR 수행
	 */
	private List<Document> readPdfPagesWithOcr(Resource resource, String filename) {
		byte[] pdfBytes;
		try (InputStream in = resource.getInputStream()) {
			pdfBytes = in.readAllBytes();
		}
		catch (Exception e) {
			throw new IllegalArgumentException("PDF 읽기 실패");
		}

		try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
			PDFRenderer renderer = new PDFRenderer(pdf);
			PDFTextStripper stripper = new PDFTextStripper();

			List<Document> documents = new ArrayList<>();
			int pages = pdf.getNumberOfPages();

			for (int i = 0; i < pages; i++) {
				int pageNumber = i + 1;
				String text = extractPdfTextForPage(stripper, pdf, pageNumber);

				if (text == null || text.isBlank() || text.strip().length() < PDF_OCR_MIN_TEXT_LENGTH) {
					String ocr = ocrPdfPage(renderer, pdf, i, filename, pageNumber);
					if (ocr != null && !ocr.isBlank()) {
						text = ocr;
					}
				}

				if (text == null || text.isBlank()) {
					continue;
				}

				Map<String, Object> meta = new HashMap<>();
				meta.put("filename", filename == null ? "unknown" : filename);
				meta.put("pageNumber", pageNumber);
				meta.put("source", "pdf");
				meta.put("extractedAt", Instant.now().toString());

				documents.add(new Document(text, meta));
			}

			return documents;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("PDF 파싱 실패");
		}
	}

	private static String extractPdfTextForPage(PDFTextStripper stripper, PDDocument pdf, int pageNumber) {
		try {
			stripper.setStartPage(pageNumber);
			stripper.setEndPage(pageNumber);
			return stripper.getText(pdf);
		}
		catch (Exception e) {
			return "";
		}
	}

	/**
	 * PDF 페이지 이미지를 멀티모달 LLM로 OCR한다. // 출력은 "추출 텍스트만" 엄격 강제
	 */
	private String ocrPdfPage(PDFRenderer renderer, PDDocument pdf, int pageIndex, String filename, int pageNumber) {
		try {
			BufferedImage image = renderer.renderImageWithDPI(pageIndex, PDF_OCR_RENDER_DPI);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] pngBytes = baos.toByteArray();

			ByteArrayResource imageResource = new ByteArrayResource(pngBytes) {
				@Override
				public String getFilename() {
					String base = filename == null ? "document" : filename;
					return base + "-page-" + pageNumber + ".png";
				}
			};

			String prompt = """
					Role: You are an OCR engine.

					Goal: Extract ONLY the characters that are actually visible in the attached image (a PDF page) as plain text, as faithfully as possible.

					Output rules (MUST follow):
					1) Output the OCR result text ONLY. Do not add any preface, explanation, summary, apology, or meta commentary.
					2) Do NOT guess or infer missing content. If something is not visible, omit it.
					3) Preserve layout as much as possible: line breaks, paragraphs, bullets, numbering, and indentation.
					4) If a region looks like a table, reproduce it as a Markdown table when confident; otherwise keep the original line-break layout.
					5) For code/commands/logs, output the content exactly as seen. Do not add explanations.
					6) Repeated decorative elements (headers/footers/page numbers) may be omitted ONLY if clearly repetitive and non-content.
					7) If there is almost no readable text, output an empty string.

					Now output ONLY the OCR text.
					""";

			String out = ocrChatClient.prompt()
					.user(u -> u.text(prompt).media(MimeTypeUtils.IMAGE_PNG, imageResource))
					.call()
					.content();

			return out == null ? "" : out.strip();
		}
		catch (Exception e) {
			return "";
		}
	}
}

