package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.entity.Page;
import com.searchengine.service.CrawlQueueService;
import com.searchengine.service.PageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecrawlScheduler {

    private final PageService pageService;
    private final CrawlQueueService crawlQueueService;
    private final CrawlStateStore stateStore;
    private final CrawlerProperties props;

    @Scheduled(cron = "${crawler.recrawl-cron:0 0 3 * * *}")
    public void requeueStalePages() {
        Instant threshold = Instant.now().minus(props.getRecrawlAfterDays(), ChronoUnit.DAYS);
        List<Page> stale = pageService.findStaleFetchedPages(threshold, props.getRecrawlBatchSize());
        if (stale.isEmpty()) {
            return;
        }

        for (Page page : stale) {
            stateStore.unmarkVisited(page.getUrlHash());
            crawlQueueService.requeueForRecrawl(page.getUrl(), page.getUrlHash());
        }

        log.info("Re-queued {} stale page(s) for recrawl (fetched before {}, older than {} days)",
            stale.size(), threshold, props.getRecrawlAfterDays());
    }
}
