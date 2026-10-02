package com.orbe.social.controller;

import com.orbe.auth.model.UserAccount;
import com.orbe.cinema.domain.YearParser;
import com.orbe.security.CurrentUser;
import com.orbe.social.dto.SocialDtos.Card;
import com.orbe.social.dto.SocialDtos.DraftRequest;
import com.orbe.social.dto.SocialDtos.HistoryView;
import com.orbe.social.dto.SocialDtos.OpinionView;
import com.orbe.social.dto.SocialDtos.PlanBody;
import com.orbe.social.dto.SocialDtos.ProfileView;
import com.orbe.social.dto.SocialDtos.RateRequest;
import com.orbe.social.dto.SocialDtos.RateResponse;
import com.orbe.social.dto.SocialDtos.RatingMine;
import com.orbe.social.dto.SocialDtos.SavedView;
import com.orbe.social.dto.SocialDtos.ShelfRequest;
import com.orbe.social.dto.SocialDtos.ShelfView;
import com.orbe.social.model.CatalogDraft;
import com.orbe.social.model.ContentKind;
import com.orbe.social.model.Rating;
import com.orbe.social.model.SavedItem;
import com.orbe.social.model.Shelf;
import com.orbe.social.service.CatalogQueryService;
import com.orbe.social.service.EngagementService;
import com.orbe.social.service.EngagementService.Stats;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Transactional
public class SocialController {

	private final EngagementService engagement;
	private final CatalogQueryService catalog;
	private final CurrentUser currentUser;

	public SocialController(EngagementService engagement, CatalogQueryService catalog, CurrentUser currentUser) {
		this.engagement = engagement;
		this.catalog = catalog;
		this.currentUser = currentUser;
	}

