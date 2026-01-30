package minkyu307.spring_ai.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 문서 적재 파이프라인 설정.
 */
@Configuration
public class RagIngestionConfig {

	/**
	 * 문서 청킹(Chunking)을 위한 토큰 기반 Splitter.
	 */
	@Bean
	public TokenTextSplitter ragTokenTextSplitter() {
		return TokenTextSplitter.builder()
				.withChunkSize(800)
				.withMinChunkSizeChars(200)
				.withMinChunkLengthToEmbed(10)
				.withKeepSeparator(true)
				.build();
	}
}

