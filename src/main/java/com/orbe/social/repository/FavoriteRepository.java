package com.orbe.social.repository;

import com.orbe.auth.model.UserAccount;
import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.ContentKind;
import com.orbe.social.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

	Optional<Favorite> findByUserAndCatalog(UserAccount user, CatalogEntry catalog);

	List<Favorite> findByUserOrderByCreatedAtDesc(UserAccount user);

	boolean existsByUserAndCatalog(UserAccount user, CatalogEntry catalog);

	long countByCatalog(CatalogEntry catalog);

	List<Favorite> findByUserAndCatalogKindOrderByCreatedAtDesc(UserAccount user, ContentKind kind);
}
