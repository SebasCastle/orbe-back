package com.orbe.starwars.service;

import com.orbe.cinema.domain.YearParser;
import com.orbe.common.ApiException;
import com.orbe.common.Texts;
import com.orbe.security.CurrentUser;
import com.orbe.social.model.CatalogDraft;
import com.orbe.social.model.ContentKind;
import com.orbe.social.service.EngagementService;
import com.orbe.starwars.dto.FilmDtos.FilmResponse;
import com.orbe.starwars.swapi.FilmSnapshot;
import com.orbe.starwars.swapi.SwapiClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class StarWarsService {

	private final SwapiClient swapi;
	private final EngagementService engagement;
	private final CurrentUser currentUser;

	public StarWarsService(SwapiClient swapi, EngagementService engagement, CurrentUser currentUser) {
		this.swapi = swapi;
		this.engagement = engagement;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public List<FilmResponse> list() {
		Set<String> saved = currentUser.optional()
				.map(user -> engagement.savedExternalIds(user, ContentKind.FILM))
				.orElse(Set.of());
		return swapi.list().stream()
				.sorted(Comparator.comparingInt(FilmSnapshot::episodeId).thenComparingInt(FilmSnapshot::id))
				.map(film -> toResponse(film, saved.contains(Integer.toString(film.id()))))
				.toList();
	}

	@Transactional(readOnly = true)
	public FilmResponse get(int id) {
		FilmSnapshot film = requireRemote(id);
		boolean saved = currentUser.optional()
				.map(user -> engagement.savedExternalIds(user, ContentKind.FILM).contains(Integer.toString(id)))
				.orElse(false);
		return toResponse(film, saved);
	}

	@Transactional
	public FilmResponse save(int id) {
		FilmSnapshot film = requireRemote(id);
		engagement.save(currentUser.require(), ContentKind.FILM, Integer.toString(id), draft(film));
		return toResponse(film, true);
	}

	@Transactional
	public void delete(int id) {
		engagement.unsaveExternal(currentUser.require(), ContentKind.FILM, Integer.toString(id));
	}

	private FilmSnapshot requireRemote(int id) {
		FilmSnapshot film = swapi.get(id);
		if (film == null || film.id() <= 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "No encontré esa película de Star Wars.");
		}
		return film;
	}

	private CatalogDraft draft(FilmSnapshot film) {
		String crawl = film.openingCrawl() == null ? null : film.openingCrawl().replace("\r\n", " ").replace('\n', ' ');
		return new CatalogDraft(
				film.title(),
				film.releaseDate(),
				YearParser.yearOrZero(film.releaseDate()),
				Texts.clean(film.director()),
				null,
				Texts.clean(crawl),
				"Star Wars",
				null,
				null,
				null,
				"film"
		);
	}

	private FilmResponse toResponse(FilmSnapshot film, boolean saved) {
		return new FilmResponse(
				film.id(),
				film.episodeId(),
				film.title(),
				film.openingCrawl(),
				film.director(),
				film.producer(),
				film.releaseDate(),
				saved
		);
	}
}
