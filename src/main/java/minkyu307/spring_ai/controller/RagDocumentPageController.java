package minkyu307.spring_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * RAG 문서 관리 화면 컨트롤러. // vector_store 기반 문서 목록/업로드/삭제 UI 제공
 */
@Controller
public class RagDocumentPageController {

	@GetMapping("/rag/documents")
	public String ragDocumentsPage() {
		return "rag-documents";
	}
}

