package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.service.CrawlQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {

    @Mock CrawlWorker worker;
    @Mock CrawlQueueService crawlQueueService;

    CrawlerService service;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        service = new CrawlerService(
            worker, new CrawlStateStore(), new CrawlerProperties(),
            crawlQueueService, directExecutor
        );
    }

    @Test
    void seed_rejectsInvalidUrls() {
        service.seed(List.of("not-a-url", "javascript:alert(1)"));
        verify(crawlQueueService, never()).enqueueIfAbsent(anyString(), anyString(), anyInt());
    }

    @Test
    void seed_rejectsPrivateIp() {
        service.seed(List.of("http://192.168.1.1/page"));
        verify(crawlQueueService, never()).enqueueIfAbsent(anyString(), anyString(), anyInt());
    }

    @Test
    void seed_acceptsValidPublicUrl() {
        service.seed(List.of("https://example.com/page"));
        verify(crawlQueueService).enqueueIfAbsent(anyString(), anyString(), anyInt());
    }

    @Test
    void isRunning_falseByDefault() {
        assertThat(service.isRunning()).isFalse();
    }
}
