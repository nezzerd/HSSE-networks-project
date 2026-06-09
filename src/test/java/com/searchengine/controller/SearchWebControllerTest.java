package com.searchengine.controller;

import com.searchengine.config.SearchProperties;
import com.searchengine.indexer.SearchHit;
import com.searchengine.indexer.SearchPage;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchWebController.class)
@Import(SearchProperties.class)
class SearchWebControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    SearchService searchService;

    @Test
    @WithMockUser
    void search_rendersResultsView() throws Exception {
        SearchHit hit = new SearchHit(1L, "https://example.com", "Example", "Example", "An <mark>example</mark>", 1.0f);
        when(searchService.search(anyString(), anyInt()))
            .thenReturn(new SearchPage(List.of(hit), false, 1L, null));

        mockMvc.perform(get("/search").param("q", "example"))
            .andExpect(status().isOk())
            .andExpect(view().name("results"))
            .andExpect(model().attribute("hasResults", true))
            .andExpect(model().attribute("totalHits", 1L))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("https://example.com")));
    }

    @Test
    @WithMockUser
    void search_emptyQuery_rendersEmptyResults() throws Exception {
        mockMvc.perform(get("/search").param("q", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("results"))
            .andExpect(model().attribute("hasResults", false));
    }

    @Test
    @WithMockUser
    void about_rendersAboutView() throws Exception {
        when(searchService.countIndexedDocs()).thenReturn(123L);

        mockMvc.perform(get("/about"))
            .andExpect(status().isOk())
            .andExpect(view().name("about"))
            .andExpect(model().attribute("indexedDocs", 123L));
    }
}
