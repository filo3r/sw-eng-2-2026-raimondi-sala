package it.polimi.se.bbp.service.mapbox;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import it.polimi.se.bbp.config.mapbox.MapboxConfig;
import it.polimi.se.bbp.dto.mapbox.MapboxGeocodeResponse;
import it.polimi.se.bbp.dto.mapbox.MapboxDirectionsResponse;
import it.polimi.se.bbp.exception.mapbox.*;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for interacting with Mapbox APIs.
 * Provides geocoding (Search Box API) and cycling route calculation (Directions API).
 * Optimized with caching at chunk level, parallel processing using Virtual Threads,
 * and rate limiting (10 req/sec geocoding, 5 req/sec directions).
 * Uses self-injection pattern to enable cache on internal method calls.
 */
@Service
public class MapboxService {

    /**
     * Maximum waypoints per Mapbox Directions API call.
     */
    private static final int MAX_WAYPOINTS_PER_REQUEST = 25;

    /**
     * Maximum forward geocoding requests per second.
     */
    private static final int FORWARD_GEOCODING_REQUESTS_PER_SECOND = 10;

    /**
     * Maximum reverse geocoding requests per second.
     */
    private static final int REVERSE_GEOCODING_REQUESTS_PER_SECOND = 10;

    /**
     * Maximum directions requests per second.
     */
    private static final int DIRECTIONS_REQUESTS_PER_SECOND = 5;

    /**
     * Rate limiter refresh period in seconds.
     */
    private static final int RATE_LIMIT_REFRESH_PERIOD_SECONDS = 1;

    /**
     * Maximum wait time for rate limiter permission in seconds.
     */
    private static final int RATE_LIMITER_TIMEOUT_SECONDS = 5;

    /**
     * Parallel geocoding batch size.
     * Controls concurrency towards RateLimiter. Lower values increase stability under
     * high concurrent load, higher values maximize throughput for single requests.
     */
    private static final int GEOCODING_BATCH_SIZE = 10;

    /**
     * RestClient configured for Mapbox API calls.
     */
    private final RestClient mapboxRestClient;

    /**
     * Configuration bean containing Mapbox API settings and credentials.
     */
    private final MapboxConfig mapboxConfig;

    /**
     * Executor service for parallel geocoding operations.
     */
    private final ExecutorService executorService;

    /**
     * Rate limiter for forward geocoding API (10 requests/second).
     */
    private final RateLimiter forwardGeocodingRateLimiter;

    /**
     * Rate limiter for reverse geocoding API (10 requests/second).
     */
    private final RateLimiter reverseGeocodingRateLimiter;

    /**
     * Rate limiter for directions API (5 requests/second).
     */
    private final RateLimiter directionsRateLimiter;

    /**
     * Self-reference to Spring proxy.
     * Enables cache interception on internal method calls.
     * Lazy annotation prevents circular dependency.
     */
    private final MapboxService self;

    /**
     * Mapbox Search Box Forward API endpoint path.
     */
    @Value("${mapbox.api.searchbox.forward.endpoint}")
    private String searchBoxForwardEndpoint;

    /**
     * Mapbox Search Box Reverse API endpoint path.
     */
    @Value("${mapbox.api.searchbox.reverse.endpoint}")
    private String searchBoxReverseEndpoint;

    /**
     * Mapbox Directions API endpoint path.
     */
    @Value("${mapbox.api.directions.endpoint}")
    private String directionsEndpoint;

