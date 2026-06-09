package com.searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/docs",
                "/docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api-docs/**",
                "/v3/api-docs/**"
            )
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self'; " +
                        "object-src 'none'; " +
                        "base-uri 'self';"
                    )
                )
                .frameOptions(frame -> frame.sameOrigin())
            );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/search",
                    "/about",
                    "/api/search",
                    "/api/crawl",
                    "/api/crawl/**",
                    "/api/index",
                    "/api/index/**",
                    "/actuator/health",
                    "/robots.txt",
                    "/sitemap.xml",
                    "/css/**",
                    "/js/**",
                    "/favicon.ico"
                ).permitAll()
                .anyRequest().denyAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self'; " +
                        "img-src 'self' data:; " +
                        "font-src 'self'; " +
                        "object-src 'none'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self';"
                    )
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000)
                )
            );

        return http.build();
    }
}
