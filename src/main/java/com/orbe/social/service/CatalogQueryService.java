package com.orbe.social.service;

import com.orbe.auth.model.UserAccount;
import com.orbe.security.CurrentUser;
import com.orbe.social.dto.SocialDtos.Card;
import com.orbe.social.dto.SocialDtos.HomeView;
import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.ContentKind;
import com.orbe.social.model.Rating;
import com.orbe.social.repository.CatalogRepository;
import com.orbe.social.repository.FavoriteRepository;
import com.orbe.social.repository.RatingRepository;
import com.orbe.social.repository.SavedItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogQueryService {

	private final CatalogRepository catalogs;
	private final RatingRepository ratings;
	private final SavedItemRepository savedItems;
	private final FavoriteRepository favorites;
	private final CurrentUser currentUser;
	private final EngagementService engagement;

	public CatalogQueryService(
			CatalogRepository catalogs,
			RatingRepository ratings,
			SavedItemRepository savedItems,
			FavoriteRepository favorites,
			CurrentUser currentUser,
			EngagementService engagement
	) {
		this.catalogs = catalogs;
		this.ratings = ratings;
		this.savedItems = savedItems;
		this.favorites = favorites;
		this.currentUser = currentUser;
		this.engagement = engagement;
	}

	@Transactional(readOnly = true)
	public HomeView home() {
		return new HomeView(
				cards(catalogs.findByKindOrderByTitleAsc(ContentKind.MOVIE, PageRequest.of(0, 18))),
				cards(catalogs.findByKindOrderByTitleAsc(ContentKind.BOOK, PageRequest.of(0, 18))),
				cards(catalogs.findByKindOrderByTitleAsc(ContentKind.TRACK, PageRequest.of(0, 18))),
				cards(catalogs.findByKindOrderByTitleAsc(ContentKind.FILM, PageRequest.of(0, 18))),
				trending(12)
		);
	}

	@Transactional(readOnly = true)
	public List<Card> search(String kind, String query, String category) {
		ContentKind parsed = kind == null || kind.isBlank() ? null : ContentKind.parse(kind);
		String q = query == null || query.isBlank() ? null : query.trim();
		String cat = category == null || category.isBlank() ? null : category.trim();
		return cards(catalogs.search(parsed, q, cat, PageRequest.of(0, 48)));
	}

	@Transactional(readOnly = true)
	public List<Card> trending(int limit) {
		List<Object[]> rows = ratings.trending(Instant.now().minus(30, ChronoUnit.DAYS), PageRequest.of(0, limit));
		List<Card> cards = ordered(rows);
		if (!cards.isEmpty()) {
			return cards;
		}
		return cards(catalogs.findAll(PageRequest.of(0, limit)).getContent());
	}

	@Transactional(readOnly = true)
	public List<Card> topRated(int limit) {
		return ordered(ratings.topRated(PageRequest.of(0, limit)));
	}

	@Transactional(readOnly = true)
	public List<Card> popular(int limit) {
		return ordered(ratings.popular(PageRequest.of(0, limit)));
	}

	@Transactional(readOnly = true)
	public Card card(CatalogEntry entry) {
		return cards(List.of(entry)).get(0);
	}

	public List<Card> cards(List<CatalogEntry> entries) {
		UserAccount user = currentUser.optional().orElse(null);
		Map<Long, Integer> personal = new HashMap<>();
		if (user != null) {
			for (Rating rating : engagement.ratingsOf(user)) {
				personal.put(rating.getCatalog().getId(), rating.getScore());
			}
		}
		List<Card> result = new ArrayList<>();
		for (CatalogEntry entry : entries) {
			var stats = engagement.stats(entry);
			boolean saved = user != null && savedItems.existsByUserAndCatalog(user, entry);
			boolean favorite = user != null && favorites.existsByUserAndCatalog(user, entry);
			result.add(new Card(
					entry.getId(),
					entry.getKind().name(),
					entry.getExternalId(),
					entry.getTitle(),
					entry.getYearLabel(),
					entry.getCreator(),
					entry.getImageUrl(),
					entry.getSynopsis(),
					entry.getCategory(),
					entry.getRuntimeMinutes(),
					entry.getPreviewUrl(),
					entry.getSourceRating(),
					entry.getSubtype(),
					stats.average(),
					stats.votes(),
					personal.get(entry.getId()),
					saved,
					favorite
			));
		}
		return result;
	}

	private List<Card> ordered(List<Object[]> rows) {
		List<Long> ids = new ArrayList<>();
		for (Object[] row : rows) {
			if (row[0] instanceof Number number) {
				ids.add(number.longValue());
			}
		}
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<Long, CatalogEntry> byId = new HashMap<>();
		for (CatalogEntry entry : catalogs.findByIdIn(ids)) {
			byId.put(entry.getId(), entry);
		}
		List<CatalogEntry> ordered = new ArrayList<>();
		for (Long id : ids) {
			CatalogEntry entry = byId.get(id);
			if (entry != null) {
				ordered.add(entry);
			}
		}
		return cards(ordered);
	}
}
