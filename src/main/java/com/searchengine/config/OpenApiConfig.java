package com.searchengine.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Search Engine API")
                .description("""
                    REST API мини-поисковика на Spring Boot.

                    Возможности:
                    - запуск и мониторинг краулера, который обходит сайты и складывает страницы в базу;
                    - управление полнотекстовым индексом Apache Lucene;
                    - поиск по проиндексированным страницам с пагинацией и подсветкой совпадений.

                    Все эндпоинты публичные, но защищены ограничением частоты запросов (rate limiting).
                    """)
                .version("1.0.0")
                .contact(new Contact().name("Admin").email("admin@example.com"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
            )
            .externalDocs(new ExternalDocumentation()
                .description("Описание проекта")
                .url("/about"))
            .servers(List.of(
                new Server().url("/").description("Текущий сервер")
            ));
    }
}
