package com.searchengine.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Tag(name = "SEO", description = "robots.txt and sitemap.xml")
public class SeoController {

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robotsTxt() {
        String baseUrl = baseUrl();
        String content = """
                User-agent: *
                Allow: /
                Allow: /search
                Allow: /about
                Allow: /docs
                Disallow: /api/
                Disallow: /actuator/

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
        return ResponseEntity.ok(content);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemapXml() {
        String baseUrl = baseUrl();
        String content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                    <url>
                        <loc>%1$s/</loc>
                        <changefreq>daily</changefreq>
                        <priority>1.0</priority>
                    </url>
                    <url>
                        <loc>%1$s/about</loc>
                        <changefreq>monthly</changefreq>
                        <priority>0.5</priority>
                    </url>
                    <url>
                        <loc>%1$s/docs</loc>
                        <changefreq>weekly</changefreq>
                        <priority>0.4</priority>
                    </url>
                </urlset>
                """.formatted(baseUrl);
        return ResponseEntity.ok(content);
    }

    private String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
