package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PolitenessThrottle {

    private static final int MAX_TRACKED_DOMAINS = 10_000;

    private final CrawlerProperties props;

    private final Map<String, Long> lastAccessTime = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > MAX_TRACKED_DOMAINS;
            }
        }
    );

    public void waitIfNeeded(String url) throws InterruptedException {
        String domain = UrlUtils.extractDomain(url);
        if (domain.isBlank()) return;

        long now = System.currentTimeMillis();
        Long last;
        synchronized (lastAccessTime) {
            last = lastAccessTime.put(domain, now);
        }

        if (last != null) {
            long delay = props.getPolitenessDelayMs() - (now - last);
            if (delay > 0) {
                Thread.sleep(delay);
                synchronized (lastAccessTime) {
                    lastAccessTime.put(domain, System.currentTimeMillis());
                }
            }
        }
    }
}
