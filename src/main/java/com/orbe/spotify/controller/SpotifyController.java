package com.orbe.spotify.controller;

import com.orbe.common.ApiException;
import com.orbe.config.OrbeProperties;
import com.orbe.spotify.dto.SpotifyDtos.AlbumPage;
import com.orbe.spotify.dto.SpotifyDtos.AuthUrlResponse;
import com.orbe.spotify.dto.SpotifyDtos.SpotifyProfile;
import com.orbe.spotify.dto.SpotifyDtos.TrackCard;
import com.orbe.security.CurrentUser;
import com.orbe.spotify.service.SpotifyService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Validated
public class SpotifyController {

	private final SpotifyService spotify;
	private final OrbeProperties properties;
	private final CurrentUser currentUser;

	public SpotifyController(SpotifyService spotify, OrbeProperties properties, CurrentUser currentUser) {
		this.spotify = spotify;
		this.properties = properties;
		this.currentUser = currentUser;
	}

	@GetMapping({"/api/spotify/login", "/api/login"})
	public AuthUrlResponse login() {
		Long ownerId = currentUser.optional().map(user -> user.getId()).orElse(null);
		return spotify.authorizationUrl(ownerId);
	}

	@GetMapping({"/api/spotify/callback", "/api/get-user-code"})
	public void callback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String error,
			HttpServletResponse response
	) throws IOException {
		if (error != null || code == null || code.isBlank()) {
			response.sendRedirect(redirectError(
					"access_denied".equals(error) ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST,
					"access_denied".equals(error) ? "SPOTIFY_ACCESS_DENIED" : "SPOTIFY_BAD_REQUEST"
			));
			return;
		}
		try {
			SpotifyProfile profile = spotify.connect(code, state);
			response.sendRedirect(frontend("/spotify?userId=" + encode(profile.id())));
		} catch (ApiException exception) {
			response.sendRedirect(redirectError(exception.status(), errorCode(exception.status())));
		} catch (RuntimeException exception) {
			response.sendRedirect(redirectError(HttpStatus.BAD_GATEWAY, "SPOTIFY_TEMPORARY_ERROR"));
		}
	}

	@GetMapping({"/api/spotify/profile", "/api/home"})
	public SpotifyProfile profile(@RequestParam String userId) {
		return spotify.profile(userId);
	}

	@GetMapping({"/api/spotify/albums", "/api/user-saved-album"})
	public AlbumPage albums(
			@RequestParam String userId,
			@RequestParam(defaultValue = "0") @Min(0) @Max(1000) int offset
	) {
		return spotify.albums(userId, offset);
	}

	@GetMapping({"/api/spotify/top-tracks", "/api/user-top-songs"})
	public List<TrackCard> topTracks(
			@RequestParam String userId,
			@RequestParam(defaultValue = "medium_term") String range
	) {
		return spotify.topTracks(userId, range);
	}

	@DeleteMapping("/api/spotify/session")
	public ResponseEntity<Void> disconnect(@RequestParam String userId) {
		spotify.disconnect(userId);
		return ResponseEntity.noContent().build();
	}

	private String redirectError(HttpStatus status, String code) {
		return frontend("/spotify?errorStatus=" + status.value() + "&errorCode=" + encode(code));
	}

	private String errorCode(HttpStatus status) {
		return switch (status.value()) {
			case 400 -> "SPOTIFY_BAD_REQUEST";
			case 401 -> "SPOTIFY_UNAUTHORIZED";
			case 403 -> "SPOTIFY_FORBIDDEN";
			case 404 -> "SPOTIFY_NOT_FOUND";
			case 429 -> "SPOTIFY_RATE_LIMITED";
			default -> status.is5xxServerError() ? "SPOTIFY_TEMPORARY_ERROR" : "SPOTIFY_ERROR";
		};
	}

	private String frontend(String path) {
		String base = properties.frontendUrl() == null || properties.frontendUrl().isBlank()
				? "http://localhost:5173"
				: properties.frontendUrl().trim();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + path;
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
