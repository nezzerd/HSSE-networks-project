package com.searchengine.service;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.entity.Page;
import com.searchengine.repository.PageRepository;
import com.searchengine.util.SimHash;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageService {

    private final PageRepository pageRepository;
    private final CrawlerProperties crawlerProperties;

    public Optional<Page> findById(Long id) {
        return pageRepository.findById(id);
    }

    public Optional<Page> findByUrlHash(String urlHash) {
        return pageRepository.findByUrlHash(urlHash);
    }

    public boolean existsByUrlHash(String urlHash) {
        return pageRepository.existsByUrlHash(urlHash);
    }

    public boolean existsByContentHash(String contentHash) {
        return pageRepository.existsByContentHash(contentHash);
    }

    public List<Page> findFetchedPages(Pageable pageable) {
        return pageRepository.findByStatus(Page.PageStatus.FETCHED, pageable);
    }

    public List<String> findUrlHashesNewestFirst(Pageable pageable) {
        return pageRepository.findUrlHashesOrderByFetchedAtDesc(pageable);
    }

    public List<Page> findStaleFetchedPages(Instant fetchedBefore, int limit) {
        return pageRepository.findByStatusAndFetchedAtBeforeOrderByFetchedAtAsc(
            Page.PageStatus.FETCHED, fetchedBefore, PageRequest.of(0, limit));
    }

    public long countFetched() {
        return pageRepository.countByStatus(Page.PageStatus.FETCHED);
    }

    public long countErrors() {
        return pageRepository.countByStatus(Page.PageStatus.ERROR);
    }

    @Transactional
    public Optional<Page> saveIfNew(String url, String urlHash, String contentHash,
                                    String title, String content) {
        if (pageRepository.existsByUrlHash(urlHash) || pageRepository.existsByContentHash(contentHash)) {
            return Optional.empty();
        }

        Long simhash = (content != null && !content.isBlank()) ? SimHash.compute(content) : null;
        if (simhash != null && isNearDuplicate(simhash)) {
            return Optional.empty();
        }

        Page saved = pageRepository.save(Page.builder()
            .url(url).urlHash(urlHash).contentHash(contentHash)
            .title(title).content(content).simhash(simhash)
            .status(Page.PageStatus.FETCHED).build());
        return Optional.of(saved);
    }

    private boolean isNearDuplicate(long simhash) {
        int threshold = crawlerProperties.getSimhashThreshold();
        List<Long> candidates = pageRepository.findRecentSimhashes(
            PageRequest.of(0, crawlerProperties.getSimhashCandidates()));
        for (Long candidate : candidates) {
            if (candidate != null && SimHash.hammingDistance(simhash, candidate) < threshold) {
                return true;
            }
        }
        return false;
    }
}
