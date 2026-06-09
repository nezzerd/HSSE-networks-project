package com.searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search")
@Getter
@Setter
public class SearchProperties {

    private String indexPath = "./data/lucene-index";
    private int maxResults = 100;
    private int snippetLength = 200;
    private int pageSize = 10;
}
