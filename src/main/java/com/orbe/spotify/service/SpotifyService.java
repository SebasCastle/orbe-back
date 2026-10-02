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
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SpotifyService {

	private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);
	private static final String SCOPES = "user-read-private user-read-email user-library-read user-top-read";
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
		URI uri = execute(() -> api.authorizationCodeUri()
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
		AuthorizationCodeCredentials credentials = execute(() -> api.authorizationCode(code).build().execute());
		api.setAccessToken(credentials.getAccessToken());
		if (credentials.getRefreshToken() != null) {
			api.setRefreshToken(credentials.getRefreshToken());
		}
		User user = execute(() -> api.getCurrentUsersProfile().build().execute());
		SpotifyAccount account = accounts.findByRefId(user.getId()).orElseGet(SpotifyAccount::new);
		account.setRefId(user.getId());
		account.setDisplayName(user.getDisplayName());
		account.setEmail(user.getEmail());
		account.setImageUrl(image(user.getImages()));
		account.setProduct(productName(user.getProduct()));
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
		User user = execute(() -> api.getCurrentUsersProfile().build().execute());
		account.setDisplayName(user.getDisplayName());
		account.setEmail(user.getEmail());
		account.setImageUrl(image(user.getImages()));
		account.setProduct(productName(user.getProduct()));
		accounts.save(account);
		return profileOf(account);
	}

	@Transactional
	public AlbumPage albums(String userId, int offset) {
		SpotifyAccount account = requireOwned(userId);
		SpotifyApi api = authorized(account);
		Paging<SavedAlbum> paging = execute(() -> api.getCurrentUsersSavedAlbums()
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
		Paging<Track> paging = execute(() -> api.getUsersTopTracks()
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
					track.getPopularity() == null ? 0 : track.getPopularity(),
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
			AuthorizationCodeCredentials credentials = execute(() -> api.authorizationCodeRefresh().build().execute());
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
				account.getEmail(),
				account.getImageUrl(),
				account.getProduct()
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

	private String productName(Object product) {
		return product == null ? null : product.toString().toLowerCase(java.util.Locale.ROOT);
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

	private <T> T execute(SpotifyCall<T> call) {
		try {
			return call.run();
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			log.warn("Falló una llamada a Spotify: {}", exception.getClass().getSimpleName());
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Spotify no respondió. Vuelve a conectar tu cuenta.");
		}
	}

	@FunctionalInterface
	private interface SpotifyCall<T> {
		T run() throws Exception;
	}
}
