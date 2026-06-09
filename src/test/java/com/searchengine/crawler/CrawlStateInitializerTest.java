package com.searchengine.crawler;

import com.searchengine.service.PageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlStateInitializerTest {

    @Mock
    PageService pageService;

    @Test
    void restoreVisitedHashes_marksPersistedHashesAsVisited() {
        CrawlStateStore stateStore = new CrawlStateStore();
        CrawlStateInitializer initializer = new CrawlStateInitializer(pageService, stateStore);

        when(pageService.findUrlHashesNewestFirst(any(Pageable.class)))
            .thenReturn(List.of("hash-a", "hash-b"));

        assertThat(stateStore.isVisited("hash-a")).isFalse();

        initializer.restoreVisitedHashes();

        assertThat(stateStore.isVisited("hash-a")).isTrue();
        assertThat(stateStore.isVisited("hash-b")).isTrue();
        assertThat(stateStore.getPageCount()).isZero();
    }
}
