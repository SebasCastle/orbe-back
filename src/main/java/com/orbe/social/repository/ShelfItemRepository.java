package com.orbe.social.repository;

import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.Shelf;
import com.orbe.social.model.ShelfItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfItemRepository extends JpaRepository<ShelfItem, Long> {

	List<ShelfItem> findByShelfOrderByAddedAtAsc(Shelf shelf);

	Optional<ShelfItem> findByShelfAndCatalog(Shelf shelf, CatalogEntry catalog);

	void deleteByShelf(Shelf shelf);
}
