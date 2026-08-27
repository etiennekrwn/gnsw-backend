package com.gns.gns_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.frontend.member-portal-url}")
    private String memberPortalUrl;

    @Value("${app.frontend.admin-url}")
    private String adminUrl;

    @Value("${app.frontend.main-site-url}")
    private String mainSiteUrl;

    /**
     * Provides the CORS policy as a {@link CorsConfigurationSource} so Spring
     * Security can apply it inside the security filter chain (via
     * {@code http.cors(...)}). This ensures preflight OPTIONS requests for the
     * PUBLIC endpoints also receive the Access-Control-Allow-Origin header.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Restrict CORS to the three GNS frontends (configured via env on Railway).
        // allowedOriginPatterns(...) works with allowCredentials(true), unlike setAllowedOrigins.
        config.setAllowedOriginPatterns(List.of(
                // Configured frontend origins (from backend env vars).
                stripTrailingSlash(mainSiteUrl),
                stripTrailingSlash(adminUrl),
                stripTrailingSlash(memberPortalUrl),
                // Safety net: the actual deployed Railway origins, so a mismatch in
                // the env vars (trailing slash / case / protocol) can never block login.
                "https://gns-frontend.up.railway.app",
                "https://gns-admin.up.railway.app",
                "https://gns-membersportal.up.railway.app",
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}