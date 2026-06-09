package com.searchengine.controller;

import com.searchengine.crawler.CrawlStateStore;
import com.searchengine.crawler.CrawlerService;
import com.searchengine.dto.CrawlRequest;
import com.searchengine.dto.CrawlStatusResponse;
import com.searchengine.entity.CrawlQueue;
import com.searchengine.exception.RateLimitExceededException;
import com.searchengine.service.CrawlQueueService;
import com.searchengine.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@Validated
@Tag(name = "Crawler", description = "Crawl management")
public class CrawlerController {

    private static final int START_REQUESTS_PER_MINUTE = 5;

    private final CrawlerService crawlerService;
    private final CrawlStateStore stateStore;
    private final CrawlQueueService crawlQueueService;
    private final RateLimiterService rateLimiter;

    @PostMapping("/start")
    @Operation(
        summary = "Запустить краулер",
        description = "Добавляет переданные URL в очередь и запускает фоновый обход. "
            + "Невалидные и приватные адреса отклоняются. Возвращает текущий статус."
    )
    public ResponseEntity<CrawlStatusResponse> start(@Valid @RequestBody CrawlRequest request,
                                                     HttpServletRequest httpRequest) {
        String clientId = rateLimiter.resolveClientId(httpRequest);
        if (!rateLimiter.tryConsume("crawl-start", clientId, START_REQUESTS_PER_MINUTE)) {
            throw new RateLimitExceededException("Crawl API rate limit exceeded. Try again later.");
        }
        crawlerService.seed(request.seedUrls());
        crawlerService.startAsync();
        return ResponseEntity.accepted().body(status());
    }

    @PostMapping("/stop")
    @Operation(summary = "Остановить краулер",
        description = "Подаёт сигнал остановки. Текущие задачи завершатся, новые не запустятся.")
    public ResponseEntity<CrawlStatusResponse> stop() {
        crawlerService.stop();
        return ResponseEntity.ok(status());
    }

    @GetMapping("/status")
    @Operation(summary = "Статус краулера",
        description = "Возвращает признак работы и статистику по очереди обхода.")
    public ResponseEntity<CrawlStatusResponse> getStatus() {
        return ResponseEntity.ok(status());
    }

    private CrawlStatusResponse status() {
        return new CrawlStatusResponse(
            crawlerService.isRunning(),
            stateStore.getPageCount(),
            crawlQueueService.countByStatus(CrawlQueue.QueueStatus.PENDING),
            crawlQueueService.countByStatus(CrawlQueue.QueueStatus.DONE)
        );
    }
}
