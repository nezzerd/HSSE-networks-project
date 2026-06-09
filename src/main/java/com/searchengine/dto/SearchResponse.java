package com.searchengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ответ поиска с метаданными и списком результатов")
public record SearchResponse(

    @Schema(description = "Исходный поисковый запрос", example = "spring boot")
    String query,

    @Schema(description = "Номер текущей страницы (с нуля)", example = "0")
    int page,

    @Schema(description = "Размер страницы", example = "10")
    int pageSize,

    @Schema(description = "Сколько результатов возвращено на этой странице", example = "10")
    int returned,

    @Schema(description = "Общее число найденных результатов (с верхней планкой maxResults)", example = "57")
    long totalHits,

    @Schema(description = "Есть ли следующая страница", example = "true")
    boolean hasMore,

    @Schema(description = "Время выполнения запроса в миллисекундах", example = "12")
    long tookMillis,

    @Schema(description = "Предлагаемое исправление запроса при отсутствии результатов (может быть null)",
        example = "spring")
    String suggestion,

    @Schema(description = "Список найденных страниц")
    List<SearchResultDto> results
) {}
