package com.orbe.social.controller;

import com.orbe.social.dto.SocialDtos.CommentRequest;
import com.orbe.social.dto.SocialDtos.CommentView;
import com.orbe.social.dto.SocialDtos.Overview;
import com.orbe.social.dto.SocialDtos.PostRequest;
import com.orbe.social.dto.SocialDtos.PostView;
import com.orbe.social.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

	private final CommunityService community;

	public CommunityController(CommunityService community) {
		this.community = community;
	}

	@GetMapping
	public Overview overview() {
		return community.overview();
	}

	@PostMapping("/posts")
	public ResponseEntity<PostView> create(@Valid @RequestBody PostRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(community.createPost(request.title(), request.body()));
	}

	@DeleteMapping("/posts/{id}")
	public ResponseEntity<Void> deletePost(@PathVariable Long id) {
		community.deletePost(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/posts/{id}/comments")
	public ResponseEntity<CommentView> comment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(community.comment(id, request.body()));
	}

	@DeleteMapping("/comments/{id}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
		community.deleteComment(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/posts/{id}/likes")
	public PostView like(@PathVariable Long id) {
		return community.toggleLike(id);
	}
}
