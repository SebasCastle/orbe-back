package com.orbe.cinema.omdb;

import com.orbe.cinema.omdb.OmdbModels.OmdbSearch;
import com.orbe.cinema.omdb.OmdbModels.OmdbSearchItem;
import com.orbe.cinema.omdb.OmdbModels.OmdbTitle;
import com.orbe.common.ApiException;
import com.orbe.common.ExternalHttp;
import com.orbe.common.Texts;
import com.orbe.config.OrbeProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Locale;

@Service
public class OmdbClient {

	private final OrbeProperties properties;
	private final ExternalHttp http;

	public OmdbClient(OrbeProperties properties, ExternalHttp http) {
		this.properties = properties;
		this.http = http;
	}

	public OmdbSearch search(String query, int page) {
		ensureKey();
		String url = builder()
				.queryParam("s", query)
				.queryParam("page", page)
				.build()
				.toUriString();
		OmdbSearch result = http.get(url, OmdbSearch.class);
		if (result != null && failed(result.response())) {
			String error = result.error() == null ? "" : result.error();
			String text = error.toLowerCase(Locale.ROOT);
			if (text.contains("not found")) {
				return empty();
			}
			if (text.contains("too many") && page == 1) {
				return exactTitle(query);
			}
			throw mapped(error);
		}
		return result == null ? empty() : result;
	}

	public OmdbTitle details(String imdbId) {
		ensureKey();
		String url = builder()
				.queryParam("i", imdbId)
				.queryParam("plot", "full")
				.build()
				.toUriString();
		OmdbTitle title = http.get(url, OmdbTitle.class);
		if (title == null || failed(title.response()) || Texts.blank(title.title())) {
			throw mapped(title == null ? null : title.error());
		}
		return title;
	}

	private OmdbSearch exactTitle(String query) {
		String url = builder()
				.queryParam("t", query)
				.queryParam("plot", "short")
				.build()
				.toUriString();
		OmdbTitle title = http.get(url, OmdbTitle.class);
		if (title == null || failed(title.response()) || Texts.blank(title.title())) {
			throw mapped(title == null ? "Too many results." : title.error());
		}
		return new OmdbSearch(
				List.of(new OmdbSearchItem(title.title(), title.year(), title.imdbId(), title.type(), title.poster())),
				"1",
				"True",
				null
		);
	}

	private OmdbSearch empty() {
		return new OmdbSearch(List.of(), "0", "True", null);
	}

	private UriComponentsBuilder builder() {
		String base = properties.omdb() == null || Texts.blank(properties.omdb().baseUrl())
				? "https://www.omdbapi.com/"
				: properties.omdb().baseUrl();
		return UriComponentsBuilder.fromHttpUrl(base)
				.queryParam("apikey", properties.omdb().apiKey());
	}

	private void ensureKey() {
		if (properties.omdb() == null || Texts.blank(properties.omdb().apiKey())) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Falta la clave de OMDB. Agrégala como OMDB_API_KEY.");
		}
	}

	private boolean failed(String response) {
		return response != null && response.equalsIgnoreCase("False");
	}

	private ApiException mapped(String error) {
		String text = error == null ? "" : error.toLowerCase(Locale.ROOT);
		if (text.contains("invalid api key")) {
			return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La clave de OMDB no es válida.");
		}
		if (text.contains("too many")) {
			return new ApiException(HttpStatus.BAD_REQUEST, "La búsqueda es muy amplia. Prueba con un título más específico.");
		}
		if (text.contains("not found")) {
			return new ApiException(HttpStatus.NOT_FOUND, "No encontré ese título.");
		}
		if (text.contains("limit")) {
			return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OMDB está limitando las consultas. Inténtalo en unos segundos.");
		}
		return new ApiException(HttpStatus.BAD_GATEWAY, "OMDB no pudo completar la consulta.");
	}
}
