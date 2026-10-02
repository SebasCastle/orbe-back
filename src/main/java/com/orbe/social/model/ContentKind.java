package com.orbe.social.model;

public enum ContentKind {
	MOVIE,
	BOOK,
	TRACK,
	FILM;

	public static ContentKind parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new com.orbe.common.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "El tipo de contenido no es válido.");
		}
		try {
			return ContentKind.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new com.orbe.common.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "El tipo de contenido no es válido.");
		}
	}
}
