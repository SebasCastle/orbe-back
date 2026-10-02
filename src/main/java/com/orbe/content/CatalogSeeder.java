package com.orbe.content;

import com.orbe.cinema.domain.YearParser;
import com.orbe.cinema.omdb.OmdbClient;
import com.orbe.cinema.omdb.OmdbModels.OmdbSearchItem;
import com.orbe.common.Texts;
import com.orbe.social.model.CatalogDraft;
import com.orbe.social.model.ContentKind;
import com.orbe.social.service.EngagementService;
import com.orbe.starwars.swapi.FilmSnapshot;
import com.orbe.starwars.swapi.SwapiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogSeeder {

	private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);
	private static final String[] MOVIE_QUERIES = {
			"inception", "spirited away", "parasite", "the godfather", "whiplash",
			"spider-man", "dune", "amelie", "mad max", "portrait"
	};
	private static final String[] BOOK_SUBJECTS = {
			"science_fiction", "fantasy", "mystery", "poetry", "biography", "history"
	};
	private static final String[] MUSIC_TERMS = {
			"jazz", "bossa nova", "rock", "classical", "electronic", "salsa"
	};

	private final boolean enabled;
	private final OmdbClient omdb;
	private final SwapiClient swapi;
	private final MusicCatalogService music;
	private final com.orbe.common.ExternalHttp http;
	private final EngagementService engagement;

	public CatalogSeeder(
			@Value("${orbe.catalog.seed:true}") boolean enabled,
			OmdbClient omdb,
			SwapiClient swapi,
			MusicCatalogService music,
			com.orbe.common.ExternalHttp http,
			EngagementService engagement
	) {
		this.enabled = enabled;
		this.omdb = omdb;
		this.swapi = swapi;
		this.music = music;
		this.http = http;
		this.engagement = engagement;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		if (!enabled) {
			return;
		}
		Thread thread = new Thread(this::seed, "orbe-catalog");
		thread.setDaemon(true);
		thread.start();
	}

	private void seed() {
		int movies = seedMovies();
		int books = seedBooks();
		int tracks = seedTracks();
		int films = seedFilms();
		log.info("Catálogo listo: {} películas, {} libros, {} canciones, {} filmes de Star Wars.", movies, books, tracks, films);
	}

	private int seedMovies() {
		int count = 0;
		for (String query : MOVIE_QUERIES) {
			try {
				var result = omdb.search(query, 1);
				if (result.search() == null) {
					continue;
				}
				for (OmdbSearchItem item : result.search()) {
					if (item == null || Texts.blank(item.imdbId()) || Texts.blank(item.title())) {
						continue;
					}
					int year = YearParser.yearOrZero(item.year());
					engagement.upsert(ContentKind.MOVIE, item.imdbId(), new CatalogDraft(
							Texts.clean(item.title()),
							Texts.clean(item.year()),
							year,
							null,
							Texts.clean(item.poster()),
							null,
							null,
							null,
							null,
							null,
							Texts.clean(item.type())
					));
					count++;
				}
				Thread.sleep(200);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				return count;
			} catch (RuntimeException exception) {
				log.info("No se pudo ampliar el cine con '{}': {}", query, exception.getMessage());
			}
		}
		return count;
	}

	private int seedBooks() {
		int count = 0;
		for (String subject : BOOK_SUBJECTS) {
			try {
				String url = "https://openlibrary.org/search.json?limit=16&fields=key,title,author_name,first_publish_year,cover_i,subject,first_sentence&subject="
						+ subject;
				var docs = http.getTree(url).path("docs");
				if (!docs.isArray()) {
					continue;
				}
				for (var doc : docs) {
					String id = BookService.workId(doc.path("key").asText(""));
					CatalogDraft draft = BookService.fromSearch(doc);
					if (id == null || draft == null) {
						continue;
					}
					CatalogDraft tagged = new CatalogDraft(
							draft.title(), draft.yearLabel(), draft.releaseYear(), draft.creator(), draft.imageUrl(),
							draft.synopsis(), subject.replace('_', ' '), draft.runtimeMinutes(), null, null, "book"
					);
					engagement.upsert(ContentKind.BOOK, id, tagged);
					count++;
				}
			} catch (RuntimeException exception) {
				log.info("No se pudo ampliar libros de {}: {}", subject, exception.getMessage());
			}
		}
		return count;
	}

	private int seedTracks() {
		int count = 0;
		for (String term : MUSIC_TERMS) {
			try {
				var results = music.fetch(term);
				count += music.store(results).size();
			} catch (RuntimeException exception) {
				log.info("No se pudo ampliar música de {}: {}", term, exception.getMessage());
			}
		}
		return count;
	}

	private int seedFilms() {
		int count = 0;
		try {
			for (FilmSnapshot film : swapi.list()) {
				if (film == null || film.id() <= 0 || Texts.blank(film.title())) {
					continue;
				}
				int year = YearParser.yearOrZero(film.releaseDate());
				String crawl = film.openingCrawl() == null ? null : film.openingCrawl().replace("\r\n", " ").replace('\n', ' ');
				engagement.upsert(ContentKind.FILM, Integer.toString(film.id()), new CatalogDraft(
						film.title(),
						film.releaseDate(),
						year,
						Texts.clean(film.director()),
						null,
						Texts.clean(crawl),
						"Star Wars",
						null,
						null,
						null,
						"film"
				));
				count++;
			}
		} catch (RuntimeException exception) {
			log.info("No se pudo ampliar Star Wars: {}", exception.getMessage());
		}
		return count;
	}
}
