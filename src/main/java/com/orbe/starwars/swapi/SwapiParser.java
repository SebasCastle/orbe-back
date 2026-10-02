package com.orbe.starwars.swapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class SwapiParser {

	private SwapiParser() {
	}

	public static List<FilmSnapshot> parseList(JsonNode root) {
		if (root == null || root.isNull()) {
			return List.of();
		}
		if (root.isArray()) {
			return parseArray(root);
		}
		if (root.has("results") && root.get("results").isArray()) {
			return parseArray(root.get("results"));
		}
		FilmSnapshot single = parseOne(root, 0);
		return single == null ? List.of() : List.of(single);
	}

	public static FilmSnapshot parseOne(JsonNode node, int fallbackId) {
		if (node == null || node.isNull()) {
			return null;
		}
		JsonNode source = node.hasNonNull("properties") ? node.get("properties") : node;
		if (node.has("result") && node.get("result").hasNonNull("properties")) {
			source = node.get("result").get("properties");
		}
		String title = text(source, "title");
		if (title == null) {
			title = text(source, "name");
		}
		if (title == null) {
			title = text(node, "name");
		}
		if (title == null) {
			return null;
		}
		int episodeId = source.path("episode_id").asInt(0);
		String url = text(source, "url");
		if (url == null) {
			url = text(node, "url");
		}
		int id = SwapiIds.fromUrl(url);
		if (id == 0) {
			id = parseInt(text(node, "uid"));
		}
		if (id == 0 && node.has("result")) {
			id = parseInt(text(node.get("result"), "uid"));
		}
		if (id == 0) {
			id = fallbackId;
		}
		if (id == 0) {
			id = episodeId;
		}
		return new FilmSnapshot(
				id,
				episodeId,
				title,
				text(source, "opening_crawl"),
				text(source, "director"),
				text(source, "producer"),
				text(source, "release_date"),
				url
		);
	}

	private static List<FilmSnapshot> parseArray(JsonNode array) {
		List<FilmSnapshot> films = new ArrayList<>();
		for (JsonNode node : array) {
			FilmSnapshot film = parseOne(node, 0);
			if (film != null) {
				films.add(film);
			}
		}
		return films;
	}

	private static String text(JsonNode node, String field) {
		if (node == null || !node.hasNonNull(field)) {
			return null;
		}
		String value = node.get(field).asText();
		if (value == null || value.isBlank() || value.equalsIgnoreCase("null") || value.equalsIgnoreCase("n/a")) {
			return null;
		}
		return value.trim();
	}

	private static int parseInt(String value) {
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return 0;
		}
	}
}
