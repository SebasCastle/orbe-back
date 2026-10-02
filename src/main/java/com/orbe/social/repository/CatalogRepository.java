package com.orbe.social.repository;

import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.ContentKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository extends JpaRepository<CatalogEntry, Long> {

	Optional<CatalogEntry> findByKindAndExternalId(ContentKind kind, String externalId);

	List<CatalogEntry> findByKindOrderByTitleAsc(ContentKind kind, Pageable pageable);

	List<CatalogEntry> findByKindAndCategoryIgnoreCaseOrderByTitleAsc(ContentKind kind, String category, Pageable pageable);

	@Query("""
			select c from CatalogEntry c
			where (:kind is null or c.kind = :kind)
			and (:category is null or lower(c.category) = lower(:category))
			and (:q is null or lower(c.title) like lower(concat('%', :q, '%')) or lower(coalesce(c.creator, '')) like lower(concat('%', :q, '%')))
			order by c.title asc
			""")
	List<CatalogEntry> search(@Param("kind") ContentKind kind, @Param("q") String q, @Param("category") String category, Pageable pageable);

	List<CatalogEntry> findByIdIn(List<Long> ids);
}
