package com.orbe.social.repository;

import com.orbe.social.model.Comment;
import com.orbe.social.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query("select c from Comment c join fetch c.author where c.post = :post order by c.createdAt asc")
	List<Comment> findByPost(@Param("post") Post post);

	void deleteByPost(Post post);
}
