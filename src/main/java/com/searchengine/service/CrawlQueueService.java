package com.searchengine.service;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.entity.CrawlQueue;
import com.searchengine.entity.Page;
import com.searchengine.repository.CrawlQueueRepository;
import com.searchengine.repository.PageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CrawlQueueService {

    private final CrawlQueueRepository crawlQueueRepository;
    private final PageRepository pageRepository;
    private final CrawlerProperties crawlerProperties;
    private final DataSource dataSource;

    private boolean supportsUpsert;

    @PostConstruct
    void detectUpsertSupport() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            supportsUpsert = product != null && product.toLowerCase().contains("postgresql");
            log.debug("Database product '{}', ON CONFLICT upsert {}",
                product, supportsUpsert ? "enabled" : "disabled");
        } catch (SQLException e) {
            supportsUpsert = false;
            log.warn("Could not detect database product, falling back to check-then-insert enqueue", e);
        }
    }

    public static int priorityForDepth(int depth, int maxDepth) {
        return Math.max(0, maxDepth - depth);
    }

    public long countByStatus(CrawlQueue.QueueStatus status) {
        return crawlQueueRepository.countByStatus(status);
    }

    public long totalCount() {
        return crawlQueueRepository.count();
    }

    @Transactional
    public List<CrawlQueue> claimPendingBatch(int limit) {
        List<CrawlQueue> batch = crawlQueueRepository.findPendingBatch(PageRequest.of(0, limit));
        if (!batch.isEmpty()) {
            List<Long> ids = batch.stream().map(CrawlQueue::getId).toList();
            crawlQueueRepository.markProcessing(ids);
        }
        return batch;
    }

    @Transactional
    public void updateStatus(Long id, CrawlQueue.QueueStatus status) {
        crawlQueueRepository.updateStatusById(id, status);
    }

    @Transactional
    public void enqueueIfAbsent(String url, String urlHash, int depth, int priority) {
        if (supportsUpsert) {
            crawlQueueRepository.insertIfAbsent(url, urlHash, depth, priority);
            return;
        }
        if (crawlQueueRepository.existsByUrlHash(urlHash)) {
            return;
        }
        crawlQueueRepository.save(CrawlQueue.builder()
            .url(url).urlHash(urlHash).depth(depth).priority(priority)
            .status(CrawlQueue.QueueStatus.PENDING).build());
    }

    @Transactional
    public int clearQueue(boolean onlyPending) {
        Collection<CrawlQueue.QueueStatus> statuses = onlyPending
            ? List.of(CrawlQueue.QueueStatus.PENDING, CrawlQueue.QueueStatus.FAILED)
            : Arrays.asList(CrawlQueue.QueueStatus.values());
        return crawlQueueRepository.deleteByStatusIn(statuses);
    }

    @Transactional
    public void requeueForRecrawl(String url, String urlHash) {
        int priority = priorityForDepth(0, crawlerProperties.getMaxDepth());
        int reset = crawlQueueRepository.resetToPendingByUrlHash(urlHash, priority);
        if (reset == 0) {
            enqueueIfAbsent(url, urlHash, 0, priority);
        }
    }

    @Transactional
    public void persistError(String url, String urlHash, String reason) {
        if (pageRepository.existsByUrlHash(urlHash)) {
            return;
        }
        pageRepository.save(Page.builder()
            .url(url).urlHash(urlHash)
            .status(Page.PageStatus.ERROR).content(reason).build());
    }
}
