package com.searchengine.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiter = new RateLimiterService();

    @Test
    void resolveClientId_prefersProxySetXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.7");

        assertThat(rateLimiter.resolveClientId(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void resolveClientId_ignoresClientSuppliedXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(rateLimiter.resolveClientId(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void resolveClientId_fallsBackToRemoteAddr_whenNoRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");

        assertThat(rateLimiter.resolveClientId(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void allowsRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryConsume("test", "1.2.3.4", 5)).isTrue();
        }
    }

    @Test
    void blocksRequestsOverLimit() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryConsume("test", "5.6.7.8", 3);
        }
        assertThat(rateLimiter.tryConsume("test", "5.6.7.8", 3)).isFalse();
    }

    @Test
    void differentClientsHaveSeparateBuckets() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryConsume("test", "1.1.1.1", 3);
        }
        assertThat(rateLimiter.tryConsume("test", "1.1.1.1", 3)).isFalse();
        assertThat(rateLimiter.tryConsume("test", "2.2.2.2", 3)).isTrue();
    }

    @Test
    void differentBucketKeysAreSeparate() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.tryConsume("search", "1.1.1.1", 3);
        }
        assertThat(rateLimiter.tryConsume("search", "1.1.1.1", 3)).isFalse();
        assertThat(rateLimiter.tryConsume("crawl", "1.1.1.1", 3)).isTrue();
    }
}
