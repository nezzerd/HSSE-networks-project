package com.searchengine.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class UrlUtilsTest {

    @Test
    void normalize_removesTrailingSlash() {
        assertThat(UrlUtils.normalize("https://example.com/path/"))
            .isEqualTo("https://example.com/path");
    }

    @Test
    void normalize_removesFragment() {
        assertThat(UrlUtils.normalize("https://example.com/page#section"))
            .isEqualTo("https://example.com/page");
    }

    @Test
    void normalize_sortsQueryParams() {
        assertThat(UrlUtils.normalize("https://example.com/search?z=1&a=2"))
            .isEqualTo("https://example.com/search?a=2&z=1");
    }

    @Test
    void normalize_lowercasesSchemeAndHost() {
        assertThat(UrlUtils.normalize("HTTPS://EXAMPLE.COM/path"))
            .isEqualTo("https://example.com/path");
    }

    @Test
    void normalize_stripsDefaultHttpPort() {
        assertThat(UrlUtils.normalize("http://example.com:80/path"))
            .isEqualTo("http://example.com/path");
    }

    @Test
    void normalize_stripsDefaultHttpsPort() {
        assertThat(UrlUtils.normalize("https://example.com:443/path"))
            .isEqualTo("https://example.com/path");
    }

    @Test
    void normalize_keepsNonDefaultPort() {
        assertThat(UrlUtils.normalize("http://example.com:8080/path"))
            .isEqualTo("http://example.com:8080/path");
    }

    @ParameterizedTest
    @ValueSource(strings = {"file:///etc/passwd", "ftp://example.com", "javascript:alert(1)"})
    void normalize_rejectsDisallowedSchemes(String url) {
        assertThatThrownBy(() -> UrlUtils.normalize(url))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost/admin",
        "http://127.0.0.1/",
        "http://0.0.0.0/",
        "http://10.0.0.1/",
        "http://192.168.1.1/",
        "http://172.16.0.1/",
        "http://169.254.169.254/latest/meta-data/",
        "http://100.64.0.1/"
    })
    void checkSsrf_rejectsPrivateHosts(String url) {
        assertThatThrownBy(() -> UrlUtils.checkSsrf(url))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkSsrf_allowsPublicAddress() {
        assertThatCode(() -> UrlUtils.checkSsrf("http://93.184.216.34/"))
            .doesNotThrowAnyException();
    }
}
