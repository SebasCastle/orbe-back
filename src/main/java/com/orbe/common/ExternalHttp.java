package com.orbe.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ExternalHttp {

	private static final Logger log = LoggerFactory.getLogger(ExternalHttp.class);

	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private final ObjectMapper mapper;

	public ExternalHttp(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public String getBody(String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(12))
				.header("Accept", "application/json")
				.header("User-Agent", "OrbePortfolio/1.0")
				.GET()
				.build();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (Exception exception) {
			log.warn("No se pudo contactar {}", host(url));
			throw new ApiException(HttpStatus.BAD_GATEWAY, "No pude contactar un servicio externo.");
		}
		if (response.statusCode() == 404) {
			throw new ApiException(HttpStatus.NOT_FOUND, "No encontré ese recurso.");
		}
		if (response.statusCode() >= 400) {
			log.warn("El servicio {} respondió {}", host(url), response.statusCode());
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Un servicio externo respondió con error.");
		}
		String body = response.body() == null ? "" : response.body().trim();
		if (body.isEmpty() || !(body.startsWith("{") || body.startsWith("["))) {
			log.warn("Respuesta inesperada de {}", host(url));
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Un servicio externo devolvió una respuesta inesperada.");
		}
		return body;
	}

	public <T> T get(String url, Class<T> type) {
		try {
			return mapper.readValue(getBody(url), type);
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "No pude leer la respuesta de un servicio externo.");
		}
	}

	public JsonNode getTree(String url) {
		try {
			return mapper.readTree(getBody(url));
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "No pude leer la respuesta de un servicio externo.");
		}
	}

	private String host(String url) {
		try {
			return URI.create(url).getHost();
		} catch (Exception exception) {
			return "externo";
		}
	}
}
