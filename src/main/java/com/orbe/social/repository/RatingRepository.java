package com.orbe.social.repository;

import com.orbe.auth.model.UserAccount;
import com.orbe.social.model.CatalogEntry;
import com.orbe.social.model.Rating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

	Optional<Rating> findByUserAndCatalog(UserAccount user, CatalogEntry catalog);

	List<Rating> findByCatalogOrderByUpdatedAtDesc(CatalogEntry catalog);

	List<Rating> findByUserOrderByUpdatedAtDesc(UserAccount user);

	@Query("select avg(r.score) as average, count(r.id) as votes from Rating r where r.catalog.id = :catalogId")
	RatingSummary summarize(@Param("catalogId") Long catalogId);

	@Query("""
			select r from Rating r
			join fetch r.user
			join fetch r.catalog
			where r.opinion is not null and length(trim(r.opinion)) > 0
			order by r.updatedAt desc
			""")
	List<Rating> recentOpinions(Pageable pageable);

	@Query("""
			select r.catalog.id, avg(r.score), count(r.id)
			from Rating r
			group by r.catalog.id
			order by avg(r.score) desc, count(r.id) desc
			""")
	List<Object[]> topRated(Pageable pageable);

	@Query("""
			select r.catalog.id, count(r.id)
			from Rating r
			group by r.catalog.id
			order by count(r.id) desc, avg(r.score) desc
			""")
	List<Object[]> popular(Pageable pageable);

	@Query("""
			select r.catalog.id, count(r.id)
			from Rating r
			where r.updatedAt > :since
			group by r.catalog.id
			order by count(r.id) desc
			""")
	List<Object[]> trending(@Param("since") Instant since, Pageable pageable);

	interface RatingSummary {
		Double getAverage();

		long getVotes();
	}
}
