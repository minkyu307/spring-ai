package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 댓글 엔티티 저장소. 글별 댓글은 작성순. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByPostPostIdOrderByUpdatedAtAsc(Long postId);
}
