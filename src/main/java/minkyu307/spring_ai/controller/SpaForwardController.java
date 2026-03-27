package minkyu307.spring_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React SPA 엔트리 경로를 index.html로 포워딩한다.
 */
@Controller
public class SpaForwardController {

    /**
     * React 라우트 진입점을 정적 index.html로 연결한다.
     */
    @GetMapping({"/app/note", "/app/board", "/app/chat", "/app/documents"})
    public String forward() {
        return "forward:/app/index.html";
    }
}
