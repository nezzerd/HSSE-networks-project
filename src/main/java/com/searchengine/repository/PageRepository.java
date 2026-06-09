package com.searchengine.repository;

import com.searchengine.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    boolean existsByUrlHash(String urlHash);

    boolean existsByContentHash(String contentHash);

    Optional<Page> findByUrlHash(String urlHash);

    List<Page> findByStatus(Page.PageStatus status, Pageable pageable);

    long countByStatus(Page.PageStatus status);

    @Query("SELECT p.urlHash FROM Page p ORDER BY p.fetchedAt DESC")
    List<String> findUrlHashesOrderByFetchedAtDesc(Pageable pageable);

    @Query("""
        SELECT p.simhash FROM Page p
        WHERE p.simhash IS NOT NULL
          AND p.status = com.searchengine.entity.Page.PageStatus.FETCHED
        ORDER BY p.fetchedAt DESC
        """)
    List<Long> findRecentSimhashes(Pageable pageable);

    List<Page> findByStatusAndFetchedAtBeforeOrderByFetchedAtAsc(
        Page.PageStatus status, Instant threshold, Pageable pageable);
}
