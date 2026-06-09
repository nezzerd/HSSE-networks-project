package com.searchengine.controller;

import com.searchengine.exception.RateLimitExceededException;
import com.searchengine.service.IndexingService;
import com.searchengine.service.RateLimiterService;
import com.searchengine.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
@Tag(name = "Index", description = "Index management")
public class IndexController {

    private static final int REINDEX_REQUESTS_PER_MINUTE = 3;

    private final IndexingService indexingService;
    private final SearchService searchService;
    private final RateLimiterService rateLimiter;

    @PostMapping("/reindex")
    @Operation(summary = "Полная переиндексация",
        description = "Перестраивает индекс Lucene по всем успешно скачанным страницам из базы. "
            + "Выполняется в фоне.")
    public ResponseEntity<Map<String, Object>> reindex(HttpServletRequest request) {
        String clientId = rateLimiter.resolveClientId(request);
        if (!rateLimiter.tryConsume("reindex", clientId, REINDEX_REQUESTS_PER_MINUTE)) {
            throw new RateLimitExceededException("Reindex rate limit exceeded. Try again later.");
        }
        indexingService.reindexAllAsync();
        return ResponseEntity.accepted().body(Map.of(
            "running", indexingService.isRunning(),
            "message", "Reindex started"
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "Статус индекса",
        description = "Возвращает признак выполнения индексации и число документов в индексе.")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
            "running",      indexingService.isRunning(),
            "indexedDocs",  searchService.countIndexedDocs()
        ));
    }
}