	@GetMapping("/api/me")
	public ProfileView me() {
		UserAccount user = currentUser.require();
		List<RatingMine> ratings = engagement.ratingsOf(user).stream()
				.map(rating -> new RatingMine(catalog.card(rating.getCatalog()), rating.getScore(), rating.getOpinion(), rating.getUpdatedAt()))
				.toList();
		List<Card> favoriteCards = engagement.favorites(user).stream().map(item -> catalog.card(item.getCatalog())).toList();
		List<ShelfView> shelfViews = engagement.shelves(user).stream().map(this::shelfView).toList();
		List<HistoryView> history = engagement.historyOf(user).stream()
				.map(entry -> new HistoryView(catalog.card(entry.getCatalog()), entry.getViewedAt()))
				.toList();
		List<SavedView> saved = engagement.library(user, null).stream()
				.map(item -> new SavedView(item.getId(), item.isIncludedInPlan(), catalog.card(item.getCatalog())))
				.toList();
		return new ProfileView(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), ratings, favoriteCards, shelfViews, history, saved);
	}

	@GetMapping("/api/library")
	public List<SavedView> library(@RequestParam(required = false) String kind) {
		UserAccount user = currentUser.require();
		ContentKind parsed = kind == null || kind.isBlank() ? null : ContentKind.parse(kind);
		return engagement.library(user, parsed).stream()
				.map(item -> new SavedView(item.getId(), item.isIncludedInPlan(), catalog.card(item.getCatalog())))
				.toList();
	}

	@PostMapping("/api/library")
	public ResponseEntity<SavedView> save(@Valid @RequestBody DraftRequest request) {
		UserAccount user = currentUser.require();
		var result = engagement.save(user, ContentKind.parse(request.kind()), request.externalId(), draft(request));
		SavedItem item = result.item();
		SavedView body = new SavedView(item.getId(), item.isIncludedInPlan(), catalog.card(item.getCatalog()));
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(body);
	}

	@PatchMapping("/api/library/{id}")
	public SavedView plan(@PathVariable Long id, @RequestBody PlanBody request) {
		UserAccount user = currentUser.require();
		SavedItem item = engagement.updatePlan(user, id, request.includedInPlan());
		return new SavedView(item.getId(), item.isIncludedInPlan(), catalog.card(item.getCatalog()));
	}

	@DeleteMapping("/api/library/{id}")
	public ResponseEntity<Void> unsave(@PathVariable Long id) {
		engagement.unsave(currentUser.require(), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/ratings")
	public RateResponse rate(@Valid @RequestBody RateRequest request) {
		UserAccount user = currentUser.require();
		Rating rating = engagement.rate(user, ContentKind.parse(request.kind()), request.externalId(), request.score(), request.opinion(), draft(request));
		return toRate(rating);
	}

	@GetMapping("/api/ratings")
	public List<OpinionView> opinions(@RequestParam String kind, @RequestParam String externalId) {
		return engagement.opinions(ContentKind.parse(kind), externalId).stream()
				.filter(rating -> rating.getOpinion() != null && !rating.getOpinion().isBlank())
				.map(rating -> new OpinionView(
						rating.getUser().getUsername(),
						rating.getUser().getDisplayName(),
						rating.getScore(),
						rating.getOpinion(),
						rating.getUpdatedAt()
				))
				.toList();
	}

	@PostMapping("/api/favorites")
	public ResponseEntity<Card> favorite(@Valid @RequestBody DraftRequest request) {
		UserAccount user = currentUser.require();
		engagement.toggleFavorite(user, ContentKind.parse(request.kind()), request.externalId(), draft(request));
		return engagement.find(ContentKind.parse(request.kind()), request.externalId())
				.map(entry -> ResponseEntity.ok(catalog.card(entry)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/api/history")
	public ResponseEntity<Void> history(@Valid @RequestBody DraftRequest request) {
		engagement.remember(currentUser.require(), ContentKind.parse(request.kind()), request.externalId(), draft(request));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/shelves")
	public List<ShelfView> shelves() {
		UserAccount user = currentUser.require();
		return engagement.shelves(user).stream().map(this::shelfView).toList();
	}

	@PostMapping("/api/shelves")
	public ResponseEntity<ShelfView> createShelf(@Valid @RequestBody ShelfRequest request) {
		Shelf shelf = engagement.createShelf(currentUser.require(), request.name());
		return ResponseEntity.status(HttpStatus.CREATED).body(shelfView(shelf));
	}

	@DeleteMapping("/api/shelves/{id}")
	public ResponseEntity<Void> deleteShelf(@PathVariable Long id) {
		engagement.deleteShelf(currentUser.require(), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/shelves/{id}/items")
	public ShelfView addItem(@PathVariable Long id, @Valid @RequestBody DraftRequest request) {
		UserAccount user = currentUser.require();
		engagement.addToShelf(user, id, ContentKind.parse(request.kind()), request.externalId(), draft(request));
		Shelf shelf = engagement.shelves(user).stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
		return shelfView(shelf);
	}

	@DeleteMapping("/api/shelves/{id}/items/{catalogId}")
	public ResponseEntity<Void> removeItem(@PathVariable Long id, @PathVariable Long catalogId) {
		engagement.removeFromShelf(currentUser.require(), id, catalogId);
		return ResponseEntity.noContent().build();
	}

	private ShelfView shelfView(Shelf shelf) {
		List<Card> items = engagement.shelfItems(shelf).stream().map(item -> catalog.card(item.getCatalog())).toList();
		return new ShelfView(shelf.getId(), shelf.getName(), items);
	}

	private RateResponse toRate(Rating rating) {
		Stats stats = engagement.stats(rating.getCatalog());
		return new RateResponse(
				rating.getCatalog().getId(),
				rating.getScore(),
				stats.average(),
				stats.votes(),
				engagement.classification(stats.average(), stats.votes()),
				engagement.recommendation(stats.average(), stats.votes())
		);
	}

	private CatalogDraft draft(DraftRequest request) {
		return new CatalogDraft(
				request.title(),
				request.year(),
				YearParser.yearOrZero(request.year()),
				request.creator(),
				request.imageUrl(),
				request.synopsis(),
				request.category(),
				request.runtimeMinutes(),
				request.previewUrl(),
				request.sourceRating(),
				request.subtype()
		);
	}

	private CatalogDraft draft(RateRequest request) {
		return new CatalogDraft(
				request.title(),
				request.year(),
				YearParser.yearOrZero(request.year()),
				request.creator(),
				request.imageUrl(),
				request.synopsis(),
				request.category(),
				request.runtimeMinutes(),
				request.previewUrl(),
				request.sourceRating(),
				request.subtype()
		);
	}
}
