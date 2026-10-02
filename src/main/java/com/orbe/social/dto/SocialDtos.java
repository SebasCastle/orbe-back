package com.orbe.social.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SocialDtos {

	private SocialDtos() {
	}

	public record Card(
			Long id,
			String kind,
			String externalId,
			String title,
			String year,
			String creator,
			String imageUrl,
			String synopsis,
			String category,
			Integer runtimeMinutes,
			String previewUrl,
			String sourceRating,
			String subtype,
			double average,
			int votes,
			Integer personalScore,
			boolean saved,
			boolean favorite
	) {
	}

	public record OpinionView(String username, String displayName, int score, String opinion, Instant updatedAt) {
	}

	public record RateResponse(
			Long catalogId,
			int personalScore,
			double average,
			int votes,
			int classification,
			String recommendation
	) {
	}

	public record DraftRequest(
			@NotBlank(message = "Falta el tipo de contenido.") String kind,
			@NotBlank(message = "Falta el identificador.") @Size(max = 80) String externalId,
			@NotBlank(message = "Falta el título.") @Size(max = 300) String title,
			@Size(max = 40) String year,
			@Size(max = 300) String creator,
			@Size(max = 1000) String imageUrl,
			@Size(max = 4000) String synopsis,
			@Size(max = 80) String category,
			Integer runtimeMinutes,
			@Size(max = 1000) String previewUrl,
			@Size(max = 20) String sourceRating,
			@Size(max = 40) String subtype
	) {
	}

	public record RateRequest(
			@NotBlank(message = "Falta el tipo de contenido.") String kind,
			@NotBlank(message = "Falta el identificador.") @Size(max = 80) String externalId,
			@Min(value = 1, message = "La nota mínima es 1.")
			@Max(value = 5, message = "La nota máxima es 5.")
			int score,
			@Size(max = 500, message = "La opinión admite hasta 500 caracteres.") String opinion,
			@NotBlank(message = "Falta el título.") @Size(max = 300) String title,
			@Size(max = 40) String year,
			@Size(max = 300) String creator,
			@Size(max = 1000) String imageUrl,
			@Size(max = 4000) String synopsis,
			@Size(max = 80) String category,
			Integer runtimeMinutes,
			@Size(max = 1000) String previewUrl,
			@Size(max = 20) String sourceRating,
			@Size(max = 40) String subtype
	) {
	}

	public record PlanBody(boolean includedInPlan) {
	}

	public record ShelfRequest(
			@NotBlank(message = "Escribe un nombre para la lista.")
			@Size(max = 40, message = "El nombre admite hasta 40 caracteres.")
			String name
	) {
	}

	public record ShelfView(Long id, String name, List<Card> items) {
	}

	public record SavedView(Long id, boolean includedInPlan, Card card) {
	}

	public record HistoryView(Card card, Instant viewedAt) {
	}

	public record RatingMine(Card card, int score, String opinion, Instant updatedAt) {
	}

	public record ProfileView(
			Long id,
			String username,
			String displayName,
			String email,
			List<RatingMine> ratings,
			List<Card> favorites,
			List<ShelfView> shelves,
			List<HistoryView> history,
			List<SavedView> saved
	) {
	}

	public record HomeView(List<Card> movies, List<Card> books, List<Card> tracks, List<Card> films, List<Card> trending) {
	}

	public record PostRequest(
			@NotBlank(message = "Escribe un título.")
			@Size(max = 120, message = "El título admite hasta 120 caracteres.")
			String title,
			@NotBlank(message = "Escribe el mensaje.")
			@Size(max = 2000, message = "El mensaje admite hasta 2000 caracteres.")
			String body
	) {
	}

	public record CommentRequest(
			@NotBlank(message = "Escribe un comentario.")
			@Size(max = 500, message = "El comentario admite hasta 500 caracteres.")
			String body
	) {
	}

	public record CommentView(Long id, String username, String displayName, String body, Instant createdAt, boolean mine) {
	}

	public record PostView(
			Long id,
			String username,
			String displayName,
			String title,
			String body,
			Instant createdAt,
			long likes,
			boolean liked,
			boolean mine,
			List<CommentView> comments
	) {
	}

	public record Overview(
			List<Card> trending,
			List<Card> topRated,
			List<Card> popular,
			List<OpinionView> recent,
			List<PostView> posts
	) {
	}
}
