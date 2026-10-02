package com.orbe.content;

import com.orbe.social.dto.SocialDtos.Card;
import com.orbe.social.dto.SocialDtos.HomeView;
import com.orbe.social.service.CatalogQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ContentController {

	private final BookService books;
	private final MusicCatalogService music;
	private final CatalogQueryService catalog;

	public ContentController(BookService books, MusicCatalogService music, CatalogQueryService catalog) {
		this.books = books;
		this.music = music;
		this.catalog = catalog;
	}

	@GetMapping("/api/catalog/home")
	public HomeView home() {
		return catalog.home();
	}

	@GetMapping("/api/catalog")
	public List<Card> catalog(
			@RequestParam(required = false) String kind,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String category
	) {
		return catalog.search(kind, q, category);
	}

	@GetMapping("/api/books/search")
	public List<Card> searchBooks(@RequestParam String q) {
		return books.search(q);
	}

	@GetMapping("/api/books/{id}")
	public Card book(@PathVariable String id) {
		return books.details(id);
	}

	@GetMapping("/api/music/search")
	public List<Card> searchMusic(@RequestParam String q) {
		return music.search(q);
	}
}
