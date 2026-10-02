package com.orbe.spotify.service;

import com.orbe.common.ApiException;
import com.orbe.common.Texts;
import com.orbe.config.OrbeProperties;
import com.orbe.security.CurrentUser;
import com.orbe.spotify.dto.SpotifyDtos.AlbumCard;
import com.orbe.spotify.dto.SpotifyDtos.AlbumPage;
import com.orbe.spotify.dto.SpotifyDtos.AuthUrlResponse;
import com.orbe.spotify.dto.SpotifyDtos.SpotifyProfile;
import com.orbe.spotify.dto.SpotifyDtos.TrackCard;
import com.orbe.spotify.model.SpotifyAccount;
import com.orbe.spotify.repository.SpotifyAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.BadGatewayException;
import se.michaelthelin.spotify.exceptions.detailed.BadRequestException;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.exceptions.detailed.InternalServerErrorException;
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import se.michaelthelin.spotify.exceptions.detailed.ServiceUnavailableException;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SpotifyService {

	private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);
	private static final String SCOPES = "user-read-private user-library-read user-top-read";
	private static final Set<String> RANGES = Set.of("short_term", "medium_term", "long_term");

	private final OrbeProperties properties;
	private final SpotifyAccountRepository accounts;
	private final CurrentUser currentUser;
	private final ConcurrentHashMap<String, Pending> states = new ConcurrentHashMap<>();

	public SpotifyService(OrbeProperties properties, SpotifyAccountRepository accounts, CurrentUser currentUser) {
		this.properties = properties;
		this.accounts = accounts;
		this.currentUser = currentUser;
	}

	public AuthUrlResponse authorizationUrl(Long ownerId) {
		ensureConfigured();
		pruneStates();
		String state = UUID.randomUUID().toString();
		states.put(state, new Pending(Instant.now().plus(Duration.ofMinutes(10)), ownerId));
		SpotifyApi api = newApi();
		URI uri = execute("GET /authorize (authorization URL)", () -> api.authorizationCodeUri()
				.scope(SCOPES)
				.show_dialog(true)
				.state(state)
				.build()
				.execute());
		return new AuthUrlResponse(uri.toString());
	}

	@Transactional
	public SpotifyProfile connect(String code, String state) {
		Long ownerId = consumeState(state);
		ensureConfigured();
		SpotifyApi api = newApi();
		AuthorizationCodeCredentials credentials = execute(
				"POST /api/token (authorization code)",
				() -> api.authorizationCode(code).build().execute()
		);
		api.setAccessToken(credentials.getAccessToken());
		if (credentials.getRefreshToken() != null) {
			api.setRefreshToken(credentials.getRefreshToken());
		}
		User user = execute("GET /v1/me (OAuth callback)", () -> api.getCurrentUsersProfile().build().execute());
		SpotifyAccount account = accounts.findByRefId(user.getId()).orElseGet(SpotifyAccount::new);
		account.setRefId(user.getId());
		account.setDisplayName(user.getDisplayName());
		account.setImageUrl(image(user.getImages()));
		account.setAccessToken(credentials.getAccessToken());
		if (!Texts.blank(credentials.getRefreshToken())) {
			account.setRefreshToken(credentials.getRefreshToken());
		}
		account.setAccessTokenExpiresAt(expiry(credentials));
		if (ownerId != null) {
			if (account.getOwnerUserId() != null && !account.getOwnerUserId().equals(ownerId)) {
				throw new ApiException(HttpStatus.CONFLICT, "Esa cuenta de Spotify ya está vinculada a otro perfil.");
			}
			account.setOwnerUserId(ownerId);
		}
		accounts.save(account);
		return profileOf(account);
	}

	@Transactional
	public SpotifyProfile profile(String userId) {
		SpotifyAccount account = requireOwned(userId);
		SpotifyApi api = authorized(account);
		User user = execute("GET /v1/me", () -> api.getCurrentUsersProfile().build().execute());
		account.setDisplayName(user.getDisplayName());
		account.setImageUrl(image(user.getImages()));
		accounts.save(account);
		return profileOf(account);
	}

	@Transactional
	public AlbumPage albums(String userId, int offset) {
		SpotifyAccount account = requireOwned(userId);
		SpotifyApi api = authorized(account);
		Paging<SavedAlbum> paging = execute("GET /v1/me/albums", () -> api.getUsersSavedAlbums()
				.limit(24)
				.offset(offset)
				.build()
				.execute());
		if (paging == null || paging.getItems() == null) {
			return new AlbumPage(List.of(), 0, offset);
		}
		List<AlbumCard> albums = new ArrayList<>();
		for (SavedAlbum saved : paging.getItems()) {
			if (saved == null || saved.getAlbum() == null) {
				continue;
			}
			var album = saved.getAlbum();
			albums.add(new AlbumCard(
					album.getId(),
					album.getName(),
					artists(album.getArtists()),
					image(album.getImages()),
					album.getReleaseDate(),
					trackCount(album),
					album.getId() == null ? null : "https://open.spotify.com/album/" + album.getId()
			));
		}
		Integer total = paging.getTotal();
		return new AlbumPage(albums, total == null ? albums.size() : total, offset);
	}

	@Transactional
	public List<TrackCard> topTracks(String userId, String range) {
		if (!RANGES.contains(range)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El periodo debe ser short_term, medium_term o long_term.");
		}
		SpotifyAccount account = requireOwned(userId);
		SpotifyApi api = authorized(account);
		Paging<Track> paging = execute("GET /v1/me/top/tracks", () -> api.getUsersTopTracks()
				.time_range(range)
				.limit(10)
				.offset(0)
				.build()
				.execute());
		if (paging == null || paging.getItems() == null) {
			return List.of();
		}
		List<TrackCard> tracks = new ArrayList<>();
		for (Track track : paging.getItems()) {
			if (track == null) {
				continue;
			}
			String albumName = track.getAlbum() == null ? null : track.getAlbum().getName();
			String imageUrl = track.getAlbum() == null ? null : image(track.getAlbum().getImages());
			tracks.add(new TrackCard(
					track.getId(),
					track.getName(),
					artists(track.getArtists()),
					albumName,
					imageUrl,
					track.getDurationMs() == null ? 0 : track.getDurationMs(),
					null,
					track.getPreviewUrl(),
					track.getId() == null ? null : "https://open.spotify.com/track/" + track.getId()
			));
		}
		return tracks;
	}

	@Transactional
	public void disconnect(String userId) {
		if (Texts.blank(userId)) {
			return;
		}
		SpotifyAccount account = requireOwned(userId);
		accounts.delete(account);
	}

	private SpotifyApi authorized(SpotifyAccount account) {
		SpotifyApi api = newApi();
		if (needsRefresh(account)) {
			if (Texts.blank(account.getRefreshToken())) {
				throw new ApiException(HttpStatus.UNAUTHORIZED, "La sesión de Spotify expiró. Conéctala de nuevo.");
			}
			api.setRefreshToken(account.getRefreshToken());
				AuthorizationCodeCredentials credentials = execute(
						"POST /api/token (refresh token)",
						() -> api.authorizationCodeRefresh().build().execute()
				);
			account.setAccessToken(credentials.getAccessToken());
			if (!Texts.blank(credentials.getRefreshToken())) {
				account.setRefreshToken(credentials.getRefreshToken());
			}
			account.setAccessTokenExpiresAt(expiry(credentials));
			accounts.save(account);
		}
		api.setAccessToken(account.getAccessToken());
		if (!Texts.blank(account.getRefreshToken())) {
			api.setRefreshToken(account.getRefreshToken());
		}
		return api;
	}

	private boolean needsRefresh(SpotifyAccount account) {
		Instant expiry = account.getAccessTokenExpiresAt();
		return expiry == null || Instant.now().isAfter(expiry.minusSeconds(60));
	}

	private SpotifyAccount require(String userId) {
		if (Texts.blank(userId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Falta el identificador de usuario.");
		}
		return accounts.findByRefId(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No encuentro esa sesión de Spotify. Conéctala de nuevo."));
	}

	private SpotifyAccount requireOwned(String userId) {
		SpotifyAccount account = require(userId);
		var current = currentUser.require();
		if (account.getOwnerUserId() == null || !account.getOwnerUserId().equals(current.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Esa sesión de Spotify no pertenece a tu cuenta.");
		}
		return account;
	}

	private Long consumeState(String state) {
		if (Texts.blank(state)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La autorización de Spotify no incluyó el estado de seguridad.");
		}
		Pending pending = states.remove(state);
		if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La autorización expiró o no es válida. Inténtalo de nuevo.");
		}
		return pending.ownerId();
	}

	private void pruneStates() {
		Instant now = Instant.now();
		states.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
	}

	private record Pending(Instant expiresAt, Long ownerId) {
	}

	private SpotifyApi newApi() {
		ensureConfigured();
		return new SpotifyApi.Builder()
				.setHttpManager(new SpotifyHttpManager.Builder().build())
				.setClientId(properties.spotify().clientId().trim())
				.setClientSecret(properties.spotify().clientSecret().trim())
				.setRedirectUri(URI.create(properties.spotify().redirectUri().trim()))
				.build();
	}

	private void ensureConfigured() {
		if (properties.spotify() == null
				|| Texts.blank(properties.spotify().clientId())
				|| Texts.blank(properties.spotify().clientSecret())
				|| Texts.blank(properties.spotify().redirectUri())) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
					"Spotify no está configurado. Agrega SPOTIFY_CLIENT_ID y SPOTIFY_CLIENT_SECRET.");
		}
	}

	private Instant expiry(AuthorizationCodeCredentials credentials) {
		int seconds = credentials.getExpiresIn() == null ? 3600 : credentials.getExpiresIn();
		return Instant.now().plusSeconds(Math.max(seconds, 60));
	}

	private SpotifyProfile profileOf(SpotifyAccount account) {
		return new SpotifyProfile(
				account.getRefId(),
				account.getDisplayName(),
				null,
				account.getImageUrl(),
				null
		);
	}

	private String artists(ArtistSimplified[] artists) {
		if (artists == null || artists.length == 0) {
			return "Artista desconocido";
		}
		String names = Arrays.stream(artists)
				.map(artist -> artist == null ? null : artist.getName())
				.filter(name -> !Texts.blank(name))
				.collect(Collectors.joining(", "));
		return names.isBlank() ? "Artista desconocido" : names;
	}

	private int trackCount(se.michaelthelin.spotify.model_objects.specification.Album album) {
		if (album.getTracks() == null || album.getTracks().getTotal() == null) {
			return 0;
		}
		return album.getTracks().getTotal();
	}

	private String image(Image[] images) {
		if (images == null) {
			return null;
		}
		for (Image image : images) {
			if (image != null && !Texts.blank(image.getUrl())) {
				return image.getUrl();
			}
		}
		return null;
	}

	private <T> T execute(String operation, SpotifyCall<T> call) {
		try {
			return call.run();
		} catch (ApiException exception) {
			logDiagnostic(operation, exception);
			throw exception;
		} catch (Exception exception) {
			logDiagnostic(operation, exception);
			HttpStatus status = actualHttpStatus(exception).orElse(HttpStatus.BAD_GATEWAY);
			ApiException apiException = new ApiException(status, spotifyMessage(status));
			apiException.initCause(exception);
			throw apiException;
		}
	}

	private java.util.Optional<HttpStatus> actualHttpStatus(Throwable exception) {
		for (Throwable current : causeChain(exception)) {
			if (current instanceof ApiException apiException) {
				return java.util.Optional.of(apiException.status());
			}
			Integer status = statusFromAccessor(current);
			if (status == null) {
				status = statusFromMessage(current.getMessage());
			}
			if (status != null) {
				HttpStatus resolved = HttpStatus.resolve(status);
				if (resolved != null) return java.util.Optional.of(resolved);
			}
			if (current instanceof SpotifyWebApiException) {
				HttpStatus sdkStatus = spotifySdkStatus(current);
				if (sdkStatus != null) return java.util.Optional.of(sdkStatus);
			}
		}
		return java.util.Optional.empty();
	}

	private Integer statusFromAccessor(Throwable exception) {
		for (String methodName : List.of("getStatusCode", "getStatus", "statusCode", "status")) {
			try {
				Method method = exception.getClass().getMethod(methodName);
				Object value = method.invoke(exception);
				if (value instanceof Number number) return number.intValue();
				if (value instanceof HttpStatus status) return status.value();
				if (value instanceof org.springframework.http.HttpStatusCode status) return status.value();
			} catch (ReflectiveOperationException ignored) {
				// This exception type does not expose an HTTP status through this accessor.
			}
		}
		return null;
	}

	private Integer statusFromMessage(String message) {
		if (message == null) return null;
		java.util.regex.Matcher matcher = java.util.regex.Pattern
				.compile("(?i)(?:HTTP(?:/\\d(?:\\.\\d)?)?\\s+|status(?:\\s*code)?\\s*[:=]\\s*|\\[)([1-5]\\d{2})(?:\\]|\\b)")
				.matcher(message);
		return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
	}

	private HttpStatus spotifySdkStatus(Throwable exception) {
		if (exception instanceof BadRequestException) return HttpStatus.BAD_REQUEST;
		if (exception instanceof UnauthorizedException) return HttpStatus.UNAUTHORIZED;
		if (exception instanceof ForbiddenException) return HttpStatus.FORBIDDEN;
		if (exception instanceof NotFoundException) return HttpStatus.NOT_FOUND;
		if (exception instanceof TooManyRequestsException) return HttpStatus.TOO_MANY_REQUESTS;
		if (exception instanceof InternalServerErrorException) return HttpStatus.INTERNAL_SERVER_ERROR;
		if (exception instanceof BadGatewayException) return HttpStatus.BAD_GATEWAY;
		if (exception instanceof ServiceUnavailableException) return HttpStatus.SERVICE_UNAVAILABLE;
		return null;
	}

	private List<Throwable> causeChain(Throwable exception) {
		List<Throwable> chain = new ArrayList<>();
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		Throwable current = exception;
		while (current != null && seen.add(current) && chain.size() < 12) {
			chain.add(current);
			current = current.getCause();
		}
		return chain;
	}

	private void logDiagnostic(String operation, Throwable exception) {
		StringBuilder diagnostic = new StringBuilder("Spotify operation=").append(operation);
		for (Throwable cause : causeChain(exception)) {
			diagnostic.append(" | exception=").append(cause.getClass().getName())
					.append(" message=").append(redact(cause.getMessage()));
			Integer status = statusFromAccessor(cause);
			if (status == null) status = statusFromMessage(cause.getMessage());
			if (status != null) diagnostic.append(" httpStatus=").append(status);
			else if (cause instanceof SpotifyWebApiException) {
				HttpStatus sdkStatus = spotifySdkStatus(cause);
				diagnostic.append(" httpStatus=").append(sdkStatus == null ? "unavailable" : sdkStatus.value());
			}
		}
		log.error("{}", diagnostic);
	}

	private String redact(String value) {
		if (value == null) return "<no message>";
		return value
				.replaceAll("(?i)(authorization\\s*:\\s*bearer\\s+)[^\\s,;\\\"']+", "$1[REDACTED]")
				.replaceAll("(?i)([\\\"']?(?:access_token|refresh_token|client_secret|client_id|authorization_code|code|token)[\\\"']?\\s*[:=]\\s*[\\\"']?)[^\\\"',&\\s}]+", "$1[REDACTED]");
	}

	private String spotifyMessage(HttpStatus status) {
		return switch (status.value()) {
			case 400 -> "La solicitud a Spotify no es válida.";
			case 401 -> "Tu autorización de Spotify expiró. Vuelve a conectar tu cuenta.";
			case 403 -> "Esta cuenta de Spotify no está autorizada para esta demo.";
			case 404 -> "El recurso de Spotify no fue encontrado.";
			case 429 -> "Spotify está limitando temporalmente las solicitudes. Intenta nuevamente.";
			default -> status.is5xxServerError()
					? "Spotify está teniendo problemas temporalmente. Intenta nuevamente."
					: "No se pudo completar la solicitud a Spotify.";
		};
	}

	@FunctionalInterface
	private interface SpotifyCall<T> {
		T run() throws Exception;
	}
}
