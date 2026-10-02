package com.orbe.social.service;

import com.orbe.auth.model.UserAccount;
import com.orbe.common.ApiException;
import com.orbe.security.CurrentUser;
import com.orbe.social.dto.SocialDtos.CommentView;
import com.orbe.social.dto.SocialDtos.OpinionView;
import com.orbe.social.dto.SocialDtos.Overview;
import com.orbe.social.dto.SocialDtos.PostView;
import com.orbe.social.model.Comment;
import com.orbe.social.model.Post;
import com.orbe.social.model.PostLike;
import com.orbe.social.model.Rating;
import com.orbe.social.repository.CommentRepository;
import com.orbe.social.repository.PostLikeRepository;
import com.orbe.social.repository.PostRepository;
import com.orbe.social.repository.RatingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CommunityService {

	private final CatalogQueryService catalog;
	private final RatingRepository ratings;
	private final PostRepository posts;
	private final CommentRepository comments;
	private final PostLikeRepository likes;
	private final CurrentUser currentUser;

	public CommunityService(
			CatalogQueryService catalog,
			RatingRepository ratings,
			PostRepository posts,
			CommentRepository comments,
			PostLikeRepository likes,
			CurrentUser currentUser
	) {
		this.catalog = catalog;
		this.ratings = ratings;
		this.posts = posts;
		this.comments = comments;
		this.likes = likes;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public Overview overview() {
		return new Overview(
				catalog.trending(12),
				catalog.topRated(12),
				catalog.popular(12),
				recent(),
				posts.latest().stream().limit(20).map(this::postView).toList()
		);
	}

	@Transactional
	public PostView createPost(String title, String body) {
		UserAccount author = currentUser.require();
		Post post = new Post();
		post.setAuthor(author);
		post.setTitle(title.trim());
		post.setBody(body.trim());
		post.setCreatedAt(Instant.now());
		return postView(posts.save(post));
	}

	@Transactional
	public void deletePost(Long id) {
		Post post = posts.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré esa publicación."));
		UserAccount user = currentUser.require();
		if (!post.getAuthor().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes borrar tus propias publicaciones.");
		}
		likes.deleteByPost(post);
		comments.deleteByPost(post);
		posts.delete(post);
	}

	@Transactional
	public CommentView comment(Long postId, String body) {
		Post post = posts.findById(postId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré esa publicación."));
		UserAccount author = currentUser.require();
		Comment comment = new Comment();
		comment.setPost(post);
		comment.setAuthor(author);
		comment.setBody(body.trim());
		comment.setCreatedAt(Instant.now());
		comments.save(comment);
		return new CommentView(comment.getId(), author.getUsername(), author.getDisplayName(), comment.getBody(), comment.getCreatedAt(), true);
	}

	@Transactional
	public void deleteComment(Long id) {
		Comment comment = comments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré ese comentario."));
		UserAccount user = currentUser.require();
		if (!comment.getAuthor().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes borrar tus propios comentarios.");
		}
		comments.delete(comment);
	}

	@Transactional
	public PostView toggleLike(Long postId) {
		Post post = posts.findById(postId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré esa publicación."));
		UserAccount user = currentUser.require();
		PostLike existing = likes.findByUserAndPost(user, post).orElse(null);
		if (existing != null) {
			likes.delete(existing);
		} else {
			PostLike like = new PostLike();
			like.setUser(user);
			like.setPost(post);
			like.setCreatedAt(Instant.now());
			likes.save(like);
		}
		return postView(post);
	}

	private List<OpinionView> recent() {
		return ratings.recentOpinions(PageRequest.of(0, 16)).stream()
				.map(this::opinion)
				.toList();
	}

	private OpinionView opinion(Rating rating) {
		return new OpinionView(
				rating.getUser().getUsername(),
				rating.getUser().getDisplayName(),
				rating.getScore(),
				rating.getOpinion(),
				rating.getUpdatedAt()
		);
	}

	private PostView postView(Post post) {
		UserAccount user = currentUser.optional().orElse(null);
		boolean mine = user != null && post.getAuthor().getId().equals(user.getId());
		boolean liked = user != null && likes.existsByUserAndPost(user, post);
		List<CommentView> thread = comments.findByPost(post).stream()
				.map(comment -> new CommentView(
						comment.getId(),
						comment.getAuthor().getUsername(),
						comment.getAuthor().getDisplayName(),
						comment.getBody(),
						comment.getCreatedAt(),
						user != null && comment.getAuthor().getId().equals(user.getId())
				))
				.toList();
		return new PostView(
				post.getId(),
				post.getAuthor().getUsername(),
				post.getAuthor().getDisplayName(),
				post.getTitle(),
				post.getBody(),
				post.getCreatedAt(),
				likes.countByPost(post),
				liked,
				mine,
				thread
		);
	}
}
