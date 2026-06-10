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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@Validated
@Tag(name = "Crawler", description = "Crawl management")
public class CrawlerController {

    private static final int START_REQUESTS_PER_MINUTE = 5;
    private static final int CLEAR_REQUESTS_PER_MINUTE = 5;

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

    @PostMapping("/clear")
    @Operation(
        summary = "Очистить очередь обхода",
        description = "Удаляет URL из очереди краулинга. По умолчанию (onlyPending=true) удаляются "
            + "только PENDING и FAILED — выполняющиеся (PROCESSING) и завершённые (DONE) сохраняются. "
            + "При onlyPending=false очередь очищается полностью и сбрасывается множество "
            + "посещённых URL. Краулер должен быть остановлен, иначе возвращается 409 Conflict."
    )
    public ResponseEntity<Map<String, Object>> clearQueue(
            @RequestParam(value = "onlyPending", defaultValue = "true") boolean onlyPending,
            HttpServletRequest httpRequest) {
        String clientId = rateLimiter.resolveClientId(httpRequest);
        if (!rateLimiter.tryConsume("crawl-clear", clientId, CLEAR_REQUESTS_PER_MINUTE)) {
            throw new RateLimitExceededException("Crawl API rate limit exceeded. Try again later.");
        }
        if (crawlerService.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", "Stop the crawler before clearing the queue"));
        }

        int cleared = crawlQueueService.clearQueue(onlyPending);
        if (!onlyPending) {
            stateStore.reset();
        }

        return ResponseEntity.ok(Map.of(
            "cleared", cleared,
            "status", status()));
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
