package it.polimi.se.bbp.config.mapbox;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for Mapbox API integration.
 * Sets up RestClient with authentication and timeout settings.
 */
@Configuration
@Getter
public class MapboxConfig {

    /**
     * Mapbox API access token for authentication.
     */
    @Value("${mapbox.api.key}")
    private String apiKey;

    /**
     * Base URL for Mapbox API endpoints.
     */
    @Value("${mapbox.api.base-url}")
    private String baseUrl;

    /**
     * Connection and read timeout in milliseconds for Mapbox API requests.
     */
    @Value("${mapbox.api.timeout}")
    private int timeout;

    /**
     * Creates a RestClient configured for Mapbox API calls.
     * Includes base URL, timeouts, and JSON accept header.
     * @return configured RestClient for Mapbox API
     */
    @Bean(name = "mapboxRestClient")
    public RestClient mapboxRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Creates a request factory with configured timeouts.
     * @return ClientHttpRequestFactory with connection and read timeouts
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeout));
        factory.setReadTimeout(Duration.ofMillis(timeout));
        return factory;
    }

}