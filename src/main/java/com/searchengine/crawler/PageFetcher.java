package com.searchengine.crawler;

import com.searchengine.config.CrawlerProperties;
import com.searchengine.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageFetcher {

    private static final String CONTENT_TYPE_HTML = "text/html";

    private final CrawlerProperties props;

    public CrawlResult fetch(String url) {
        String currentUrl = url;

        try {
            for (int redirects = 0; redirects <= props.getMaxRedirects(); redirects++) {
                try {
                    UrlUtils.checkSsrf(currentUrl);
                } catch (IllegalArgumentException e) {
                    return CrawlResult.failure(url, "SSRF check failed: " + e.getMessage());
                }

                Connection.Response response = Jsoup.connect(currentUrl)
                    .userAgent(props.getUserAgent())
                    .timeout(props.getReadTimeoutMs())
                    .maxBodySize((int) props.getMaxPageSizeBytes())
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();

                int status = response.statusCode();

                if (status >= 300 && status < 400) {
                    String location = response.header("Location");
                    if (location == null || location.isBlank()) {
                        return CrawlResult.failure(url, "Redirect without Location header");
                    }
                    try {
                        currentUrl = UrlUtils.normalize(response.url().toURI().resolve(location).toString());
                    } catch (Exception e) {
                        return CrawlResult.failure(url, "Invalid redirect target");
                    }
                    continue;
                }

                if (status < 200 || status >= 300) {
                    return CrawlResult.failure(url, "HTTP " + status);
                }

                String contentType = response.contentType();
                if (contentType == null || !contentType.contains(CONTENT_TYPE_HTML)) {
                    return CrawlResult.failure(url, "Skipped non-HTML content-type: " + contentType);
                }

                Document doc = response.parse();
                String title = doc.title();
                String text = doc.body() != null ? doc.body().text() : "";
                Set<String> links = extractLinks(doc);

                return CrawlResult.builder()
                    .url(url)
                    .title(title)
                    .text(text)
                    .outboundLinks(links)
                    .success(true)
                    .build();
            }

            return CrawlResult.failure(url, "Too many redirects");

        } catch (IOException e) {
            log.debug("Fetch failed for {}: {}", url, e.getMessage());
            return CrawlResult.failure(url, e.getMessage());
        } catch (RuntimeException e) {
            log.debug("Unexpected fetch error for {}: {}", url, e.getMessage());
            return CrawlResult.failure(url, "Fetch error");
        }
    }

    private Set<String> extractLinks(Document doc) {
        Set<String> links = new HashSet<>();
        Elements anchors = doc.select("a[href]");
        for (Element anchor : anchors) {
            String href = anchor.absUrl("href");
            if (href.isBlank()) continue;
            try {
                String normalized = UrlUtils.normalize(href);
                UrlUtils.checkSsrf(normalized);
                links.add(normalized);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return links;
    }
}
