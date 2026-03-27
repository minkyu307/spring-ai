package minkyu307.spring_ai.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import minkyu307.spring_ai.config.TraceIdFilter;
import minkyu307.spring_ai.error.ApiErrorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalApiExceptionHandlerMockMvcTest {

	private MockMvc mockMvc;
	private LocalValidatorFactoryBean validator;

	@BeforeEach
	void setUp() {
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
			.standaloneSetup(new TestErrorController())
			.setControllerAdvice(new GlobalApiExceptionHandler(new ApiErrorFactory()))
			.setValidator(validator)
			.addFilters(new TraceIdFilter())
			.build();
	}

	@AfterEach
	void tearDown() {
		validator.close();
	}

	@Test
	void illegalArgumentExceptionIsMappedToCustomEnvelope() throws Exception {
		mockMvc.perform(get("/test/errors/bad-request"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
			.andExpect(jsonPath("$.message").value("제목은 필수입니다."))
			.andExpect(jsonPath("$.details.status").value(400))
			.andExpect(jsonPath("$.traceId").isNotEmpty())
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void validationFailureIsMappedToCustomEnvelope() throws Exception {
		mockMvc.perform(
				post("/test/errors/validate")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\"}")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.message").value("name은 필수입니다."))
			.andExpect(jsonPath("$.details.fieldErrors.name").value("name은 필수입니다."))
			.andExpect(jsonPath("$.traceId").isNotEmpty())
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@RestController
	@RequestMapping("/test/errors")
	static class TestErrorController {

		@GetMapping("/bad-request")
		public Map<String, String> badRequest() {
			throw new IllegalArgumentException("제목은 필수입니다.");
		}

		@PostMapping("/validate")
		public Map<String, String> validate(@Valid @RequestBody TestRequest request) {
			return Map.of("name", request.name());
		}
	}

	record TestRequest(
		@NotBlank(message = "name은 필수입니다.")
		String name
	) {
	}
}
