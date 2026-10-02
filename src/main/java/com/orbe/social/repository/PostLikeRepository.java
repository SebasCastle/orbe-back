package com.orbe.social.repository;

import com.orbe.auth.model.UserAccount;
import com.orbe.social.model.Post;
import com.orbe.social.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

	Optional<PostLike> findByUserAndPost(UserAccount user, Post post);

	long countByPost(Post post);

	void deleteByPost(Post post);

	boolean existsByUserAndPost(UserAccount user, Post post);
}
