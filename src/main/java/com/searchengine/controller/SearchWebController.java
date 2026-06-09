package com.searchengine.controller;

import com.searchengine.config.SearchProperties;
import com.searchengine.dto.SearchResultDto;
import com.searchengine.indexer.SearchPage;
import com.searchengine.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Validated
public class SearchWebController {

    private static final int MAX_PAGE = 99;
    private static final int MAX_QUERY_LENGTH = 256;
    private static final int PAGE_WINDOW = 10;

    private final SearchService searchService;
    private final SearchProperties searchProperties;

    @GetMapping("/search")
    public String search(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "page", defaultValue = "0")
        @Min(0) @Max(MAX_PAGE) int page,
        Model model
    ) {
        String query = q == null ? "" : q.strip();
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
        }

        model.addAttribute("query", query);
        model.addAttribute("page", page);

        if (query.isBlank()) {
            model.addAttribute("results", List.of());
            model.addAttribute("hasMore", false);
            model.addAttribute("hasResults", false);
            model.addAttribute("totalHits", 0L);
            model.addAttribute("totalPages", 0);
            model.addAttribute("pageNumbers", List.of());
            model.addAttribute("suggestion", null);
            return "results";
        }

        long start = System.currentTimeMillis();
        SearchPage result = searchService.search(query, page);
        long took = System.currentTimeMillis() - start;

        List<SearchResultDto> results = result.hits().stream()
            .map(SearchResultDto::from)
            .toList();

        int pageSize = searchProperties.getPageSize();
        long totalHits = result.totalHits();
        int totalPages = (int) ((totalHits + pageSize - 1) / pageSize);

        model.addAttribute("results", results);
        model.addAttribute("hasResults", !results.isEmpty());
        model.addAttribute("hasMore", result.hasMore());
        model.addAttribute("tookMillis", took);
        model.addAttribute("totalHits", totalHits);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageNumbers", buildPageWindow(page, totalPages));
        model.addAttribute("suggestion", result.suggestion());
        model.addAttribute("prevPage", page > 0 ? page - 1 : null);
        model.addAttribute("nextPage", result.hasMore() ? page + 1 : null);

        return "results";
    }

    private List<Integer> buildPageWindow(int current, int totalPages) {
        if (totalPages <= 1) {
            return List.of();
        }
        int start = Math.max(0, current - PAGE_WINDOW / 2);
        int end = Math.min(totalPages - 1, start + PAGE_WINDOW - 1);
        start = Math.max(0, end - PAGE_WINDOW + 1);

        List<Integer> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            pages.add(i);
        }
        return pages;
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("indexedDocs", searchService.countIndexedDocs());
        return "about";
    }
}
