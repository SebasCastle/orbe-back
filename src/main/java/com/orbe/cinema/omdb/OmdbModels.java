package com.orbe.cinema.omdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class OmdbModels {

	private OmdbModels() {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OmdbTitle(
			@JsonProperty("Title") String title,
			@JsonProperty("Year") String year,
			@JsonProperty("Runtime") String runtime,
			@JsonProperty("Genre") String genre,
			@JsonProperty("Director") String director,
			@JsonProperty("Writer") String writer,
			@JsonProperty("Actors") String actors,
			@JsonProperty("Plot") String plot,
			@JsonProperty("Language") String language,
			@JsonProperty("Poster") String poster,
			@JsonProperty("imdbRating") String imdbRating,
			@JsonProperty("imdbID") String imdbId,
			@JsonProperty("Type") String type,
			@JsonProperty("Rated") String rated,
			@JsonProperty("Released") String released,
			@JsonProperty("Response") String response,
			@JsonProperty("Error") String error
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OmdbSearchItem(
			@JsonProperty("Title") String title,
			@JsonProperty("Year") String year,
			@JsonProperty("imdbID") String imdbId,
			@JsonProperty("Type") String type,
			@JsonProperty("Poster") String poster
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OmdbSearch(
			@JsonProperty("Search") List<OmdbSearchItem> search,
			@JsonProperty("totalResults") String totalResults,
			@JsonProperty("Response") String response,
			@JsonProperty("Error") String error
	) {
	}
}
