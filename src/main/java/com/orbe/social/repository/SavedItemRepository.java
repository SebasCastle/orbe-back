package com.orbe.social.repository;

import com.orbe.auth.model.UserAccount;
import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.ContentKind;
import com.orbe.social.model.SavedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedItemRepository extends JpaRepository<SavedItem, Long> {

	Optional<SavedItem> findByUserAndCatalog(UserAccount user, CatalogEntry catalog);

	Optional<SavedItem> findByIdAndUser(Long id, UserAccount user);

	List<SavedItem> findByUserAndCatalogKind(UserAccount user, ContentKind kind);

	List<SavedItem> findByUser(UserAccount user);

	boolean existsByUserAndCatalog(UserAccount user, CatalogEntry catalog);
}
