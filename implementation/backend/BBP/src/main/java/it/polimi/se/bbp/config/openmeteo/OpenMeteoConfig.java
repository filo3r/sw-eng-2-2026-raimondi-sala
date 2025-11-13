package it.polimi.se.bbp.config.openmeteo;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration class for Open-Meteo API integration.
 * Provides a configured RestClient bean for making HTTP requests to Open-Meteo weather services.
 * Open-Meteo is a free, open-source weather API that doesn't require authentication.
 * Uses the Forecast API which can retrieve both forecast and recent historical data.
 * Historical data is available for approximately the last 3 months.
 * Timezone handling: All API calls use 'timezone=auto' parameter, which automatically
 * determines the correct timezone based on the provided latitude and longitude coordinates.
 * This ensures weather data is in the local timezone of the trip location.
 */
@Configuration
@Getter
public class OpenMeteoConfig {

    /**
     * The base URL for Open-Meteo Forecast API endpoints.
     * Injected from application.properties via the property 'open-meteo.api.base-url'.
     */
    @Value("${open-meteo.api.base-url}")
    private String baseUrl;

    /**
     * The endpoint path for forecast weather data (can retrieve historical data for recent dates).
     * Injected from application.properties via the property 'open-meteo.api.forecast.endpoint'.
     */
    @Value("${open-meteo.api.forecast.endpoint}")
    private String forecastEndpoint;

    /**
     * The timezone parameter for weather data requests.
     * Injected from application.properties via the property 'open-meteo.api.timezone'.
     * Default value is 'auto', which automatically determines the timezone based on coordinates.
     */
    @Value("${open-meteo.api.timezone}")
    private String timezone;

    /**
     * Connection and read timeout in milliseconds for HTTP requests to Open-Meteo API.
     * Injected from application.properties via the property 'open-meteo.api.timeout'.
     */
    @Value("${open-meteo.api.timeout}")
    private int timeout;

    /**
     * Creates and configures a RestClient bean specifically for Open-Meteo API calls.
     * The client is pre-configured with:
     * - Base URL pointing to Open-Meteo API
     * - Connection and read timeouts
     * - Default headers for JSON content
     * Note: Open-Meteo is free and doesn't require API keys or authentication.
     * @return a configured RestClient instance for Open-Meteo API
     */
    @Bean(name = "openMeteoRestClient")
    public RestClient openMeteoRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Creates a ClientHttpRequestFactory with configured timeouts.
     * This factory is used by RestClient to create HTTP connections.
     * @return configured ClientHttpRequestFactory with timeout settings
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeout));
        factory.setReadTimeout(Duration.ofMillis(timeout));
        return factory;
    }

}