package com.searchengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Запрос на запуск краулера со списком стартовых URL")
public record CrawlRequest(

    @Schema(
        description = "Стартовые URL для обхода (от 1 до 20)",
        example = "[\"https://ru.wikipedia.org/wiki/Java\"]"
    )
    @NotEmpty(message = "At least one seed URL is required")
    @Size(max = 20, message = "Maximum 20 seed URLs per request")
    List<@org.hibernate.validator.constraints.URL String> seedUrls
) {}
