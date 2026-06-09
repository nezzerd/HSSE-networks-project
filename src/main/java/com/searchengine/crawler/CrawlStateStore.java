package com.searchengine.crawler;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CrawlStateStore {

    private static final int MAX_VISITED = 2_000_000;

    private final Map<String, Boolean> visitedHashes = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > MAX_VISITED;
            }
        }
    );

    private final AtomicInteger sessionPageCount = new AtomicInteger(0);

    public boolean markVisited(String urlHash) {
        synchronized (visitedHashes) {
            return visitedHashes.putIfAbsent(urlHash, Boolean.TRUE) == null;
        }
    }

    public boolean isVisited(String urlHash) {
        synchronized (visitedHashes) {
            return visitedHashes.containsKey(urlHash);
        }
    }

    public int getMaxVisited() {
        return MAX_VISITED;
    }

    public int incrementAndGetPageCount() {
        return sessionPageCount.incrementAndGet();
    }

    public int getPageCount() {
        return sessionPageCount.get();
    }

    public void resetSessionCount() {
        sessionPageCount.set(0);
    }

    public void reset() {
        synchronized (visitedHashes) {
            visitedHashes.clear();
        }
        sessionPageCount.set(0);
    }
}
