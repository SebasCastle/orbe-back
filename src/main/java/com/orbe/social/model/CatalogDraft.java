package com.orbe.social.model;

public record CatalogDraft(
		String title,
		String yearLabel,
		int releaseYear,
		String creator,
		String imageUrl,
		String synopsis,
		String category,
		Integer runtimeMinutes,
		String previewUrl,
		String sourceRating,
		String subtype
) {
}
