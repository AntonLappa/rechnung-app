package com.antonlappa.rechnungapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the {@code app.jwt.*} properties from application.yaml
 * to a type-safe POJO.
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Hex-encoded 256-bit secret used to sign JWTs.
     */
    private String secret;

    /**
     * Token expiration time in milliseconds (default: 24h).
     */
    private long expiration;
}
