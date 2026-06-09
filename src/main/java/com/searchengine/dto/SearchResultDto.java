package com.searchengine.dto;

import com.searchengine.indexer.SearchHit;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Один результат поиска")
public record SearchResultDto(

    @Schema(description = "URL найденной страницы", example = "https://spring.io/projects/spring-boot")
    String url,

    @Schema(description = "Заголовок страницы (plain text)", example = "Spring Boot")
    String title,

    @Schema(description = "Заголовок с подсвеченными совпадениями (HTML с тегами mark, экранирован)",
        example = "<mark>Spring</mark> Boot")
    String titleHighlighted,

    @Schema(description = "Фрагмент текста с подсвеченными совпадениями (HTML с тегами mark)",
        example = "...build <mark>spring</mark> applications...")
    String snippet,

    @Schema(description = "Релевантность (оценка BM25)", example = "2.34")
    float score
) {
    public static SearchResultDto from(SearchHit hit) {
        return new SearchResultDto(
            hit.url(),
            hit.title(),
            hit.titleHighlighted(),
            hit.snippet(),
            hit.score()
        );
    }
}
