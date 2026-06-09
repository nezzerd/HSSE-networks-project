package com.searchengine.service;

import com.searchengine.indexer.LuceneSearcher;
import com.searchengine.indexer.SearchPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final LuceneSearcher searcher;

    public SearchPage search(String query, int page) {
        if (query == null || query.isBlank()) {
            return SearchPage.empty();
        }
        try {
            return searcher.search(query, page);
        } catch (ParseException e) {
            log.debug("Invalid query syntax: {}", query);
            return SearchPage.empty();
        } catch (IOException e) {
            log.error("Search failed for query: {}", query, e);
            return SearchPage.empty();
        }
    }

    public long countIndexedDocs() {
        try {
            return searcher.countDocs();
        } catch (IOException e) {
            log.error("Could not count indexed docs", e);
            return 0;
        }
    }
}
