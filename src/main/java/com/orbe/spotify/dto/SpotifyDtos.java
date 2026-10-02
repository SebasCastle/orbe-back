package com.orbe.spotify.dto;

import java.util.List;

public final class SpotifyDtos {

	private SpotifyDtos() {
	}

	public record AuthUrlResponse(String authorizationUrl) {
	}

	public record SpotifyProfile(
			String id,
			String displayName,
			String email,
			String imageUrl,
			String product
	) {
	}

	public record AlbumCard(
			String id,
			String name,
			String artists,
			String imageUrl,
			String releaseDate,
			int totalTracks,
			String externalUrl
	) {
	}

	public record AlbumPage(List<AlbumCard> albums, int total, int offset) {
	}

	public record TrackCard(
			String id,
			String name,
			String artists,
			String albumName,
			String imageUrl,
			int durationMs,
			Integer popularity,
			String previewUrl,
			String externalUrl
	) {
	}
}
