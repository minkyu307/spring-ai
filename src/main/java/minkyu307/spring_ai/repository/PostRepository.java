package minkyu307.spring_ai.repository;

import minkyu307.spring_ai.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 게시글 엔티티 저장소. 목록은 최신순. */
public interface PostRepository extends JpaRepository<Post, Long> {

	List<Post> findAllByOrderByUpdatedAtDesc();
}
