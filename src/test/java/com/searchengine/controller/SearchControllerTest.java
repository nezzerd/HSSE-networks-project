package com.searchengine.controller;

import com.searchengine.config.SearchProperties;
import com.searchengine.indexer.SearchHit;
import com.searchengine.indexer.SearchPage;
import com.searchengine.service.RateLimiterService;
import com.searchengine.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@Import({SearchProperties.class, RateLimiterService.class})
class SearchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    SearchService searchService;

    @Test
    @WithMockUser
    void search_returnsResults() throws Exception {
        SearchHit hit = new SearchHit(1L, "https://example.com", "Example", "An <mark>example</mark> page", 1.5f);
        when(searchService.search(eq("example"), anyInt()))
            .thenReturn(new SearchPage(List.of(hit), false));

        mockMvc.perform(get("/api/search").param("q", "example"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query").value("example"))
            .andExpect(jsonPath("$.returned").value(1))
            .andExpect(jsonPath("$.hasMore").value(false))
            .andExpect(jsonPath("$.results[0].url").value("https://example.com"));
    }

    @Test
    @WithMockUser
    void search_rejectsBlankQuery() throws Exception {
        mockMvc.perform(get("/api/search").param("q", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void search_rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "test").param("page", "-1"))
            .andExpect(status().isBadRequest());
    }
}
