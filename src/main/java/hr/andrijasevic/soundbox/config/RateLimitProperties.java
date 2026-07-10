package hr.andrijasevic.soundbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config for per-IP rate limiting on auth endpoints.
 * {@code app.rate-limit.enabled=false} turns it off (used in tests).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int authCapacity,
        int authRefillPerMinute
) {
    public RateLimitProperties {
        if (authCapacity <= 0) authCapacity = 10;
        if (authRefillPerMinute <= 0) authRefillPerMinute = 10;
    }
}
