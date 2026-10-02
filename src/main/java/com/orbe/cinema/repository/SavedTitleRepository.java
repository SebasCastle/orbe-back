package com.orbe.cinema.repository;

import com.orbe.cinema.model.SavedTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavedTitleRepository extends JpaRepository<SavedTitle, Long> {

	Optional<SavedTitle> findByImdbId(String imdbId);
}
