package com.searchengine.controller;

import com.searchengine.config.SearchProperties;
import com.searchengine.dto.SearchResponse;
import com.searchengine.dto.SearchResultDto;
import com.searchengine.exception.RateLimitExceededException;
import com.searchengine.indexer.SearchPage;
import com.searchengine.service.RateLimiterService;
import com.searchengine.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Search", description = "Full-text search over the index")
public class SearchController {

    private static final int MAX_PAGE = 99;
    private static final int REQUESTS_PER_MINUTE = 30;

    private final SearchService searchService;
    private final SearchProperties searchProperties;
    private final RateLimiterService rateLimiter;

    @GetMapping
    @Operation(
        summary = "Поиск по проиндексированным страницам",
        description = "Выполняет полнотекстовый поиск по индексу. Возвращает страницу результатов "
            + "со сниппетами, подсветкой совпадений и постраничной навигацией."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Результаты поиска",
            content = @Content(schema = @Schema(implementation = SearchResponse.class))),
        @ApiResponse(responseCode = "400", description = "Некорректный запрос (пустой или слишком длинный)",
            content = @Content),
        @ApiResponse(responseCode = "429", description = "Превышен лимит запросов",
            content = @Content)
    })
    public ResponseEntity<SearchResponse> search(
        @Parameter(description = "Search query", example = "spring boot")
        @RequestParam("q")
        @NotBlank(message = "Query must not be blank")
        @Size(max = 256, message = "Query too long")
        String q,

        @Parameter(description = "Zero-based page number")
        @RequestParam(value = "page", defaultValue = "0")
        @Min(value = 0, message = "Page must be >= 0")
        @Max(value = MAX_PAGE, message = "Page too large")
        int page,

        HttpServletRequest request
    ) {
        String clientId = rateLimiter.resolveClientId(request);
        if (!rateLimiter.tryConsume("search", clientId, REQUESTS_PER_MINUTE)) {
            throw new RateLimitExceededException("Search rate limit exceeded. Try again later.");
        }

        long start = System.currentTimeMillis();
        SearchPage result = searchService.search(q, page);
        long took = System.currentTimeMillis() - start;

        List<SearchResultDto> results = result.hits().stream()
            .map(SearchResultDto::from)
            .toList();

        SearchResponse response = new SearchResponse(
            q,
            page,
            searchProperties.getPageSize(),
            results.size(),
            result.hasMore(),
            took,
            results
        );

        return ResponseEntity.ok(response);
    }
}
