package it.polimi.se.bbp.service.mapbox;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import it.polimi.se.bbp.config.mapbox.MapboxConfig;
import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.CyclingRouteResult;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import it.polimi.se.bbp.mapper.mapbox.CyclingRouteResultMapper;
import it.polimi.se.bbp.mapper.mapbox.GeocodeResultMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for interacting with Mapbox APIs.
 * Provides geocoding via Search Box API and route calculation for cycling routes.
 * Optimized with:
 * - Caching at chunk level to maximize cache hit rate and API call reuse
 * - Parallel processing using Virtual Threads
 * - Rate limiting to respect Mapbox API limits (10 req/sec geocoding, 5 req/sec directions)
 * - Self-injection pattern to enable cache on internal method calls
 */
@Service
public class MapboxService {

    /**
     * Maximum number of waypoints allowed per Mapbox Directions API call.
     */
    private static final int MAX_WAYPOINTS_PER_REQUEST = 25;

    /**
     * RestClient configured specifically for Mapbox API calls.
     */
    private final RestClient mapboxRestClient;

    /**
     * Configuration bean containing Mapbox API settings and credentials.
     */
    private final MapboxConfig mapboxConfig;

    /**
     * Mapper for parsing Mapbox Geocoding API responses into GeocodeResult DTOs.
     */
    private final GeocodeResultMapper geocodeResultMapper;

    /**
     * Mapper for parsing Mapbox Directions API responses into CyclingRouteResult DTOs.
     */
    private final CyclingRouteResultMapper cyclingRouteResultMapper;

    /**
     * Executor service for parallel geocoding operations.
     */
    private final ExecutorService executorService;

    /**
     * Rate limiter for geocoding API (10 requests per second).
     */
    private final RateLimiter geocodingRateLimiter;

    /**
     * Rate limiter for directions API (5 requests per second).
     */
    private final RateLimiter directionsRateLimiter;

    /**
     * Self-reference to this service's Spring proxy.
     * Used to enable cache interception on internal method calls.
     * The Lazy annotation prevents circular dependency issues during bean initialization.
     */
    private final MapboxService self;

    /**
     * Endpoint path for the Mapbox Search Box Forward API.
     */
    @Value("${mapbox.api.searchbox.forward.endpoint}")
    private String searchBoxForwardEndpoint;

    /**
     * Endpoint path for the Mapbox Directions API.
     */
    @Value("${mapbox.api.directions.endpoint}")
    private String directionsEndpoint;

    /**
     * Constructor for dependency injection.
     * Initializes Virtual Thread executor and rate limiters.
     * @param mapboxRestClient RestClient configured for Mapbox API calls
     * @param mapboxConfig Configuration bean for Mapbox API settings
     * @param geocodeResultMapper Mapper for geocoding responses
     * @param cyclingRouteResultMapper Mapper for directions responses
     */
    public MapboxService(@Qualifier("mapboxRestClient") RestClient mapboxRestClient, MapboxConfig mapboxConfig, GeocodeResultMapper geocodeResultMapper, CyclingRouteResultMapper cyclingRouteResultMapper, @Lazy MapboxService self) {
        this.mapboxRestClient = mapboxRestClient;
        this.mapboxConfig = mapboxConfig;
        this.geocodeResultMapper = geocodeResultMapper;
        this.cyclingRouteResultMapper = cyclingRouteResultMapper;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.geocodingRateLimiter = RateLimiter.of("geocoding", RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build());
        this.directionsRateLimiter = RateLimiter.of("directions", RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build());
        this.self = self;
    }

