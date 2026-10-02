package com.orbe.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> api(ApiException exception) {
		if (exception.status().is5xxServerError()) {
			log.warn("API {}: {}", exception.status().value(), exception.getMessage());
		}
		return ResponseEntity.status(exception.status())
				.body(new ErrorResponse(exception.getMessage(), exception.status().value()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> conflict(DataIntegrityViolationException exception) {
		return ResponseEntity.status(409).body(new ErrorResponse("Esa acción ya estaba registrada.", 409));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage() == null ? "Datos inválidos." : error.getDefaultMessage())
				.findFirst()
				.orElse("Datos inválidos.");
		return ResponseEntity.badRequest().body(new ErrorResponse(message, 400));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> invalidParam(ConstraintViolationException exception) {
		String message = exception.getConstraintViolations().stream()
				.map(violation -> violation.getMessage())
				.findFirst()
				.orElse("Datos inválidos.");
		return ResponseEntity.badRequest().body(new ErrorResponse(message, 400));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> missing(MissingServletRequestParameterException exception) {
		return ResponseEntity.badRequest()
				.body(new ErrorResponse("Falta el dato " + exception.getParameterName() + ".", 400));
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ErrorResponse> unreadable(Exception exception) {
		return ResponseEntity.badRequest().body(new ErrorResponse("El formato de los datos no es válido.", 400));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
		log.error("Error inesperado en {}", request.getRequestURI(), exception);
		return ResponseEntity.internalServerError().body(new ErrorResponse("Ocurrió un error inesperado.", 500));
	}
}
