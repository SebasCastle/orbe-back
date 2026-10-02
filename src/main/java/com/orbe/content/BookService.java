package com.orbe.content;

import com.orbe.common.ApiException;
import com.orbe.common.ExternalHttp;
import com.orbe.common.Texts;
import com.orbe.social.dto.SocialDtos.Card;
import com.orbe.social.model.CatalogDraft;
import com.orbe.social.model.ContentKind;
import com.orbe.social.service.CatalogQueryService;
import com.orbe.social.service.EngagementService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {

	private final ExternalHttp http;
	private final EngagementService engagement;
	private final CatalogQueryService catalog;

	public BookService(ExternalHttp http, EngagementService engagement, CatalogQueryService catalog) {
		this.http = http;
		this.engagement = engagement;
		this.catalog = catalog;
	}

	@Transactional
	public List<Card> search(String query) {
		if (Texts.blank(query)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Escribe un libro para buscar.");
		}
		String url = "https://openlibrary.org/search.json?limit=20&fields=key,title,author_name,first_publish_year,cover_i,subject,first_sentence&q="
				+ UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
		JsonNode docs = http.getTree(url).path("docs");
		List<Card> cards = new ArrayList<>();
		if (docs.isArray()) {
			for (JsonNode doc : docs) {
				CatalogDraft draft = fromSearch(doc);
				String externalId = workId(doc.path("key").asText(""));
				if (draft == null || externalId == null) {
					continue;
				}
				cards.add(catalog.card(engagement.upsert(ContentKind.BOOK, externalId, draft)));
			}
		}
		return cards;
	}

	@Transactional
	public Card details(String id) {
		String externalId = workId(id);
		if (externalId == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El identificador del libro no es válido.");
		}
		JsonNode work = http.getTree("https://openlibrary.org/works/" + externalId + ".json");
		String title = text(work, "title");
		if (title == null) {
			return engagement.find(ContentKind.BOOK, externalId)
					.map(catalog::card)
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encontré ese libro."));
		}
		String synopsis = description(work.get("description"));
		CatalogDraft draft = new CatalogDraft(title, null, 0, null, null, synopsis, null, null, null, null, "book");
		var existing = engagement.find(ContentKind.BOOK, externalId).orElse(null);
		if (existing != null && Texts.blank(synopsis)) {
			draft = new CatalogDraft(title, existing.getYearLabel(), existing.getReleaseYear(), existing.getCreator(), existing.getImageUrl(), existing.getSynopsis(), existing.getCategory(), existing.getRuntimeMinutes(), null, null, "book");
		}
		return catalog.card(engagement.upsert(ContentKind.BOOK, externalId, draft));
	}

	public static CatalogDraft fromSearch(JsonNode doc) {
		String title = text(doc, "title");
		if (title == null) {
			return null;
		}
		int year = doc.path("first_publish_year").asInt(0);
		String author = firstText(doc.path("author_name"));
		int cover = doc.path("cover_i").asInt(0);
		String image = cover > 0 ? "https://covers.openlibrary.org/b/id/" + cover + "-M.jpg" : null;
		String subject = firstText(doc.path("subject"));
		String sentence = firstText(doc.path("first_sentence"));
		return new CatalogDraft(title, year > 0 ? String.valueOf(year) : null, year, author, image, sentence, subject, null, null, null, "book");
	}

	public static String workId(String key) {
		if (key == null) {
			return null;
		}
		String trimmed = key.trim();
		int slash = trimmed.lastIndexOf('/');
		String id = slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
		if (!id.matches("OL\\d+W")) {
			return null;
		}
		return id;
	}

	private static String description(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isTextual()) {
			return Texts.clean(node.asText());
		}
		return Texts.clean(node.path("value").asText(null));
	}

	private static String text(JsonNode node, String field) {
		return Texts.clean(node.path(field).asText(null));
	}

	private static String firstText(JsonNode node) {
		if (node == null || !node.isArray() || node.isEmpty()) {
			return null;
		}
		JsonNode first = node.get(0);
		return first != null && first.isTextual() ? Texts.clean(first.asText()) : null;
	}
}
