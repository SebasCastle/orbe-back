package com.orbe.cinema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public final class CinemaDtos {

	private CinemaDtos() {
	}

	public record SearchItem(String imdbId, String title, String year, String type, String poster) {
	}

	public record SearchResponse(List<SearchItem> results, int total, int page) {
	}

	public record TitleDetail(
			String imdbId,
			String title,
			String year,
			int releaseYear,
			Integer runtimeMinutes,
			String rated,
			String released,
			String genre,
			String director,
			String writer,
			String actors,
			String plot,
			String language,
			String poster,
			String imdbRating,
			String type,
			boolean saved,
			Long libraryId,
			int totalRatings,
			double averageRating,
			int classification,
			String recommendation,
			boolean includedInPlan,
			Integer personalScore
	) {
	}

	public record LibraryItem(
			Long id,
			String imdbId,
			String title,
			String year,
			int releaseYear,
			Integer runtimeMinutes,
			String type,
			String poster,
			String plot,
			String director,
			String genre,
			String imdbRating,
			boolean includedInPlan,
			int totalRatings,
			double averageRating,
			int classification,
			String recommendation,
			Integer personalScore
	) {
	}

	public record LibrarySummary(int count, int totalMinutes, List<LibraryItem> items) {
	}

	public record SaveTitleRequest(
			@NotBlank(message = "Falta el identificador de IMDb.")
			@Pattern(regexp = "tt\\d+", message = "El identificador de IMDb no es válido.")
			String imdbId
	) {
	}

	public record RatingRequest(
			@Min(value = 1, message = "La nota mínima es 1.")
			@Max(value = 5, message = "La nota máxima es 5.")
			int score,
			@jakarta.validation.constraints.Size(max = 500, message = "La opinión admite hasta 500 caracteres.")
			String opinion
	) {
	}

	public record PlanRequest(boolean includedInPlan) {
	}
}
