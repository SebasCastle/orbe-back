package com.orbe.social.repository;

import com.orbe.auth.model.UserAccount;
import com.orbe.social.model.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {

	List<Shelf> findByUserOrderByCreatedAtAsc(UserAccount user);

	Optional<Shelf> findByIdAndUser(Long id, UserAccount user);

	boolean existsByUserAndNameIgnoreCase(UserAccount user, String name);
}
