package com.searchengine.repository;

import com.searchengine.entity.CrawlQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlQueueRepository extends JpaRepository<CrawlQueue, Long> {

    boolean existsByUrlHash(String urlHash);

    Optional<CrawlQueue> findByUrlHash(String urlHash);

    long countByStatus(CrawlQueue.QueueStatus status);

    @Query("""
        SELECT q FROM CrawlQueue q
        WHERE q.status = com.searchengine.entity.CrawlQueue.QueueStatus.PENDING
        ORDER BY q.priority DESC, q.depth ASC, q.createdAt ASC
        """)
    List<CrawlQueue> findPendingBatch(Pageable pageable);

    @Modifying
    @Query("UPDATE CrawlQueue q SET q.status = :status WHERE q.id = :id")
    void updateStatusById(@Param("id") Long id, @Param("status") CrawlQueue.QueueStatus status);

    @Modifying
    @Query("""
        UPDATE CrawlQueue q SET q.status = com.searchengine.entity.CrawlQueue.QueueStatus.PROCESSING
        WHERE q.id IN :ids
        """)
    void markProcessing(@Param("ids") List<Long> ids);

    @Modifying
    @Query("""
        UPDATE CrawlQueue q
        SET q.status = com.searchengine.entity.CrawlQueue.QueueStatus.PENDING,
            q.depth = 0,
            q.priority = :priority
        WHERE q.urlHash = :urlHash
        """)
    int resetToPendingByUrlHash(@Param("urlHash") String urlHash, @Param("priority") int priority);

    @Modifying
    @Query("DELETE FROM CrawlQueue q WHERE q.status IN :statuses")
    int deleteByStatusIn(@Param("statuses") Collection<CrawlQueue.QueueStatus> statuses);

    @Modifying
    @Query(value = """
        INSERT INTO crawl_queue (url, url_hash, depth, priority, status, created_at)
        VALUES (:url, :urlHash, :depth, :priority, 'PENDING', CURRENT_TIMESTAMP)
        ON CONFLICT (url_hash) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("url") String url, @Param("urlHash") String urlHash,
                       @Param("depth") int depth, @Param("priority") int priority);
}
