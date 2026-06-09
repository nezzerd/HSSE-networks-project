package com.searchengine.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                return size() > MAX_TRACKED_CLIENTS;
            }
        }
    );

    public boolean tryConsume(String bucketKey, String clientId, int capacityPerMinute) {
        String key = bucketKey + ":" + clientId;
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(capacityPerMinute));
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(int capacityPerMinute) {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(capacityPerMinute,
                Refill.greedy(capacityPerMinute, Duration.ofMinutes(1))))
            .build();
    }

    public String resolveClientId(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            return first.strip();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }
}
