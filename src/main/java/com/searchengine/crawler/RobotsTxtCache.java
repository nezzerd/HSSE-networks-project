package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RobotsTxtCache {

    private static final SimpleRobotRulesParser PARSER = new SimpleRobotRulesParser();
    private static final int MAX_ROBOTS_SIZE = 512 * 1024;

    private final CrawlerProperties props;
    private final ConcurrentHashMap<String, BaseRobotRules> cache = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public boolean isAllowed(String url) {
        try {
            URI uri = URI.create(url);
            String origin = uri.getScheme() + "://" + uri.getHost()
                + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            BaseRobotRules rules = cache.computeIfAbsent(origin, this::fetchRules);
            return rules.isAllowed(url);
        } catch (Exception e) {
            log.debug("robots.txt check failed for {}, allowing: {}", url, e.getMessage());
            return true;
        }
    }

    private BaseRobotRules fetchRules(String origin) {
        String robotsUrl = origin + "/robots.txt";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(robotsUrl))
                .timeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .header("User-Agent", props.getUserAgent())
                .GET()
                .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                byte[] body = response.body();
                if (body.length > MAX_ROBOTS_SIZE) {
                    byte[] trimmed = new byte[MAX_ROBOTS_SIZE];
                    System.arraycopy(body, 0, trimmed, 0, MAX_ROBOTS_SIZE);
                    body = trimmed;
                }
                return PARSER.parseContent(robotsUrl, body, "text/plain", props.getUserAgent());
            }

            return PARSER.failedFetch(response.statusCode());

        } catch (IOException | InterruptedException e) {
            log.debug("Could not fetch robots.txt from {}: {}", robotsUrl, e.getMessage());
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return PARSER.failedFetch(0);
        }
    }
}
