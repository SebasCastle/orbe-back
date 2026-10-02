package com.orbe.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<?> api(ApiException exception, HttpServletRequest request) {
		if (isSpotifyRequest(request)) {
			logSpotifyDiagnostic("ApiException", exception);
		} else if (exception.status().is5xxServerError()) {
			log.warn("API {}: {}", exception.status().value(), exception.getMessage());
		}
		return errorResponse(request, exception.status(), exception.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<?> conflict(DataIntegrityViolationException exception, HttpServletRequest request) {
		return errorResponse(request, HttpStatus.CONFLICT, "Esa acción ya estaba registrada.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> invalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage() == null ? "Datos inválidos." : error.getDefaultMessage())
				.findFirst()
				.orElse("Datos inválidos.");
		return errorResponse(request, HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<?> invalidParam(ConstraintViolationException exception, HttpServletRequest request) {
		String message = exception.getConstraintViolations().stream()
				.map(violation -> violation.getMessage())
				.findFirst()
				.orElse("Datos inválidos.");
		return errorResponse(request, HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<?> missing(MissingServletRequestParameterException exception, HttpServletRequest request) {
		return errorResponse(request, HttpStatus.BAD_REQUEST,
				"Falta el dato " + exception.getParameterName() + ".");
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<?> unreadable(Exception exception, HttpServletRequest request) {
		return errorResponse(request, HttpStatus.BAD_REQUEST,
				"El formato de los datos no es válido.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> unexpected(Exception exception, HttpServletRequest request) {
		if (isSpotifyRequest(request)) {
			logSpotifyDiagnostic("Spotify exception", exception);
			return errorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR,
					"Spotify está teniendo problemas temporalmente. Intenta nuevamente.");
		}
		log.error("Error inesperado en {}", request.getRequestURI(), exception);
		return ResponseEntity.internalServerError().body(new ErrorResponse("Ocurrió un error inesperado.", 500));
	}

	private ResponseEntity<?> errorResponse(
			HttpServletRequest request,
			HttpStatus status,
			String message
	) {
		if (!isSpotifyRequest(request)) {
			return ResponseEntity.status(status).body(new ErrorResponse(message, status.value()));
		}
		return ResponseEntity.status(status).body(new SpotifyErrorResponse(
				status.value(),
				spotifyCode(status),
				spotifyMessage(status, message)
		));
	}

	private boolean isSpotifyRequest(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/api/spotify/")
				|| path.equals("/api/login")
				|| path.equals("/api/get-user-code")
				|| path.equals("/api/home")
				|| path.equals("/api/user-saved-album")
				|| path.equals("/api/user-top-songs");
	}

	private String spotifyCode(HttpStatus status) {
		return switch (status.value()) {
			case 400 -> "SPOTIFY_BAD_REQUEST";
			case 401 -> "SPOTIFY_UNAUTHORIZED";
			case 403 -> "SPOTIFY_FORBIDDEN";
			case 404 -> "SPOTIFY_NOT_FOUND";
			case 429 -> "SPOTIFY_RATE_LIMITED";
			default -> status.is5xxServerError() ? "SPOTIFY_TEMPORARY_ERROR" : "SPOTIFY_ERROR";
		};
	}

	private String spotifyMessage(HttpStatus status, String fallback) {
		return switch (status.value()) {
			case 400 -> "La solicitud a Spotify no es válida.";
			case 401 -> "Tu autorización de Spotify expiró. Vuelve a conectar tu cuenta.";
			case 403 -> "Esta cuenta de Spotify no está autorizada para esta demo.";
			case 404 -> "El recurso de Spotify no fue encontrado.";
			case 429 -> "Spotify está limitando temporalmente las solicitudes. Intenta nuevamente.";
			default -> status.is5xxServerError()
					? "Spotify está teniendo problemas temporalmente. Intenta nuevamente."
					: fallback;
		};
	}

	private void logSpotifyDiagnostic(String label, Throwable exception) {
		StringBuilder diagnostic = new StringBuilder(label);
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		Throwable current = exception;
		int depth = 0;
		while (current != null && seen.add(current) && depth++ < 12) {
			diagnostic.append(" | exception=").append(current.getClass().getName())
					.append(" message=").append(redact(current.getMessage()));
			if (current instanceof ApiException apiException) {
				diagnostic.append(" httpStatus=").append(apiException.status().value());
			}
			current = current.getCause();
		}
		log.error("Spotify diagnostic: {}", diagnostic);
	}

	private String redact(String value) {
		if (value == null) return "<no message>";
		return value
				.replaceAll("(?i)(authorization\\s*:\\s*bearer\\s+)[^\\s,;\\\"']+", "$1[REDACTED]")
				.replaceAll("(?i)([\\\"']?(?:access_token|refresh_token|client_secret|client_id|authorization_code|code|token)[\\\"']?\\s*[:=]\\s*[\\\"']?)[^\\\"',&\\s}]+", "$1[REDACTED]");
	}

	private record SpotifyErrorResponse(int status, String code, String message) {
	}
}
