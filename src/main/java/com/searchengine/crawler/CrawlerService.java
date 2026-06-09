package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.entity.CrawlQueue;
import com.searchengine.service.CrawlQueueService;
import com.searchengine.util.HashUtils;
import com.searchengine.util.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class CrawlerService {

    private static final int BATCH_SIZE = 32;

    private final CrawlWorker worker;
    private final CrawlStateStore stateStore;
    private final CrawlerProperties props;
    private final CrawlQueueService crawlQueueService;
    private final Executor crawlerExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public CrawlerService(CrawlWorker worker,
                          CrawlStateStore stateStore,
                          CrawlerProperties props,
                          CrawlQueueService crawlQueueService,
                          @Qualifier("crawlerExecutor") Executor crawlerExecutor) {
        this.worker = worker;
        this.stateStore = stateStore;
        this.props = props;
        this.crawlQueueService = crawlQueueService;
        this.crawlerExecutor = crawlerExecutor;
    }

    public void seed(List<String> seedUrls) {
        for (String raw : seedUrls) {
            String url;
            try {
                url = UrlUtils.normalize(raw);
                UrlUtils.checkSsrf(url);
            } catch (IllegalArgumentException e) {
                log.warn("Rejected seed URL {}: {}", raw, e.getMessage());
                continue;
            }
            String hash = HashUtils.sha256(url);
            if (!stateStore.isVisited(hash)) {
                int priority = CrawlQueueService.priorityForDepth(0, props.getMaxDepth());
                crawlQueueService.enqueueIfAbsent(url, hash, 0, priority);
            }
        }
    }

    @Async("crawlerExecutor")
    public void startAsync() {
        if (!running.compareAndSet(false, true)) {
            log.info("Crawler already running");
            return;
        }
        log.info("Crawler started");
        stateStore.resetSessionCount();
        try {
            runLoop();
        } catch (RuntimeException e) {
            log.error("Crawler loop terminated abnormally", e);
        } finally {
            running.set(false);
            log.info("Crawler stopped. Pages crawled: {}", stateStore.getPageCount());
        }
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    private void runLoop() {
        while (running.get() && stateStore.getPageCount() < props.getMaxPages()) {
            List<CrawlQueue> batch = crawlQueueService.claimPendingBatch(BATCH_SIZE);
            if (batch.isEmpty()) {
                break;
            }

            CompletableFuture<?>[] tasks = batch.stream()
                .map(item -> CompletableFuture.runAsync(() -> safeProcess(item), crawlerExecutor))
                .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(tasks).join();
        }
    }

    private void safeProcess(CrawlQueue item) {
        try {
            worker.process(item);
        } catch (RuntimeException e) {
            log.error("Unhandled error processing {}: {}", item.getUrl(), e.getMessage());
        }
    }
}
