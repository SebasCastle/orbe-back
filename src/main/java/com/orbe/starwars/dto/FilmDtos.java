package com.orbe.starwars.dto;

public final class FilmDtos {

	private FilmDtos() {
	}

	public record FilmResponse(
			int id,
			int episodeId,
			String title,
			String openingCrawl,
			String director,
			String producer,
			String releaseDate,
			boolean saved
	) {
	}
}