    /**
     * Initializes Virtual Thread executor and rate limiters.
     * @param mapboxRestClient RestClient configured for Mapbox API
     * @param mapboxConfig Mapbox API configuration
     * @param self self-reference for cache interception
     */
    public MapboxService(@Qualifier("mapboxRestClient") RestClient mapboxRestClient, MapboxConfig mapboxConfig, @Lazy MapboxService self) {
        this.mapboxRestClient = mapboxRestClient;
        this.mapboxConfig = mapboxConfig;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.forwardGeocodingRateLimiter = RateLimiter.of("forwardGeocoding", RateLimiterConfig.custom()
                .limitForPeriod(FORWARD_GEOCODING_REQUESTS_PER_SECOND)
                .limitRefreshPeriod(Duration.ofSeconds(RATE_LIMIT_REFRESH_PERIOD_SECONDS))
                .timeoutDuration(Duration.ofSeconds(RATE_LIMITER_TIMEOUT_SECONDS))
                .build());
        this.reverseGeocodingRateLimiter = RateLimiter.of("reverseGeocoding", RateLimiterConfig.custom()
                .limitForPeriod(REVERSE_GEOCODING_REQUESTS_PER_SECOND)
                .limitRefreshPeriod(Duration.ofSeconds(RATE_LIMIT_REFRESH_PERIOD_SECONDS))
                .timeoutDuration(Duration.ofSeconds(RATE_LIMITER_TIMEOUT_SECONDS))
                .build());
        this.directionsRateLimiter = RateLimiter.of("directions", RateLimiterConfig.custom()
                .limitForPeriod(DIRECTIONS_REQUESTS_PER_SECOND)
                .limitRefreshPeriod(Duration.ofSeconds(RATE_LIMIT_REFRESH_PERIOD_SECONDS))
                .timeoutDuration(Duration.ofSeconds(RATE_LIMITER_TIMEOUT_SECONDS))
                .build());
        this.self = self;
    }

    /**
     * Converts address to coordinates using Mapbox Search Box Forward API.
     * Results cached, rate limited to 10 req/sec.
     * Searches street, address, and POI types only.
     * @param address address to geocode (e.g., "Via Roma, 10, Milano, Italy")
     * @return GeocodeResult with formatted address and coordinates
     * @throws IllegalArgumentException if address is null or blank
     * @throws InvalidAddressException if address not found
     * @throws MapboxRateLimitException if rate limit exceeded
     * @throws MapboxTimeoutException if request times out
     * @throws MapboxApiException if Mapbox returns error or malformed response
     */
    @Cacheable(value = "forwardGeocoding", key = "#address.trim().toLowerCase().replaceAll('\\s+', ' ').replaceAll('[,.]', '')", sync = true)
    public GeocodeResult geocodeAddress(String address) {
        // Defensive check
        if (address == null || address.isBlank())
            throw new IllegalArgumentException("Address cannot be null or blank");
        // Acquire rate limiter permit
        try {
            RateLimiter.waitForPermission(forwardGeocodingRateLimiter);
        } catch (RequestNotPermitted e) {
            throw new MapboxRateLimitException("Geocoding service is temporarily unavailable due to high traffic. Please try again later.", e);
        }
        // Execute Mapbox API request and handle infrastructure exceptions
        try {
            // Build the API request URL with query parameters
            String url = UriComponentsBuilder.fromPath(searchBoxForwardEndpoint)
                    .queryParam("q", address)
                    .queryParam("access_token", mapboxConfig.getApiKey())
                    .queryParam("types", "address,street,poi")
                    .queryParam("limit", 1)
                    .queryParam("language", "en")
                    .toUriString();
            // Execute HTTP GET request and deserialize JSON response
            MapboxGeocodeResponse response = mapboxRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(MapboxGeocodeResponse.class);
            // Defensive null check (should never happen with RestClient)
            if (response == null)
                throw new MapboxApiException("Received null response from Mapbox API");
            // Parse JSON response and create GeocodeResult DTO
            return response.toGeocodeResult(address);
        } catch (InvalidAddressException | MapboxApiException e) {
            // Domain exceptions - re-throw without wrapping
            throw e;
        } catch (ResourceAccessException e) {
            // Timeout or network connection failure
            throw new MapboxTimeoutException("Geocoding request timed out. Please try again.", e);
        } catch (HttpClientErrorException e) {
            // HTTP 4xx errors - special handling for rate limit (429)
            if (e.getStatusCode().value() == 429)
                throw new MapboxRateLimitException("Mapbox API rate limit exceeded. Please try again later.", e);
            throw new MapboxApiException(String.format("Mapbox API client error: %s", e.getStatusCode()), e);
        } catch (HttpServerErrorException e) {
            // HTTP 5xx errors - Mapbox server issues
            throw new MapboxApiException(String.format("Mapbox API server error: %s", e.getStatusCode()), e);
        } catch (RestClientException e) {
            // Other REST errors (JSON parsing, encoding, etc.)
            throw new MapboxApiException("Mapbox API request failed", e);
        } catch (Exception e) {
            // Safety net for unexpected errors
            throw new MapboxApiException("Unexpected error during geocoding", e);
        }
    }

