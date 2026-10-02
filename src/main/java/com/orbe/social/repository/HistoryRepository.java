package com.orbe.social.repository;

import com.orbe.auth.model.UserAccount;
import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.HistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryRepository extends JpaRepository<HistoryEntry, Long> {

	Optional<HistoryEntry> findByUserAndCatalog(UserAccount user, CatalogEntry catalog);

	List<HistoryEntry> findTop30ByUserOrderByViewedAtDesc(UserAccount user);
}
