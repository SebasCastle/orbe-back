package com.orbe.cinema.service;

import com.orbe.cinema.domain.DurationParser;
import com.orbe.cinema.domain.WatchTimeCalculator;
import com.orbe.cinema.domain.YearParser;
import com.orbe.cinema.dto.CinemaDtos.LibraryItem;
import com.orbe.cinema.dto.CinemaDtos.LibrarySummary;
import com.orbe.cinema.dto.CinemaDtos.SearchItem;
import com.orbe.cinema.dto.CinemaDtos.SearchResponse;
import com.orbe.cinema.dto.CinemaDtos.TitleDetail;
import com.orbe.cinema.omdb.OmdbClient;
import com.orbe.cinema.omdb.OmdbModels.OmdbSearch;
import com.orbe.cinema.omdb.OmdbModels.OmdbTitle;
import com.orbe.common.ApiException;
import com.orbe.common.Texts;
import com.orbe.security.CurrentUser;
import com.orbe.social.model.CatalogDraft;
import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.ContentKind;
import com.orbe.social.model.SavedItem;
import com.orbe.social.service.EngagementService;
import com.orbe.social.service.EngagementService.Stats;
import com.orbe.social.service.EngagementService.Viewer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CinemaService {

	private final OmdbClient omdbClient;
	private final CinemaMapper mapper;
	private final EngagementService engagement;
	private final CurrentUser currentUser;

	public CinemaService(OmdbClient omdbClient, CinemaMapper mapper, EngagementService engagement, CurrentUser currentUser) {
		this.omdbClient = omdbClient;
		this.mapper = mapper;
		this.engagement = engagement;
		this.currentUser = currentUser;
	}

	public SearchResponse search(String query, int page) {
		OmdbSearch result = omdbClient.search(query.trim(), page);
		List<SearchItem> items = result.search() == null
				? List.of()
				: result.search().stream().map(mapper::searchItem).toList();
		int total = 0;
		if (result.totalResults() != null) {
			try {
				total = Integer.parseInt(result.totalResults());
			} catch (NumberFormatException ignored) {
				total = items.size();
			}
		}
		return new SearchResponse(items, total, page);
	}

	@Transactional(readOnly = true)
	public TitleDetail details(String imdbId) {
		try {
			OmdbTitle remote = omdbClient.details(imdbId);
			return apply(mapper.detail(remote, null));
		} catch (ApiException exception) {
			CatalogEntry saved = engagement.find(ContentKind.MOVIE, imdbId).orElse(null);
			if (saved == null) {
				throw exception;
			}
			Viewer viewer = engagement.view(ContentKind.MOVIE, imdbId);
			TitleDetail base = new TitleDetail(
					saved.getExternalId(),
					saved.getTitle(),
					saved.getYearLabel(),
					saved.getReleaseYear(),
					saved.getRuntimeMinutes(),
					null,
					null,
					saved.getCategory(),
					saved.getCreator(),
					null,
					null,
					saved.getSynopsis(),
					null,
					saved.getImageUrl(),
					saved.getSourceRating(),
					saved.getSubtype() == null ? "movie" : saved.getSubtype(),
					false,
					null,
					0,
					0,
					0,
					null,
					false,
					null
			);
			return mapper.socialize(base, viewer.saved(), viewer.libraryId(), viewer.votes(), viewer.average(), viewer.personalScore(), viewer.includedInPlan());
		}
	}

	@Transactional
	public SaveOutcome save(String imdbId) {
		OmdbTitle remote = omdbClient.details(imdbId);
		var result = engagement.save(currentUser.require(), ContentKind.MOVIE, imdbId, draft(remote));
		return new SaveOutcome(toItem(result.item()), result.created());
	}

	@Transactional(readOnly = true)
	public LibrarySummary library(String sort) {
		List<SavedItem> items = engagement.library(currentUser.require(), ContentKind.MOVIE);
		List<LibraryItem> mapped = items.stream().map(this::toItem).sorted(comparator(sort)).toList();
		WatchTimeCalculator calculator = new WatchTimeCalculator();
		for (LibraryItem item : mapped) {
			if (item.runtimeMinutes() != null) {
				calculator.include(item.runtimeMinutes());
			}
		}
		return new LibrarySummary(mapped.size(), calculator.total(), mapped);
	}

	@Transactional
	public LibraryItem rate(Long id, int score, String opinion) {
		var rating = engagement.rateSaved(currentUser.require(), id, score, opinion);
		SavedItem item = engagement.requireOwned(currentUser.require(), id);
		if (!item.getCatalog().getId().equals(rating.getCatalog().getId())) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Ese título no está en tu biblioteca.");
		}
		return toItem(item);
	}

	@Transactional
	public LibraryItem updatePlan(Long id, boolean included) {
		return toItem(engagement.updatePlan(currentUser.require(), id, included));
	}

	@Transactional
	public void delete(Long id) {
		engagement.unsave(currentUser.require(), id);
	}

	private TitleDetail apply(TitleDetail detail) {
		Viewer viewer = engagement.view(ContentKind.MOVIE, detail.imdbId());
		return mapper.socialize(detail, viewer.saved(), viewer.libraryId(), viewer.votes(), viewer.average(), viewer.personalScore(), viewer.includedInPlan());
	}

	private LibraryItem toItem(SavedItem item) {
		CatalogEntry catalog = item.getCatalog();
		Stats stats = engagement.stats(catalog);
		Integer personal = engagement.view(catalog.getKind(), catalog.getExternalId()).personalScore();
		return new LibraryItem(
				item.getId(),
				catalog.getExternalId(),
				catalog.getTitle(),
				catalog.getYearLabel(),
				catalog.getReleaseYear(),
				catalog.getRuntimeMinutes(),
				catalog.getSubtype() == null ? "movie" : catalog.getSubtype(),
				catalog.getImageUrl(),
				catalog.getSynopsis(),
				catalog.getCreator(),
				catalog.getCategory(),
				catalog.getSourceRating(),
				item.isIncludedInPlan(),
				stats.votes(),
				stats.average(),
				engagement.classification(stats.average(), stats.votes()),
				engagement.recommendation(stats.average(), stats.votes()),
				personal
		);
	}

	private CatalogDraft draft(OmdbTitle remote) {
		String genre = Texts.clean(remote.genre());
		if (genre != null && genre.contains(",")) {
			genre = genre.substring(0, genre.indexOf(',')).trim();
		}
		return new CatalogDraft(
				Texts.clean(remote.title()),
				Texts.clean(remote.year()),
				YearParser.yearOrZero(remote.year()),
				Texts.clean(remote.director()),
				Texts.clean(remote.poster()),
				Texts.clean(remote.plot()),
				genre,
				DurationParser.minutesOrNull(remote.runtime()),
				null,
				Texts.clean(remote.imdbRating()),
				remote.type() == null ? "movie" : remote.type()
		);
	}

	private Comparator<LibraryItem> comparator(String sort) {
		return switch (sort) {
			case "name" -> Comparator.comparing(item -> item.title() == null ? "" : item.title().toLowerCase(Locale.ROOT));
			case "year" -> Comparator.comparingInt(LibraryItem::releaseYear);
			case "rating" -> Comparator.comparingDouble(LibraryItem::averageRating).reversed()
					.thenComparing(item -> item.title() == null ? "" : item.title(), String.CASE_INSENSITIVE_ORDER);
			default -> throw new ApiException(HttpStatus.BAD_REQUEST, "El orden debe ser name, year o rating.");
		};
	}

	public record SaveOutcome(LibraryItem item, boolean created) {
	}
}
