package com.searchengine.service;

import com.searchengine.entity.Page;
import com.searchengine.indexer.LuceneIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private static final int BATCH_SIZE = 200;
    private static final int COMMIT_THRESHOLD = 50;

    private final PageService pageService;
    private final LuceneIndexer luceneIndexer;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger uncommitted = new AtomicInteger(0);

    @Async("indexerExecutor")
    public void reindexAllAsync() {
        if (!running.compareAndSet(false, true)) {
            log.info("Indexing already in progress");
            return;
        }

        log.info("Full reindex started");
        int page = 0;
        long total = 0;

        try {
            List<Page> batch;
            do {
                batch = pageService.findFetchedPages(PageRequest.of(page++, BATCH_SIZE));
                if (!batch.isEmpty()) {
                    luceneIndexer.indexPages(batch);
                    total += batch.size();
                    log.debug("Indexed batch, total so far: {}", total);
                }
            } while (batch.size() == BATCH_SIZE);

            luceneIndexer.commit();
            uncommitted.set(0);
            log.info("Full reindex complete. Total pages indexed: {}", total);

        } catch (IOException e) {
            log.error("Indexing failed", e);
        } finally {
            running.set(false);
        }
    }

    public void indexPage(Page page) {
        try {
            luceneIndexer.indexPage(page);
            if (uncommitted.incrementAndGet() >= COMMIT_THRESHOLD) {
                flush();
            }
        } catch (IOException e) {
            log.error("Failed to index page id={} url={}", page.getId(), page.getUrl(), e);
        }
    }

    @Scheduled(fixedDelay = 10_000)
    public void flushScheduled() {
        if (uncommitted.get() > 0) {
            flush();
        }
    }

    private synchronized void flush() {
        try {
            luceneIndexer.commit();
            uncommitted.set(0);
        } catch (IOException e) {
            log.error("Index commit failed", e);
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