    /**
     * Converts an address string to geographic coordinates using Mapbox Search Box Forward API.
     * Results are cached to avoid repeated API calls for the same address.
     * Rate limited to 10 requests per second.
     * Searches only for street, address, and POI types.
     * Exception handling:
     * - IllegalArgumentException: Invalid address (no results found) → 400 BAD_REQUEST
     * - IllegalStateException: Mapbox service unavailable → 503 SERVICE_UNAVAILABLE
     * @param address the address to geocode (e.g., "Via Roma, 10, Milano, Italy")
     * @return GeocodeResult containing the formatted address and coordinates
     * @throws IllegalArgumentException if no results are found for the address
     * @throws IllegalStateException if the Mapbox service is unavailable or rate limit is exceeded
     */
    @Cacheable(value = "geocoding", key = "#address.trim().toLowerCase().replaceAll('\\s+', ' ').replaceAll('[,.]', '')", sync = true)
    public GeocodeResult geocodeAddress(String address) {
        try {
            // Acquire rate limiter permit (blocks if necessary, throws RequestNotPermitted after timeout)
            RateLimiter.waitForPermission(geocodingRateLimiter);
        } catch (RequestNotPermitted e) {
            throw new IllegalStateException("Geocoding service is temporarily unavailable due to high traffic. Please try again later.", e);
        }
        try {
            // Build the API request URL with query parameters
            String url = UriComponentsBuilder.fromPath(searchBoxForwardEndpoint)
                    .queryParam("q", address)
                    .queryParam("access_token", mapboxConfig.getApiKey())
                    .queryParam("types", "address,street,poi")
                    .queryParam("limit", 1)
                    .queryParam("language", "en")
                    .toUriString();
            // Execute HTTP GET request and retrieve response body as String
            String response = mapboxRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            // Parse JSON response and create GeocodeResult DTO
            return geocodeResultMapper.fromJsonResponse(response);
        } catch (IllegalArgumentException e) {
            // Invalid address → 400
            throw e;
        } catch (IllegalStateException e) {
            // Mapbox service unavailable → 503
            throw e;
        } catch (Exception e) {
            // Any other error (network issues, etc.) - wrap as service unavailable → 503
            throw new IllegalStateException("Mapbox geocoding service is currently unavailable", e);
        }
    }

