package com.searchengine.controller;

import com.searchengine.config.AdminProperties;
import com.searchengine.config.SecurityConfig;
import com.searchengine.crawler.CrawlStateStore;
import com.searchengine.crawler.CrawlerService;
import com.searchengine.service.CrawlQueueService;
import com.searchengine.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrawlerController.class)
@Import({SecurityConfig.class, AdminProperties.class, RateLimiterService.class})
class CrawlerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CrawlerService crawlerService;

    @MockBean
    CrawlStateStore stateStore;

    @MockBean
    CrawlQueueService crawlQueueService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void clearQueue_whenStopped_clearsPendingAndFailed() throws Exception {
        when(crawlerService.isRunning()).thenReturn(false);
        when(crawlQueueService.clearQueue(true)).thenReturn(5);

        mockMvc.perform(post("/api/crawl/clear").param("onlyPending", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cleared").value(5));

        verify(crawlQueueService).clearQueue(true);
        verify(stateStore, never()).reset();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void clearQueue_all_resetsVisitedState() throws Exception {
        when(crawlerService.isRunning()).thenReturn(false);
        when(crawlQueueService.clearQueue(false)).thenReturn(9);

        mockMvc.perform(post("/api/crawl/clear").param("onlyPending", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cleared").value(9));

        verify(crawlQueueService).clearQueue(false);
        verify(stateStore).reset();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void clearQueue_whenRunning_returnsConflict() throws Exception {
        when(crawlerService.isRunning()).thenReturn(true);

        mockMvc.perform(post("/api/crawl/clear"))
            .andExpect(status().isConflict());

        verify(crawlQueueService, never()).clearQueue(anyBoolean());
    }

    @Test
    void clearQueue_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/crawl/clear"))
            .andExpect(status().isUnauthorized());

        verify(crawlQueueService, never()).clearQueue(anyBoolean());
    }
}
