package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.entity.CrawlQueue;
import com.searchengine.entity.Page;
import com.searchengine.service.CrawlQueueService;
import com.searchengine.service.IndexingService;
import com.searchengine.service.PageService;
import com.searchengine.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlWorker {

    private static final int MAX_ERROR_REASON_LENGTH = 1000;

    private final PageFetcher fetcher;
    private final RobotsTxtCache robotsTxtCache;
    private final PolitenessThrottle throttle;
    private final CrawlStateStore stateStore;
    private final CrawlerProperties props;
    private final PageService pageService;
    private final CrawlQueueService crawlQueueService;
    private final IndexingService indexingService;

    public void process(CrawlQueue item) {
        if (!stateStore.markVisited(item.getUrlHash())) {
            crawlQueueService.updateStatus(item.getId(), CrawlQueue.QueueStatus.DONE);
            return;
        }
        if (stateStore.getPageCount() >= props.getMaxPages()) {
            crawlQueueService.updateStatus(item.getId(), CrawlQueue.QueueStatus.FAILED);
            return;
        }
        if (!robotsTxtCache.isAllowed(item.getUrl())) {
            log.debug("Blocked by robots.txt: {}", item.getUrl());
            crawlQueueService.updateStatus(item.getId(), CrawlQueue.QueueStatus.DONE);
            return;
        }
        try {
            throttle.waitIfNeeded(item.getUrl());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            crawlQueueService.updateStatus(item.getId(), CrawlQueue.QueueStatus.FAILED);
            return;
        }

        CrawlResult result = fetcher.fetch(item.getUrl());
        int count = stateStore.incrementAndGetPageCount();
        log.info("[{}/{}] {} — {}", count, props.getMaxPages(),
            result.isSuccess() ? "OK" : "FAIL", item.getUrl());

        try {
            if (result.isSuccess()) {
                handleSuccess(item, result);
            } else {
                crawlQueueService.persistError(
                    item.getUrl(), item.getUrlHash(), truncateReason(result.getErrorMessage()));
            }
            crawlQueueService.updateStatus(item.getId(), CrawlQueue.QueueStatus.DONE);
        } catch (RuntimeException e) {
            log.error("Failed to persist crawl result for {}: {}", item.getUrl(), e.getMessage());
            crawlQueueService.updateStatus(item.getId(), CrawlQueue.QueueStatus.FAILED);
        }
    }

    private void handleSuccess(CrawlQueue item, CrawlResult result) {
        Optional<Page> saved;
        try {
            saved = pageService.saveIfNew(
                result.getUrl(),
                HashUtils.sha256(result.getUrl()),
                HashUtils.sha256(result.getText()),
                result.getTitle(),
                result.getText()
            );
        } catch (DataAccessException | TransactionException e) {
            log.trace("Skipping concurrent duplicate page: {}", result.getUrl());
            saved = Optional.empty();
        }

        saved.ifPresent(indexingService::indexPage);

        if (item.getDepth() < props.getMaxDepth()) {
            enqueueLinks(result.getOutboundLinks(), item.getDepth() + 1);
        }
    }

    private void enqueueLinks(Set<String> links, int depth) {
        if (crawlQueueService.totalCount() >= props.getMaxQueueSize()) {
            log.debug("Crawl queue cap reached ({}), not enqueueing more links", props.getMaxQueueSize());
            return;
        }

        int added = 0;
        for (String link : links) {
            if (added >= props.getMaxLinksPerPage()) {
                break;
            }
            String hash = HashUtils.sha256(link);
            if (stateStore.isVisited(hash)) {
                continue;
            }
            try {
                crawlQueueService.enqueueIfAbsent(link, hash, depth);
                added++;
            } catch (DataAccessException | TransactionException e) {
                log.trace("Skipping concurrent duplicate enqueue: {}", link);
            }
        }
    }

    private String truncateReason(String reason) {
        if (reason == null) return null;
        return reason.length() > MAX_ERROR_REASON_LENGTH
            ? reason.substring(0, MAX_ERROR_REASON_LENGTH)
            : reason;
    }
}
