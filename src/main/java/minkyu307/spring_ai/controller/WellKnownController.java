package minkyu307.spring_ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chrome DevTools 등이 요청하는 .well-known 경로 처리. 404 방지용.
 */
@RestController
public class WellKnownController {

	@GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
	public ResponseEntity<String> chromeDevTools() {
		return ResponseEntity.ok("{}");
	}
}
