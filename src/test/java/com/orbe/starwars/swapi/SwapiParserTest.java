package com.orbe.starwars.swapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SwapiParserTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void parsesAClassicFilmAndItsUrlId() throws Exception {
		var node = mapper.readTree("""
				{"title":"A New Hope","episode_id":4,"opening_crawl":"It is a period of civil war.","director":"George Lucas","producer":"Gary Kurtz","release_date":"1977-05-25","url":"https://swapi.py4e.com/api/films/1/"}
				""");
		FilmSnapshot film = SwapiParser.parseOne(node, 0);
		assertEquals(1, film.id());
		assertEquals(4, film.episodeId());
		assertEquals("A New Hope", film.title());
		assertEquals("George Lucas", film.director());
		assertEquals("1977-05-25", film.releaseDate());
	}

	@Test
	void parsesAListAndAnAlternateWrapper() throws Exception {
		var list = mapper.readTree("""
				{"count":1,"results":[{"title":"The Empire Strikes Back","episode_id":5,"director":"Irvin Kershner","release_date":"1980-05-17","url":"https://swapi.dev/api/films/2/"}]}
				""");
		List<FilmSnapshot> films = SwapiParser.parseList(list);
		assertEquals(1, films.size());
		assertEquals(2, films.get(0).id());

		var wrapped = mapper.readTree("""
				{"result":{"properties":{"title":"A New Hope","episode_id":4,"opening_crawl":"crawl","director":"George Lucas","release_date":"1977-05-25"},"uid":"1"}}
				""");
		FilmSnapshot film = SwapiParser.parseOne(wrapped, 0);
		assertEquals(1, film.id());
		assertEquals("A New Hope", film.title());
		assertEquals("crawl", film.openingCrawl());
		assertNull(film.url());
	}
}
