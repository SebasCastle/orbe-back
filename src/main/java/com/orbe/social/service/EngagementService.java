package com.orbe.social.service;

import com.orbe.auth.model.UserAccount;
import com.orbe.cinema.domain.RecommendationFilter;
import com.orbe.common.ApiException;
import com.orbe.common.Texts;
import com.orbe.security.CurrentUser;
import com.orbe.social.model.CatalogDraft;
import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.ContentKind;
import com.orbe.social.model.Favorite;
import com.orbe.social.model.HistoryEntry;
import com.orbe.social.model.Rating;
import com.orbe.social.model.SavedItem;
import com.orbe.social.model.Shelf;
import com.orbe.social.model.ShelfItem;
import com.orbe.social.repository.CatalogRepository;
import com.orbe.social.repository.FavoriteRepository;
import com.orbe.social.repository.HistoryRepository;
import com.orbe.social.repository.RatingRepository;
import com.orbe.social.repository.RatingRepository.RatingSummary;
import com.orbe.social.repository.SavedItemRepository;
import com.orbe.social.repository.ShelfItemRepository;
import com.orbe.social.repository.ShelfRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EngagementService {

	private final CatalogRepository catalogs;
	private final RatingRepository ratings;
	private final SavedItemRepository savedItems;
	private final FavoriteRepository favorites;
	private final HistoryRepository history;
	private final ShelfRepository shelves;
	private final ShelfItemRepository shelfItems;
	private final CurrentUser currentUser;
	private final RecommendationFilter recommendations = new RecommendationFilter();

	public EngagementService(
			CatalogRepository catalogs,
			RatingRepository ratings,
			SavedItemRepository savedItems,
			FavoriteRepository favorites,
			HistoryRepository history,
			ShelfRepository shelves,
			ShelfItemRepository shelfItems,
			CurrentUser currentUser
	) {
		this.catalogs = catalogs;
		this.ratings = ratings;
		this.savedItems = savedItems;
		this.favorites = favorites;
		this.history = history;
		this.shelves = shelves;
		this.shelfItems = shelfItems;
		this.currentUser = currentUser;
	}

	public record Stats(double average, int votes) {
	}

	public record Viewer(boolean saved, Long libraryId, boolean includedInPlan, int votes, double average, Integer personalScore, boolean favorite) {
		public static Viewer empty() {
			return new Viewer(false, null, false, 0, 0, null, false);
		}
	}

	public record SaveResult(SavedItem item, boolean created) {
	}

	@Transactional
	public CatalogEntry upsert(ContentKind kind, String externalId, CatalogDraft draft) {
		if (Texts.blank(externalId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Falta el identificador del contenido.");
		}
		CatalogEntry entry = catalogs.findByKindAndExternalId(kind, externalId.trim()).orElseGet(CatalogEntry::new);
		entry.setKind(kind);
		entry.setExternalId(externalId.trim());
		assign(entry, draft);
		if (Texts.blank(entry.getTitle())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Falta el título.");
		}
		return catalogs.save(entry);
	}

	@Transactional(readOnly = true)
	public Optional<CatalogEntry> find(ContentKind kind, String externalId) {
		return catalogs.findByKindAndExternalId(kind, externalId);
	}

	@Transactional(readOnly = true)
	public Viewer view(ContentKind kind, String externalId) {
		CatalogEntry entry = catalogs.findByKindAndExternalId(kind, externalId).orElse(null);
		if (entry == null) {
			return Viewer.empty();
		}
		Stats stats = stats(entry);
		UserAccount user = currentUser.optional().orElse(null);
		if (user == null) {
			return new Viewer(false, null, false, stats.votes(), stats.average(), null, false);
		}
		SavedItem saved = savedItems.findByUserAndCatalog(user, entry).orElse(null);
		Integer personal = ratings.findByUserAndCatalog(user, entry).map(Rating::getScore).orElse(null);
		return new Viewer(
				saved != null,
				saved == null ? null : saved.getId(),
				saved != null && saved.isIncludedInPlan(),
				stats.votes(),
				stats.average(),
				personal,
				favorites.existsByUserAndCatalog(user, entry)
		);
	}

	@Transactional(readOnly = true)
	public Stats stats(CatalogEntry entry) {
		RatingSummary summary = ratings.summarize(entry.getId());
		if (summary == null || summary.getVotes() == 0 || summary.getAverage() == null) {
			return new Stats(0, 0);
		}
		return new Stats(summary.getAverage(), (int) summary.getVotes());
	}

	@Transactional(readOnly = true)
	public Set<String> savedExternalIds(UserAccount user, ContentKind kind) {
		return savedItems.findByUserAndCatalogKind(user, kind).stream()
				.map(item -> item.getCatalog().getExternalId())
				.collect(java.util.stream.Collectors.toSet());
	}

	@Transactional
	public SaveResult save(UserAccount user, ContentKind kind, String externalId, CatalogDraft draft) {
		CatalogEntry entry = upsert(kind, externalId, draft);
		SavedItem existing = savedItems.findByUserAndCatalog(user, entry).orElse(null);
		if (existing != null) {
			return new SaveResult(existing, false);
		}
		SavedItem item = new SavedItem();
		item.setUser(user);
		item.setCatalog(entry);
		item.setSavedAt(Instant.now());
		return new SaveResult(savedItems.save(item), true);
	}

	@Transactional
	public SavedItem requireOwned(UserAccount user, Long id) {
		return savedItems.findByIdAndUser(id, user)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ese título no está en tu biblioteca."));
	}

	@Transactional
	public void unsave(UserAccount user, Long id) {
		SavedItem item = requireOwned(user, id);
		savedItems.delete(item);
	}

	@Transactional
	public void unsaveExternal(UserAccount user, ContentKind kind, String externalId) {
		CatalogEntry entry = catalogs.findByKindAndExternalId(kind, externalId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ese título no está en tu biblioteca."));
		SavedItem item = savedItems.findByUserAndCatalog(user, entry)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ese título no está en tu biblioteca."));
		savedItems.delete(item);
	}

	@Transactional
	public SavedItem updatePlan(UserAccount user, Long id, boolean included) {
		SavedItem item = requireOwned(user, id);
		item.setIncludedInPlan(included);
		return savedItems.save(item);
	}

	@Transactional
	public Rating rateSaved(UserAccount user, Long savedId, int score, String opinion) {
		SavedItem item = requireOwned(user, savedId);
		return applyRating(user, item.getCatalog(), score, opinion);
	}

	@Transactional
	public Rating rate(UserAccount user, ContentKind kind, String externalId, int score, String opinion, CatalogDraft draft) {
		CatalogEntry entry = upsert(kind, externalId, draft);
		return applyRating(user, entry, score, opinion);
	}

	@Transactional(readOnly = true)
	public List<Rating> opinions(ContentKind kind, String externalId) {
		return catalogs.findByKindAndExternalId(kind, externalId)
				.map(ratings::findByCatalogOrderByUpdatedAtDesc)
				.orElse(List.of());
	}

	@Transactional(readOnly = true)
	public List<SavedItem> library(UserAccount user, ContentKind kind) {
		if (kind == null) {
			return savedItems.findByUser(user);
		}
		return savedItems.findByUserAndCatalogKind(user, kind);
	}

	@Transactional
	public boolean toggleFavorite(UserAccount user, ContentKind kind, String externalId, CatalogDraft draft) {
		CatalogEntry entry = upsert(kind, externalId, draft);
		Favorite existing = favorites.findByUserAndCatalog(user, entry).orElse(null);
		if (existing != null) {
			favorites.delete(existing);
			return false;
		}
		Favorite favorite = new Favorite();
		favorite.setUser(user);
		favorite.setCatalog(entry);
		favorite.setCreatedAt(Instant.now());
		favorites.save(favorite);
		return true;
	}

	@Transactional
	public void remember(UserAccount user, ContentKind kind, String externalId, CatalogDraft draft) {
		CatalogEntry entry = upsert(kind, externalId, draft);
		HistoryEntry row = history.findByUserAndCatalog(user, entry).orElseGet(HistoryEntry::new);
		row.setUser(user);
		row.setCatalog(entry);
		row.setViewedAt(Instant.now());
		history.save(row);
	}

	@Transactional
	public Shelf createShelf(UserAccount user, String name) {
		String clean = name == null ? "" : name.trim();
		if (clean.isBlank() || clean.length() > 40) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre de la lista debe tener entre 1 y 40 caracteres.");
		}
		if (shelves.existsByUserAndNameIgnoreCase(user, clean)) {
			throw new ApiException(HttpStatus.CONFLICT, "Ya tienes una lista con ese nombre.");
		}
		Shelf shelf = new Shelf();
		shelf.setUser(user);
		shelf.setName(clean);
		shelf.setCreatedAt(Instant.now());
		return shelves.save(shelf);
	}

	@Transactional
	public void deleteShelf(UserAccount user, Long id) {
		Shelf shelf = shelves.findByIdAndUser(id, user)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré esa lista."));
		shelfItems.deleteByShelf(shelf);
		shelves.delete(shelf);
	}

	@Transactional
	public ShelfItem addToShelf(UserAccount user, Long shelfId, ContentKind kind, String externalId, CatalogDraft draft) {
		Shelf shelf = shelves.findByIdAndUser(shelfId, user)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré esa lista."));
		CatalogEntry entry = upsert(kind, externalId, draft);
		ShelfItem existing = shelfItems.findByShelfAndCatalog(shelf, entry).orElse(null);
		if (existing != null) {
			return existing;
		}
		ShelfItem item = new ShelfItem();
		item.setShelf(shelf);
		item.setCatalog(entry);
		item.setAddedAt(Instant.now());
		return shelfItems.save(item);
	}

	@Transactional
	public void removeFromShelf(UserAccount user, Long shelfId, Long catalogId) {
		Shelf shelf = shelves.findByIdAndUser(shelfId, user)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré esa lista."));
		CatalogEntry entry = catalogs.findById(catalogId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ese título no está en la lista."));
		ShelfItem item = shelfItems.findByShelfAndCatalog(shelf, entry)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ese título no está en la lista."));
		shelfItems.delete(item);
	}

	@Transactional(readOnly = true)
	public List<Shelf> shelves(UserAccount user) {
		return shelves.findByUserOrderByCreatedAtAsc(user);
	}

	@Transactional(readOnly = true)
	public List<ShelfItem> shelfItems(Shelf shelf) {
		return shelfItems.findByShelfOrderByAddedAtAsc(shelf);
	}

	@Transactional(readOnly = true)
	public List<Favorite> favorites(UserAccount user) {
		return favorites.findByUserOrderByCreatedAtDesc(user);
	}

	@Transactional(readOnly = true)
	public List<Rating> ratingsOf(UserAccount user) {
		return ratings.findByUserOrderByUpdatedAtDesc(user);
	}

	@Transactional(readOnly = true)
	public List<HistoryEntry> historyOf(UserAccount user) {
		return history.findTop30ByUserOrderByViewedAtDesc(user);
	}

	public int classification(double average, int votes) {
		if (votes <= 0) {
			return 0;
		}
		return recommendations.classification(average);
	}

	public String recommendation(double average, int votes) {
		if (votes <= 0) {
			return null;
		}
		return recommendations.recommend(average);
	}

	private Rating applyRating(UserAccount user, CatalogEntry entry, int score, String opinion) {
		if (score < 1 || score > 5) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La nota debe estar entre 1 y 5.");
		}
		String cleanOpinion = Texts.clean(opinion);
		if (cleanOpinion != null && cleanOpinion.length() > 500) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La opinión admite hasta 500 caracteres.");
		}
		Rating rating = ratings.findByUserAndCatalog(user, entry).orElseGet(Rating::new);
		rating.setUser(user);
		rating.setCatalog(entry);
		rating.setScore(score);
		rating.setOpinion(cleanOpinion);
		rating.setUpdatedAt(Instant.now());
		return ratings.save(rating);
	}

	private void assign(CatalogEntry entry, CatalogDraft draft) {
		if (draft == null) {
			return;
		}
		if (!Texts.blank(draft.title())) {
			entry.setTitle(limit(draft.title(), 300));
		}
		if (!Texts.blank(draft.yearLabel())) {
			entry.setYearLabel(limit(draft.yearLabel(), 40));
		}
		if (draft.releaseYear() > 0) {
			entry.setReleaseYear(draft.releaseYear());
		}
		if (!Texts.blank(draft.creator())) {
			entry.setCreator(limit(draft.creator(), 300));
		}
		if (!Texts.blank(draft.imageUrl())) {
			entry.setImageUrl(limit(draft.imageUrl(), 1000));
		}
		if (!Texts.blank(draft.synopsis())) {
			entry.setSynopsis(limit(draft.synopsis(), 4000));
		}
		if (!Texts.blank(draft.category())) {
			entry.setCategory(limit(draft.category(), 80));
		}
		if (draft.runtimeMinutes() != null) {
			entry.setRuntimeMinutes(draft.runtimeMinutes());
		}
		if (!Texts.blank(draft.previewUrl())) {
			entry.setPreviewUrl(limit(draft.previewUrl(), 1000));
		}
		if (!Texts.blank(draft.sourceRating())) {
			entry.setSourceRating(limit(draft.sourceRating(), 20));
		}
		if (!Texts.blank(draft.subtype())) {
			entry.setSubtype(limit(draft.subtype(), 40));
		}
	}

	private String limit(String value, int max) {
		String trimmed = value.trim();
		return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
	}
}
