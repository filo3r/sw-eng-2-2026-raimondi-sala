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
 * Enables caching for API calls to reduce usage and improve performance.
 * Uses Caffeine cache with customizable TTL and size for each cache type.
 * Cache configurations:
 * - geocoding: Long TTL for address to coordinate conversions, as addresses are nearly immutable.
 * - cyclingRoute: Moderate TTL for calculated routes with full geometry to allow for infrastructure changes.
 * - weatherData: No TTL as historical data is immutable. Only size-based LRU eviction is used.
 */
@Configuration
public class CacheConfig {

    /**
     * Configures the cache manager with three separate caches using Caffeine.
     * Each cache has customized maximum size and eviction strategy based on data characteristics:
     * - "geocoding": Long TTL (addresses rarely change)
     * - "cyclingRoute": Moderate TTL (routes change occasionally)
     * - "weatherData": No TTL (historical data is immutable, only LRU eviction based on size)
     * @return configured cache manager with separate cache specifications
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("geocoding", 50000, 90, TimeUnit.DAYS),
                buildCache("cyclingRoute", 5000, 30, TimeUnit.DAYS),
                buildCache("weatherData", 20000)
        ));
        return cacheManager;
    }

    /**
     * Builds a Caffeine cache with time-based and size-based eviction.
     * Entries expire after the specified TTL and are also removed when cache reaches maximum size (LRU).
     * All caches record statistics for monitoring.
     * @param name the cache name
     * @param maximumSize the maximum number of entries
     * @param ttl the time-to-live duration
     * @param timeUnit the time unit for TTL
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
     * Builds a Caffeine cache with only size-based eviction (no TTL).
     * Suitable for immutable historical data where only LRU eviction is needed.
     * When cache is full, least recently used entries are automatically removed.
     * All caches record statistics for monitoring.
     * @param name the cache name
     * @param maximumSize the maximum number of entries
     * @return configured CaffeineCache without expiration
     */
    private CaffeineCache buildCache(String name, long maximumSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .recordStats()
                .build());
    }

}