package com.searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
@Getter
@Setter
public class AdminProperties {

    private String username = "admin";
    private String password = "";

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}
