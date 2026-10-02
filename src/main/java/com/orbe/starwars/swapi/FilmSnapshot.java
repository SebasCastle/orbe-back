package com.orbe.starwars.swapi;

public record FilmSnapshot(
		int id,
		int episodeId,
		String title,
		String openingCrawl,
		String director,
		String producer,
		String releaseDate,
		String url
) {
}
