package minkyu307.spring_ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI ChatMemory 설정
 * PostgreSQL 데이터베이스에 채팅 메모리를 영구 저장
 */
@Configuration
public class ChatMemoryConfig {

	/**
	 * JdbcChatMemoryRepository 빈 등록
	 * PostgreSQL 데이터베이스에 채팅 메모리 저장
	 */
	@Bean
	public JdbcChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
		return JdbcChatMemoryRepository.builder()
				.jdbcTemplate(jdbcTemplate)
				.build();
	}

	/**
	 * MessageWindowChatMemory 빈 등록
	 * 최근 20개 메시지만 유지하는 슬라이딩 윈도우 방식
	 * JdbcChatMemoryRepository를 사용하여 PostgreSQL에 영구 저장
	 */
	@Bean
	public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder()
				.chatMemoryRepository(chatMemoryRepository)
				.maxMessages(20)  // 최대 20개 메시지 유지
				.build();
	}
}
