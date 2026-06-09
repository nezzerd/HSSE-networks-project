package com.searchengine.repository;

import com.searchengine.entity.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PageRepositoryTest {

    @Autowired
    PageRepository pageRepository;

    @Test
    void findStaleFetchedPages_returnsOnlyOldFetchedPages() {
        Instant now = Instant.now();

        pageRepository.save(Page.builder()
            .url("https://example.com/old").urlHash("hash-old")
            .content("old content").contentHash("ch-old")
            .status(Page.PageStatus.FETCHED)
            .fetchedAt(now.minus(30, ChronoUnit.DAYS))
            .build());

        pageRepository.save(Page.builder()
            .url("https://example.com/fresh").urlHash("hash-fresh")
            .content("fresh content").contentHash("ch-fresh")
            .status(Page.PageStatus.FETCHED)
            .fetchedAt(now)
            .build());

        Instant threshold = now.minus(7, ChronoUnit.DAYS);
        List<Page> stale = pageRepository.findByStatusAndFetchedAtBeforeOrderByFetchedAtAsc(
            Page.PageStatus.FETCHED, threshold, PageRequest.of(0, 500));

        assertThat(stale).extracting(Page::getUrlHash).containsExactly("hash-old");
    }
}
