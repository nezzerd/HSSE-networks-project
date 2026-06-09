package com.searchengine.crawler;

import com.searchengine.service.PageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlStateInitializer {

    private static final int BATCH_SIZE = 5000;

    private final PageService pageService;
    private final CrawlStateStore stateStore;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void restoreVisitedHashes() {
        int limit = stateStore.getMaxVisited();
        int loaded = 0;
        int page = 0;

        while (loaded < limit) {
            List<String> hashes = pageService.findUrlHashesNewestFirst(PageRequest.of(page++, BATCH_SIZE));
            if (hashes.isEmpty()) {
                break;
            }
            for (String hash : hashes) {
                if (loaded >= limit) {
                    break;
                }
                stateStore.markVisited(hash);
                loaded++;
            }
            if (hashes.size() < BATCH_SIZE) {
                break;
            }
        }

        if (loaded > 0) {
            log.info("Restored {} visited URL hashes into crawl state from the database", loaded);
        }
    }
}
