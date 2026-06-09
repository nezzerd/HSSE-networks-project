package com.searchengine.service;

import com.searchengine.entity.CrawlQueue;
import com.searchengine.entity.Page;
import com.searchengine.repository.CrawlQueueRepository;
import com.searchengine.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlQueueService {

    private final CrawlQueueRepository crawlQueueRepository;
    private final PageRepository pageRepository;

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
    public void enqueueIfAbsent(String url, String urlHash, int depth) {
        if (crawlQueueRepository.existsByUrlHash(urlHash)) {
            return;
        }
        crawlQueueRepository.save(CrawlQueue.builder()
            .url(url).urlHash(urlHash).depth(depth)
            .status(CrawlQueue.QueueStatus.PENDING).build());
    }

    @Transactional
    public void requeueForRecrawl(String url, String urlHash) {
        int reset = crawlQueueRepository.resetToPendingByUrlHash(urlHash);
        if (reset == 0) {
            enqueueIfAbsent(url, urlHash, 0);
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
