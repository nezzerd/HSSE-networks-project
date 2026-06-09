package com.searchengine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Entity
@Table(name = "page")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "url_hash", nullable = false, length = 64, unique = true)
    private String urlHash;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "simhash")
    private Long simhash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private PageStatus status = PageStatus.FETCHED;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private Instant fetchedAt = Instant.now();

    public enum PageStatus {
        FETCHED, ERROR, SKIPPED
    }
}
