package it.polimi.se.bbp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Spring Cache abstraction.
 * Enables caching for Mapbox API calls to reduce external API usage and improve performance.
 * Uses Caffeine cache with 30-day TTL for geographic data.
 * Each cache has a different maximum size based on expected memory usage:
 * - geocoding: 50,000 entries (~10 MB)
 * - cyclingRoute: 5,000 entries (~150 MB)
 */
@Configuration
public class CacheConfig {

    /**
     * Configures the cache manager with two separate caches using Caffeine:
     * - "geocoding": for address geocoding results (50,000 max entries)
     * - "cyclingRoute": for calculated cycling routes (5,000 max entries)
     * @return configured cache manager with separate cache specifications
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("geocoding", 50000),
                buildCache("cyclingRoute", 5000)
        ));
        return cacheManager;
    }

    /**
     * Builds a Caffeine cache with specific configuration.
     * @param name the cache name
     * @param maximumSize the maximum number of entries
     * @return configured CaffeineCache
     */
    private CaffeineCache buildCache(String name, long maximumSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.DAYS)
                .maximumSize(maximumSize)
                .recordStats()
                .build());
    }

}