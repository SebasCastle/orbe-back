package com.orbe.starwars.controller;

import com.orbe.starwars.dto.FilmDtos.FilmResponse;
import com.orbe.starwars.service.StarWarsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/starwars/films")
@Validated
public class StarWarsController {

	private final StarWarsService starWars;

	public StarWarsController(StarWarsService starWars) {
		this.starWars = starWars;
	}

	@GetMapping
	public List<FilmResponse> list() {
		return starWars.list();
	}

	@GetMapping("/{id}")
	public FilmResponse get(@PathVariable @Min(value = 1, message = "El número de película no es válido.") @Max(value = 20, message = "El número de película no es válido.") int id) {
		return starWars.get(id);
	}

	@PostMapping("/{id}/library")
	public FilmResponse save(@PathVariable @Min(1) @Max(20) int id) {
		return starWars.save(id);
	}

	@DeleteMapping("/{id}/library")
	public ResponseEntity<Void> delete(@PathVariable @Min(1) @Max(20) int id) {
		starWars.delete(id);
		return ResponseEntity.noContent().build();
	}
}
