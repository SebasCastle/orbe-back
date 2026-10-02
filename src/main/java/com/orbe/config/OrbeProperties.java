package com.orbe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orbe")
	public record OrbeProperties(
		String frontendUrl,
		Omdb omdb,
		Swapi swapi,
		Spotify spotify,
		Jwt jwt
) {
	public record Omdb(String apiKey, String baseUrl) {
	}

	public record Swapi(String baseUrl) {
	}

	public record Spotify(String clientId, String clientSecret, String redirectUri) {
	}

	public record Jwt(String secret) {
	}
}
