package it.polimi.se.bbp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Spring Cache using Caffeine.
 * Enables caching to reduce external API calls and improve performance.
 * NOTE: This configuration is designed for development and demonstration purposes.
 * For production deployment, cache policies must be reviewed and aligned with:
 * - Mapbox API Terms of Service and caching guidelines
 * - Open-Meteo API usage policies and data retention rules
 * Defines three caches with different eviction strategies:
 * geocoding (24h TTL for development), cyclingRoute (24h TTL for development),
 * weatherData (no TTL, historical data is immutable, LRU eviction only).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures cache manager with three Caffeine caches.
     * Cache sizes are set for development/demo purposes and should be adjusted
     * for production based on actual usage patterns and API provider policies.
     * @return configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("geocoding", 500, 24, TimeUnit.HOURS),
                buildCache("cyclingRoute", 200, 24, TimeUnit.HOURS),
                buildCache("weatherData", 1000)
        ));
        return cacheManager;
    }

    /**
     * Builds a Caffeine cache with TTL and size-based eviction.
     * Entries expire after TTL and LRU eviction applies when maximum size is reached.
     * NOTE: TTL values are set for development. Production environments must verify
     * compliance with Mapbox and Open-Meteo caching policies before deployment.
     * @param name cache name
     * @param maximumSize maximum number of entries (configured for demo purposes)
     * @param ttl time-to-live duration
     * @param timeUnit time unit for TTL
     * @return configured CaffeineCache with expiration
     */
    private CaffeineCache buildCache(String name, long maximumSize, long ttl, TimeUnit timeUnit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl, timeUnit)
                .maximumSize(maximumSize)
                .recordStats()
                .build());
    }

    /**
     * Builds a Caffeine cache with only size-based LRU eviction.
     * No TTL is applied, suitable for immutable data.
     * @param name cache name
     * @param maximumSize maximum number of entries (configured for demo purposes)
     * @return configured CaffeineCache without expiration
     */
    private CaffeineCache buildCache(String name, long maximumSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .recordStats()
                .build());
    }

}