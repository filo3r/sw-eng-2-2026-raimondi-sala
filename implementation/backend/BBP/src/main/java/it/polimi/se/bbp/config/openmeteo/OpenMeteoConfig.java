package it.polimi.se.bbp.config.openmeteo;

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
 * Configuration for Open-Meteo API integration.
 * Sets up RestClient for the free weather API service.
 * Uses Forecast API which provides historical data for the last 3 months.
 * All timestamps are returned in UTC timezone.
 */
@Configuration
@Getter
public class OpenMeteoConfig {

    /**
     * Base URL for Open-Meteo Forecast API.
     */
    @Value("${open-meteo.api.base-url}")
    private String baseUrl;

    /**
     * Endpoint path for forecast and historical weather data.
     */
    @Value("${open-meteo.api.forecast.endpoint}")
    private String forecastEndpoint;

    /**
     * Timezone parameter for API requests, set to UTC for consistent timestamps.
     */
    @Value("${open-meteo.api.timezone}")
    private String timezone;

    /**
     * Connection and read timeout in milliseconds for Open-Meteo API requests.
     */
    @Value("${open-meteo.api.timeout}")
    private int timeout;

    /**
     * Creates a RestClient configured for Open-Meteo API calls.
     * Includes base URL, timeouts, and JSON accept header.
     * No authentication required as Open-Meteo is a free service.
     * @return configured RestClient for Open-Meteo API
     */
    @Bean(name = "openMeteoRestClient")
    public RestClient openMeteoRestClient() {
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