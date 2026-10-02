package com.orbe.cinema.controller;

import com.orbe.cinema.dto.CinemaDtos.LibraryItem;
import com.orbe.cinema.dto.CinemaDtos.LibrarySummary;
import com.orbe.cinema.dto.CinemaDtos.PlanRequest;
import com.orbe.cinema.dto.CinemaDtos.RatingRequest;
import com.orbe.cinema.dto.CinemaDtos.SaveTitleRequest;
import com.orbe.cinema.dto.CinemaDtos.SearchResponse;
import com.orbe.cinema.dto.CinemaDtos.TitleDetail;
import com.orbe.cinema.service.CinemaService;
import com.orbe.cinema.service.CinemaService.SaveOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cinema")
@Validated
public class CinemaController {

	private final CinemaService cinema;

	public CinemaController(CinemaService cinema) {
		this.cinema = cinema;
	}

	@GetMapping("/search")
	public SearchResponse search(
			@RequestParam @NotBlank(message = "Escribe un título para buscar.") @Size(max = 120, message = "La búsqueda es demasiado larga.") String q,
			@RequestParam(defaultValue = "1") @Min(value = 1, message = "La página empieza en 1.") @Max(value = 100, message = "La página máxima es 100.") int page
	) {
		return cinema.search(q, page);
	}

	@GetMapping("/titles/{imdbId}")
	public TitleDetail details(
			@PathVariable @Pattern(regexp = "tt\\d+", message = "El identificador de IMDb no es válido.") String imdbId
	) {
		return cinema.details(imdbId);
	}

	@GetMapping("/library")
	public LibrarySummary library(@RequestParam(defaultValue = "name") String sort) {
		return cinema.library(sort);
	}

	@PostMapping("/library")
	public ResponseEntity<LibraryItem> save(@Valid @RequestBody SaveTitleRequest request) {
		SaveOutcome outcome = cinema.save(request.imdbId());
		HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(outcome.item());
	}

	@PostMapping("/library/{id}/ratings")
	public LibraryItem rate(@PathVariable Long id, @Valid @RequestBody RatingRequest request) {
		return cinema.rate(id, request.score(), request.opinion());
	}

	@PatchMapping("/library/{id}")
	public LibraryItem updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
		return cinema.updatePlan(id, request.includedInPlan());
	}

	@DeleteMapping("/library/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		cinema.delete(id);
		return ResponseEntity.noContent().build();
	}
}
