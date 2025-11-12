package it.polimi.se.bbp.mapper.openmeteo;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.se.bbp.dto.openmeteo.OpenMeteoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for creating OpenMeteoResponse DTOs from Open-Meteo Forecast API JSON responses.
 * Handles JSON parsing and validation of weather data responses.
 */
@Component
@RequiredArgsConstructor
public class OpenMeteoResponseMapper {

    /**
     * Jackson ObjectMapper for parsing JSON responses from Open-Meteo API.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses an Open-Meteo Forecast API JSON response and creates an OpenMeteoResponse DTO.
     * Exception handling strategy:
     * - IllegalArgumentException: Invalid response (no data, missing fields) → 400 BAD_REQUEST
     * - IllegalStateException: Open-Meteo service issues (malformed JSON, parsing errors) → 503 SERVICE_UNAVAILABLE
     * - Other exceptions: Unexpected errors in our code → 500 INTERNAL_SERVER_ERROR
     * @param jsonResponse the JSON response string from Open-Meteo Forecast API
     * @return the parsed OpenMeteoResponse DTO with hourly weather data
     * @throws IllegalArgumentException if the response contains no weather data or invalid data
     * @throws IllegalStateException if Open-Meteo service has issues (parsing errors, missing fields)
     */
    public OpenMeteoResponse fromJsonResponse(String jsonResponse) {
        try {
            // Parse JSON response into OpenMeteoResponse object
            OpenMeteoResponse response = objectMapper.readValue(jsonResponse, OpenMeteoResponse.class);
            // Validate that response contains required data
            validateResponse(response);
            return response;
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors (invalid/missing data) → 400 BAD_REQUEST
            throw e;
        } catch (Exception e) {
            // Wrap technical errors (JSON parsing, missing fields, etc.) → 503 SERVICE_UNAVAILABLE
            throw new IllegalStateException("Open-Meteo weather service is currently unavailable", e);
        }
    }

    /**
     * Validates that the OpenMeteoResponse contains all required weather data.
     * @param response the parsed OpenMeteoResponse object
     * @throws IllegalArgumentException if required data is missing or empty
     */
    private void validateResponse(OpenMeteoResponse response) {
        // Check if response has hourly data
        if (response.getHourlyData() == null)
            throw new IllegalArgumentException("No weather data available for the requested location and date");
        OpenMeteoResponse.HourlyData hourlyData = response.getHourlyData();
        // Check if all required arrays are present and not empty
        if (hourlyData.getTime() == null || hourlyData.getTime().isEmpty())
            throw new IllegalArgumentException("No time data available in weather response");
        if (hourlyData.getTemperatureValues() == null || hourlyData.getTemperatureValues().isEmpty())
            throw new IllegalArgumentException("No temperature data available in weather response");
        if (hourlyData.getHumidityValues() == null || hourlyData.getHumidityValues().isEmpty())
            throw new IllegalArgumentException("No humidity data available in weather response");
        if (hourlyData.getWindSpeedValues() == null || hourlyData.getWindSpeedValues().isEmpty())
            throw new IllegalArgumentException("No wind speed data available in weather response");
        if (hourlyData.getWeatherCodeValues() == null || hourlyData.getWeatherCodeValues().isEmpty())
            throw new IllegalArgumentException("No weather condition data available in weather response");
        // Validate that all arrays have the same length
        int expectedLength = hourlyData.getTime().size();
        if (hourlyData.getTemperatureValues().size() != expectedLength ||
                hourlyData.getHumidityValues().size() != expectedLength ||
                hourlyData.getWindSpeedValues().size() != expectedLength ||
                hourlyData.getWeatherCodeValues().size() != expectedLength) {
            throw new IllegalArgumentException("Inconsistent weather data arrays in response");
        }
    }

}