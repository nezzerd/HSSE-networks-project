package com.searchengine.repository;

import com.searchengine.entity.CrawlQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CrawlQueueRepositoryTest {

    @Autowired
    CrawlQueueRepository crawlQueueRepository;

    @Test
    void findPendingBatch_ordersByPriorityDescending() {
        crawlQueueRepository.save(CrawlQueue.builder()
            .url("https://example.com/low").urlHash("h-low").depth(3).priority(1)
            .status(CrawlQueue.QueueStatus.PENDING).build());
        crawlQueueRepository.save(CrawlQueue.builder()
            .url("https://example.com/high").urlHash("h-high").depth(0).priority(10)
            .status(CrawlQueue.QueueStatus.PENDING).build());
        crawlQueueRepository.save(CrawlQueue.builder()
            .url("https://example.com/mid").urlHash("h-mid").depth(1).priority(5)
            .status(CrawlQueue.QueueStatus.PENDING).build());

        List<CrawlQueue> batch = crawlQueueRepository.findPendingBatch(PageRequest.of(0, 10));

        assertThat(batch).extracting(CrawlQueue::getUrlHash)
            .containsExactly("h-high", "h-mid", "h-low");
    }
}
