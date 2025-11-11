package it.polimi.se.bbp.config.mapbox;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration class for Mapbox API integration.
 * Provides a configured RestClient bean for making HTTP requests to Mapbox services.
 */
@Configuration
@Getter
public class MapboxConfig {

    /**
     * The Mapbox API access token used for authentication.
     * Injected from application.properties via the property 'mapbox.api.key'.
     */
    @Value("${mapbox.api.key}")
    private String apiKey;

    /**
     * The base URL for Mapbox API endpoints.
     * Injected from application.properties via the property 'mapbox.api.base-url'.
     */
    @Value("${mapbox.api.base-url}")
    private String baseUrl;

    /**
     * Connection and read timeout in milliseconds for HTTP requests to Mapbox API.
     * Injected from application.properties via the property 'mapbox.api.timeout'.
     */
    @Value("${mapbox.api.timeout:10000}")
    private int timeout;

    /**
     * Creates and configures a RestClient bean specifically for Mapbox API calls.
     * The client is pre-configured with:
     * - Base URL pointing to Mapbox API
     * - API key as query parameter for authentication
     * - Connection and read timeouts
     * - Default headers for JSON content
     * @return a configured RestClient instance for Mapbox API
     */
    @Bean(name = "mapboxRestClient")
    public RestClient mapboxRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Creates a ClientHttpRequestFactory with configured timeouts.
     * This factory is used by RestClient to create HTTP connections.
     * @return configured ClientHttpRequestFactory
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeout));
        factory.setReadTimeout(Duration.ofMillis(timeout));
        return factory;
    }

}