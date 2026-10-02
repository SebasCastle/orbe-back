package com.orbe.starwars.swapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.orbe.common.ApiException;
import com.orbe.common.ExternalHttp;
import com.orbe.common.Texts;
import com.orbe.config.OrbeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SwapiClient {

	private static final Logger log = LoggerFactory.getLogger(SwapiClient.class);

	private final OrbeProperties properties;
	private final ExternalHttp http;
	private final ReentrantLock lock = new ReentrantLock();
	private volatile Cache cache;

	public SwapiClient(OrbeProperties properties, ExternalHttp http) {
		this.properties = properties;
		this.http = http;
	}

	public List<FilmSnapshot> list() {
		Cache current = cache;
		if (fresh(current)) {
			return current.films();
		}
		lock.lock();
		try {
			current = cache;
			if (fresh(current)) {
				return current.films();
			}
			List<FilmSnapshot> films = fetchList();
			cache = new Cache(Instant.now(), List.copyOf(films));
			return cache.films();
		} finally {
			lock.unlock();
		}
	}

	public FilmSnapshot get(int id) {
		try {
			return list().stream()
					.filter(film -> film.id() == id)
					.findFirst()
					.orElseGet(() -> fetchOne(id));
		} catch (ApiException exception) {
			log.warn("El listado de Star Wars falló, intento una consulta directa");
			return fetchOne(id);
		}
	}

	private boolean fresh(Cache current) {
		return current != null && current.at().isAfter(Instant.now().minus(Duration.ofHours(6)));
	}

	private List<FilmSnapshot> fetchList() {
		ApiException last = null;
		for (String base : bases()) {
			for (String path : List.of("/films/", "/films")) {
				try {
					JsonNode root = http.getTree(join(base, path));
					List<FilmSnapshot> films = SwapiParser.parseList(root);
					if (!films.isEmpty()) {
						return films;
					}
				} catch (ApiException exception) {
					last = exception;
					log.warn("SWAPI no respondió en {}: {}", base, exception.getMessage());
				}
			}
		}
		if (last != null && last.status() == HttpStatus.NOT_FOUND) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "No pude consultar el archivo de Star Wars.");
		}
		throw last == null
				? new ApiException(HttpStatus.BAD_GATEWAY, "No pude consultar el archivo de Star Wars.")
				: last;
	}

	private FilmSnapshot fetchOne(int id) {
		ApiException last = null;
		for (String base : bases()) {
			for (String path : List.of("/films/" + id + "/", "/films/" + id)) {
				try {
					JsonNode root = http.getTree(join(base, path));
					FilmSnapshot film = SwapiParser.parseOne(root, id);
					if (film != null) {
						return film;
					}
				} catch (ApiException exception) {
					last = exception;
				}
			}
		}
		if (last != null && last.status() == HttpStatus.NOT_FOUND) {
			throw new ApiException(HttpStatus.NOT_FOUND, "No encontré esa película de Star Wars.");
		}
		throw new ApiException(HttpStatus.NOT_FOUND, "No encontré esa película de Star Wars.");
	}

	private List<String> bases() {
		LinkedHashSet<String> urls = new LinkedHashSet<>();
		if (properties.swapi() != null && !Texts.blank(properties.swapi().baseUrl())) {
			urls.add(trim(properties.swapi().baseUrl()));
		}
		urls.add("https://swapi.py4e.com/api");
		urls.add("https://swapi.dev/api");
		urls.add("https://swapi.info/api");
		urls.add("https://www.swapi.tech/api");
		return new ArrayList<>(urls);
	}

	private String join(String base, String path) {
		String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
		return normalized + path;
	}

	private String trim(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private record Cache(Instant at, List<FilmSnapshot> films) {
	}
}