    /**
     * Geocodes multiple addresses in parallel using Virtual Threads.
     * Processes in batches (GEOCODING_BATCH_SIZE) to control concurrency towards RateLimiter.
     * Chunks processed sequentially to maintain predictable resource consumption.
     * Calls geocodeAddress via self-reference to enable caching.
     * @param addresses list of addresses to geocode
     * @return list of GeocodeResult in same order as input
     * @throws IllegalArgumentException if addresses is null or empty
     * @throws InvalidAddressException if any address is invalid
     * @throws MapboxRateLimitException if rate limit exceeded
     * @throws MapboxTimeoutException if any request times out
     * @throws MapboxApiException if Mapbox returns errors or malformed responses
     */
    public List<GeocodeResult> geocodeAddresses(List<String> addresses) {
        // Defensive check
        if (addresses == null || addresses.isEmpty())
            throw new IllegalArgumentException("Addresses list cannot be null or empty");
        List<GeocodeResult> finalResults = new ArrayList<>();
        // Process in chunks to respect rate limit
        for (int i = 0; i < addresses.size(); i += GEOCODING_BATCH_SIZE) {
            int end = Math.min(i + GEOCODING_BATCH_SIZE, addresses.size());
            List<String> batch = addresses.subList(i, end);
            // Launch parallel tasks for this batch
            List<CompletableFuture<GeocodeResult>> futures = new ArrayList<>();
            for (String address : batch) {
                CompletableFuture<GeocodeResult> future = CompletableFuture.supplyAsync(() -> self.geocodeAddress(address), executorService);
                futures.add(future);
            }
            try {
                // Wait for all batch tasks to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                for (CompletableFuture<GeocodeResult> f : futures) {
                    finalResults.add(f.join());
                }
            } catch (CompletionException e) {
                // Unwrap and re-throw original exception (hides CompletableFuture implementation detail)
                Throwable cause = e.getCause();
                switch (cause) {
                    case InvalidAddressException ex -> throw ex;
                    case MapboxRateLimitException ex -> throw ex;
                    case MapboxTimeoutException ex -> throw ex;
                    case MapboxApiException ex -> throw ex;
                    case MapboxServiceException ex -> throw ex;
                    case RuntimeException ex -> throw ex;
                    case null, default -> throw new MapboxApiException("Unexpected error during batch geocoding", cause);
                }
            }
        }
        return finalResults;
    }

    /**
     * Converts coordinates to address using Mapbox Search Box Reverse API.
     * Results cached, rate limited to 10 req/sec.
     * @param coordinate geographic coordinates to reverse geocode
     * @return GeocodeResult with formatted address and coordinates
     * @throws IllegalArgumentException if coordinate is null
     * @throws InvalidCoordinateException if no address found at coordinates
     * @throws MapboxRateLimitException if rate limit exceeded
     * @throws MapboxTimeoutException if request times out
     * @throws MapboxApiException if Mapbox returns error or malformed response
     */
    @Cacheable(value = "reverseGeocoding", key = "T(java.lang.String).format('%.4f,%.4f', #coordinate.latitude, #coordinate.longitude)", sync = true)
    public GeocodeResult geocodeCoordinate(Coordinate coordinate) {
        // Defensive check
        if (coordinate == null)
            throw new IllegalArgumentException("Coordinate cannot be null");
        // Acquire rate limiter permit
        try {
            RateLimiter.waitForPermission(reverseGeocodingRateLimiter);
        } catch (RequestNotPermitted e) {
            throw new MapboxRateLimitException("Reverse geocoding service is temporarily unavailable due to high traffic. Please try again later.", e);
        }
        // Execute Mapbox API request and handle infrastructure exceptions
        try {
            // Build the API request URL with query parameters
            String url = UriComponentsBuilder.fromPath(searchBoxReverseEndpoint)
                    .queryParam("longitude", coordinate.getLongitude())
                    .queryParam("latitude", coordinate.getLatitude())
                    .queryParam("access_token", mapboxConfig.getApiKey())
                    .queryParam("types", "address,street,poi")
                    .queryParam("limit", 1)
                    .queryParam("language", "en")
                    .toUriString();
            // Execute HTTP GET request and deserialize JSON response
            MapboxGeocodeResponse response = mapboxRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(MapboxGeocodeResponse.class);
            // Defensive null check
            if (response == null)
                throw new MapboxApiException("Received null response from Mapbox API");
            // Parse JSON response and create GeocodeResult DTO
            return response.toGeocodeResult(coordinate);
        } catch (InvalidCoordinateException | MapboxApiException e) {
            // Domain exceptions - re-throw without wrapping
            throw e;
        } catch (ResourceAccessException e) {
            // Timeout or network connection failure
            throw new MapboxTimeoutException("Geocoding request timed out. Please try again.", e);
        } catch (HttpClientErrorException e) {
            // HTTP 4xx errors - special handling for rate limit (429)
            if (e.getStatusCode().value() == 429)
                throw new MapboxRateLimitException("Mapbox API rate limit exceeded. Please try again later.", e);
            throw new MapboxApiException(String.format("Mapbox API client error: %s", e.getStatusCode()), e);
        } catch (HttpServerErrorException e) {
            // HTTP 5xx errors - Mapbox server issues
            throw new MapboxApiException(String.format("Mapbox API server error: %s", e.getStatusCode()), e);
        } catch (RestClientException e) {
            // Other REST errors (JSON parsing, encoding, etc.)
            throw new MapboxApiException("Mapbox API request failed", e);
        } catch (Exception e) {
            // Safety net for unexpected errors
            throw new MapboxApiException("Unexpected error during reverse geocoding", e);
        }
    }

    /**
     * Calculates cycling route through multiple waypoints using Mapbox Directions API.
     * Rate limited to 5 req/sec. Handles >25 waypoints by splitting into multiple API calls.
     * Delegates to calculateSingleRoute via self-reference for chunk-level caching.
     * @param waypoints ordered coordinates defining route (min 2, no max)
     * @return CyclingRouteResult with all route coordinates and total distance
     * @throws IllegalArgumentException if waypoints is null or has fewer than 2 points
     * @throws InvalidRouteException if no route can be calculated
     * @throws MapboxRateLimitException if rate limit exceeded
     * @throws MapboxTimeoutException if request times out
     * @throws MapboxApiException if Mapbox returns error or malformed response
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
     * Calculates route for single set of waypoints (≤25).
     * INTERNAL USE ONLY - public for Spring AOP @Cacheable to work.
     * External services should use calculateCyclingRoute.
     * Cached at chunk level, allowing route segments to be reused across different routes.
     * Must be called via Spring proxy (self-reference) to enable caching.
     * Rate limited to 5 req/sec.
     * @param waypoints coordinates (2-25 waypoints)
     * @return cycling route with coordinates and distance
     * @throws IllegalArgumentException if waypoints invalid or no route found
     * @throws MapboxRateLimitException if rate limit exceeded
     * @throws MapboxTimeoutException if request times out
     * @throws MapboxApiException if Mapbox service unavailable
     */
    @Cacheable(value = "cyclingRoute", key = "#waypoints", sync = true)
    public CyclingRouteResult calculateSingleRoute(List<Coordinate> waypoints) {
        // Defensive check
        if (waypoints == null || waypoints.size() < 2 || waypoints.size() > MAX_WAYPOINTS_PER_REQUEST)
            throw new IllegalArgumentException(String.format("Waypoints must be between 2 and %d and cannot be null", MAX_WAYPOINTS_PER_REQUEST));
        // Acquire rate limiter permit
        try {
            RateLimiter.waitForPermission(directionsRateLimiter);
        } catch (RequestNotPermitted e) {
            throw new MapboxRateLimitException("Routing service is temporarily unavailable due to high traffic. Please try again later.", e);
        }
        // Execute Mapbox API request and handle infrastructure exceptions
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
            // Execute HTTP GET request and deserialize JSON response
            MapboxDirectionsResponse response = mapboxRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(MapboxDirectionsResponse.class);
            // Defensive null check
            if (response == null)
                throw new MapboxApiException("Received null response from Mapbox Directions API");
            // Parse JSON response and create CyclingRouteResult DTO
            return response.toCyclingRouteResult();
        } catch (InvalidRouteException | MapboxApiException e) {
            // Domain exceptions - re-throw without wrapping
            throw e;
        } catch (ResourceAccessException e) {
            // Timeout or network connection failure
            throw new MapboxTimeoutException("Routing request timed out. Please try again.", e);
        } catch (HttpClientErrorException e) {
            // HTTP 4xx errors - special handling for rate limit (429)
            if (e.getStatusCode().value() == 429)
                throw new MapboxRateLimitException("Mapbox API rate limit exceeded. Please try again later.", e);
            throw new MapboxApiException(String.format("Mapbox API client error: %s", e.getStatusCode()), e);
        } catch (HttpServerErrorException e) {
            // HTTP 5xx errors - Mapbox server issues
            throw new MapboxApiException(String.format("Mapbox API server error: %s", e.getStatusCode()), e);
        } catch (RestClientException e) {
            // Other REST errors (JSON parsing, encoding, etc.)
            throw new MapboxApiException("Mapbox API request failed", e);
        } catch (Exception e) {
            // Safety net for unexpected errors
            throw new MapboxApiException("Unexpected error during route calculation", e);
        }
    }

    /**
     * Calculates route for waypoints exceeding 25-waypoint limit.
     * Splits into overlapping chunks and combines results.
     * Each chunk calculated via self-reference to enable caching.
     * @param waypoints coordinates (>25 waypoints)
     * @return combined cycling route result
     */
    private CyclingRouteResult calculateMultiChunkRoute(List<Coordinate> waypoints) {
        List<Coordinate> allRouteCoordinates = new ArrayList<>();
        double totalDistance = 0.0;
        int start = 0;
        while (start < waypoints.size() - 1) {
            // Determine end index for this chunk (max 25 waypoints)
            int end = Math.min(start + MAX_WAYPOINTS_PER_REQUEST, waypoints.size());
            // Extract chunk of waypoints
            List<Coordinate> chunk = waypoints.subList(start, end);
            // Calculate route for this chunk
            CyclingRouteResult chunkResult = self.calculateSingleRoute(chunk);
            // Add coordinates to result (avoid duplicates on overlap)
            if (allRouteCoordinates.isEmpty()) {
                // First chunk: add all coordinates
                allRouteCoordinates.addAll(chunkResult.routeCoordinates());
            } else {
                List<Coordinate> chunkCoords = chunkResult.routeCoordinates();
                // Skip first point to avoid duplication
                allRouteCoordinates.addAll(chunkCoords.subList(1, chunkCoords.size()));
            }
            totalDistance += chunkResult.distanceInMeters();
            // Move to next chunk (overlap last waypoint)
            start = end - 1;
        }
        return new CyclingRouteResult(allRouteCoordinates, totalDistance);
    }

}