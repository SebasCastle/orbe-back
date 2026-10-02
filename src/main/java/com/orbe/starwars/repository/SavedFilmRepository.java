package com.orbe.starwars.repository;

import com.orbe.starwars.model.SavedFilm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedFilmRepository extends JpaRepository<SavedFilm, Integer> {
}
