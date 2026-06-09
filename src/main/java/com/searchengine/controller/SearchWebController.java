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

import java.util.List;

@Controller
@RequiredArgsConstructor
@Validated
public class SearchWebController {

    private static final int MAX_PAGE = 99;
    private static final int MAX_QUERY_LENGTH = 256;

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
            return "results";
        }

        long start = System.currentTimeMillis();
        SearchPage result = searchService.search(query, page);
        long took = System.currentTimeMillis() - start;

        List<SearchResultDto> results = result.hits().stream()
            .map(SearchResultDto::from)
            .toList();

        model.addAttribute("results", results);
        model.addAttribute("hasResults", !results.isEmpty());
        model.addAttribute("hasMore", result.hasMore());
        model.addAttribute("tookMillis", took);
        model.addAttribute("prevPage", page > 0 ? page - 1 : null);
        model.addAttribute("nextPage", result.hasMore() ? page + 1 : null);

        return "results";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("indexedDocs", searchService.countIndexedDocs());
        return "about";
    }
}
