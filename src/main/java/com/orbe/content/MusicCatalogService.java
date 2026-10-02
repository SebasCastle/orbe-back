package com.orbe.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.orbe.cinema.domain.YearParser;
import com.orbe.common.ApiException;
import com.orbe.common.ExternalHttp;
import com.orbe.common.Texts;
import com.orbe.social.dto.SocialDtos.Card;
import com.orbe.social.model.CatalogDraft;
import com.orbe.social.model.ContentKind;
import com.orbe.social.service.CatalogQueryService;
import com.orbe.social.service.EngagementService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MusicCatalogService {

	private final ExternalHttp http;
	private final EngagementService engagement;
	private final CatalogQueryService catalog;

	public MusicCatalogService(ExternalHttp http, EngagementService engagement, CatalogQueryService catalog) {
		this.http = http;
		this.engagement = engagement;
		this.catalog = catalog;
	}

	@Transactional
	public List<Card> search(String query) {
		if (Texts.blank(query)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Escribe una canción o un artista.");
		}
		return store(fetch(query.trim()));
	}

	public List<Card> store(JsonNode results) {
		List<Card> cards = new ArrayList<>();
		if (results == null || !results.isArray()) {
			return cards;
		}
		for (JsonNode track : results) {
			CatalogDraft draft = fromTrack(track);
			String externalId = trackId(track);
			if (draft == null || externalId == null) {
				continue;
			}
			cards.add(catalog.card(engagement.upsert(ContentKind.TRACK, externalId, draft)));
		}
		return cards;
	}

	public JsonNode fetch(String term) {
		String url = "https://itunes.apple.com/search?entity=song&limit=24&term="
				+ UriUtils.encodeQueryParam(term, StandardCharsets.UTF_8);
		return http.getTree(url).path("results");
	}

	public static CatalogDraft fromTrack(JsonNode track) {
		String title = Texts.clean(track.path("trackName").asText(null));
		if (title == null) {
			return null;
		}
		String artist = Texts.clean(track.path("artistName").asText(null));
		String album = Texts.clean(track.path("collectionName").asText(null));
		String artwork = Texts.clean(track.path("artworkUrl100").asText(null));
		if (artwork != null) {
			artwork = artwork.replace("100x100bb", "300x300bb");
		}
		String preview = Texts.clean(track.path("previewUrl").asText(null));
		String genre = Texts.clean(track.path("primaryGenreName").asText(null));
		String released = Texts.clean(track.path("releaseDate").asText(null));
		int year = YearParser.yearOrZero(released);
		Integer minutes = null;
		int millis = track.path("trackTimeMillis").asInt(0);
		if (millis > 0) {
			minutes = Math.max(1, millis / 60000);
		}
		String synopsis = album == null ? null : "Álbum: " + album;
		return new CatalogDraft(title, year > 0 ? String.valueOf(year) : null, year, artist, artwork, synopsis, genre, minutes, preview, null, "song");
	}

	public static String trackId(JsonNode track) {
		long id = track.path("trackId").asLong(0);
		return id > 0 ? Long.toString(id) : null;
	}
}
