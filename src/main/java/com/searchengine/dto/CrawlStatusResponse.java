package com.searchengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Текущий статус краулера и статистика очереди")
public record CrawlStatusResponse(

    @Schema(description = "Работает ли краулер сейчас", example = "true")
    boolean running,

    @Schema(description = "Сколько страниц обработано за текущую сессию", example = "128")
    int pagesCrawled,

    @Schema(description = "Сколько URL ожидает обработки в очереди", example = "512")
    long pagesInQueue,

    @Schema(description = "Сколько элементов очереди завершено", example = "128")
    long pagesDone
) {}
