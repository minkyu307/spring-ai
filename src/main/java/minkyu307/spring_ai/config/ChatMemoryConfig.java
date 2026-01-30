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
	 * JdbcChatMemoryRepository 빈 등록 (빈 이름: jdbcChatMemoryRepository)
	 * spring_ai_chat_memory 테이블은 Spring AI JDBC 경로로만 사용 // JPA 엔티티 매핑 제거
	 */
	@Bean
	JdbcChatMemoryRepository jdbcChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		return JdbcChatMemoryRepository.builder()
				.jdbcTemplate(jdbcTemplate)
				.build();
	}

	/**
	 * MessageWindowChatMemory 빈 등록
	 * jdbcChatMemoryRepository를 사용하여 PostgreSQL에 영구 저장
	 */
	@Bean
	ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
		return MessageWindowChatMemory.builder()
				.chatMemoryRepository(jdbcChatMemoryRepository)
				.maxMessages(20)  // 최대 20개 메시지 유지
				.build();
	}
}
