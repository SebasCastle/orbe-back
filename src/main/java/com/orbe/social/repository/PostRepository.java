package com.orbe.social.repository;

import com.orbe.social.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("select p from Post p join fetch p.author order by p.createdAt desc")
	List<Post> latest();
}