    /**
     * Geocodes multiple addresses in parallel for improved performance.
     * Uses CompletableFuture with Virtual Threads to execute geocoding requests concurrently.
     * Each request respects the rate limit of 10 requests/second and benefits from caching.
     * Calls geocodeAddress via self-reference to ensure cache interception works correctly.
     * @param addresses list of addresses to geocode
     * @return list of GeocodeResult in the same order as the input addresses
     */
    public List<GeocodeResult> geocodeAddressesParallel(List<String> addresses) {
        // Create a CompletableFuture for each address
        List<CompletableFuture<GeocodeResult>> futures = addresses.stream()
                .map(address -> CompletableFuture.supplyAsync(() -> self.geocodeAddress(address), executorService))
                .toList();
        // Wait for all futures to complete and collect results
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Calculates a cycling route through multiple waypoints using Mapbox Directions API.
     * Rate limited to 5 requests per second.
     * Handles routes with more than 25 waypoints by splitting them into multiple API calls.
     * Caching strategy: Delegates to calculateSingleRoute via self-reference (Spring proxy),
     * which is cached at the chunk level. This allows route segments with overlapping
     * waypoints to be reused across different complete routes, maximizing cache efficiency
     * and minimizing API calls.
     * Exception handling:
     * - IllegalArgumentException: Invalid waypoints (no route found, no segment) → 400 BAD_REQUEST
     * - IllegalStateException: Mapbox service unavailable → 503 SERVICE_UNAVAILABLE
     * @param waypoints ordered list of coordinates that define the route (min 2, no max)
     * @return CyclingRouteResult containing all route coordinates and total distance
     * @throws IllegalArgumentException if waypoints are invalid or no route can be found
     * @throws IllegalStateException if the Mapbox service is unavailable
     */
    public CyclingRouteResult calculateCyclingRoute(List<Coordinate> waypoints) {
        // Validate input
        if (waypoints == null || waypoints.size() < 2)
            throw new IllegalArgumentException("At least 2 waypoints are required to calculate a route");
        // If waypoints fit in a single request, call API directly
        if (waypoints.size() <= MAX_WAYPOINTS_PER_REQUEST)
            return self.calculateSingleRoute(waypoints);
        // Split waypoints into chunks and calculate route for each chunk
        return calculateMultiChunkRoute(waypoints);
    }

    /**
     * Calculates a route for a single set of waypoints (≤25).
     * INTERNAL USE ONLY: This method is part of the internal caching and chunking strategy.
     * External services should use calculateCyclingRoute instead, which handles routes of any size.
     * This method is public only because Spring's proxy-based AOP requires it for @Cacheable to work.
     * Results are cached based on the waypoint list to avoid redundant API calls.
     * This method is cached at the chunk level, allowing route segments to be reused
     * across different complete routes that share common waypoint sequences.
     * The cache key is the entire waypoint list, so identical sequences of coordinates
     * will hit the cache, even when called as part of different larger routes.
     * Note: This method must be called via the Spring proxy (self-reference) to enable
     * caching on internal calls. Direct calls (this.calculateSingleRoute) will bypass
     * the cache proxy.
     * Rate limited to 5 requests per second.
     * @param waypoints list of coordinates (2-25 waypoints)
     * @return cycling route result with coordinates and distance
     * @throws IllegalArgumentException if waypoints are invalid or no route can be found
     * @throws IllegalStateException if the Mapbox service is unavailable or rate limit is exceeded
     */
    @Cacheable(value = "cyclingRoute", key = "#waypoints", sync = true)
    public CyclingRouteResult calculateSingleRoute(List<Coordinate> waypoints) {
        try {
            // Acquire rate limiter permit (blocks if necessary, throws RequestNotPermitted after timeout)
            RateLimiter.waitForPermission(directionsRateLimiter);
        } catch (RequestNotPermitted e) {
            throw new IllegalStateException("Routing service is temporarily unavailable due to high traffic. Please try again later.", e);
        }
        try {
            // Build coordinates parameter: "lon1,lat1;lon2,lat2;..."
            String coordinatesParam = waypoints.stream()
                    .map(coord -> coord.getLongitude() + "," + coord.getLatitude())
                    .collect(Collectors.joining(";"));
            // Build the API request URL
            String url = UriComponentsBuilder.fromPath(directionsEndpoint)
                    .path("/{coordinates}")
                    .queryParam("access_token", mapboxConfig.getApiKey())
                    .queryParam("geometries", "geojson")
                    .queryParam("overview", "full")
                    .buildAndExpand(coordinatesParam)
                    .toUriString();
            // Execute HTTP GET request
            String response = mapboxRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            // Parse JSON response and create CyclingRouteResult DTO
            return cyclingRouteResultMapper.fromJsonResponse(response);
        } catch (IllegalArgumentException e) {
            // Invalid waypoints → 400
            throw e;
        } catch (IllegalStateException e) {
            // Mapbox service unavailable → 503
            throw e;
        } catch (Exception e) {
            // Any other error → 503
            throw new IllegalStateException("Mapbox routing service is currently unavailable", e);
        }
    }

    /**
     * Calculates a route for waypoints exceeding the 25-waypoint limit.
     * Splits waypoints into overlapping chunks and combines the results.
     * Each chunk is calculated via self-reference to enable caching.
     * @param waypoints list of coordinates (>25 waypoints)
     * @return combined cycling route result
     */
    private CyclingRouteResult calculateMultiChunkRoute(List<Coordinate> waypoints) {
        List<Coordinate> allRouteCoordinates = new ArrayList<>();
        double totalDistance = 0.0;
        int start = 0;
        while (start < waypoints.size()) {
            // Determine end index for this chunk (max 25 waypoints)
            int end = Math.min(start + MAX_WAYPOINTS_PER_REQUEST, waypoints.size());
            // Extract chunk of waypoints
            List<Coordinate> chunk = waypoints.subList(start, end);
            // Calculate route for this chunk
            CyclingRouteResult chunkResult = self.calculateSingleRoute(chunk);
            // Add coordinates to result (avoid duplicates on overlap)
            if (allRouteCoordinates.isEmpty())
                // First chunk: add all coordinates
                allRouteCoordinates.addAll(chunkResult.getRouteCoordinates());
            else
                // Subsequent chunks: skip first coordinate (it's the last of previous chunk)
                allRouteCoordinates.addAll(chunkResult.getRouteCoordinates().subList(1, chunkResult.getRouteCoordinates().size()));
            // Add distance
            totalDistance += chunkResult.getDistanceInMeters();
            // Move to next chunk (overlap last waypoint)
            start = end - 1;
        }
        return cyclingRouteResultMapper.toCyclingRouteResult(allRouteCoordinates, totalDistance);
    }

}