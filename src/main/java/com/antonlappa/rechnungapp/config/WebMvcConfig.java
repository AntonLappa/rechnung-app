package com.antonlappa.rechnungapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // Set ALLOWED_ORIGINS in Railway (comma-separated) e.g. https://rechnung-frontend.up.railway.app
    @Value("${ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String allowedOriginsRaw;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] origins = allowedOriginsRaw.split(",");
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
