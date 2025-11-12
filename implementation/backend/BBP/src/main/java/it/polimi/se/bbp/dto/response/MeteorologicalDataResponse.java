package it.polimi.se.bbp.dto.response;

import it.polimi.se.bbp.enums.openmeteo.WeatherCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for MeteorologicalData response.
 * Contains weather information associated with a trip.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MeteorologicalDataResponse {

    /**
     * The weather condition during the trip.
     * Enum representing the overall weather state (e.g., CLEAR_SKY, RAIN, SNOW).
     */
    private WeatherCondition weatherCondition;

    /**
     * The weather condition description in human-readable format.
     * Example: "Clear sky", "Rain: Moderate intensity"
     */
    private String weatherDescription;

    /**
     * The ambient temperature during the trip in degrees Celsius.
     * Range: -99.9 to +99.9
     */
    private Double temperature;

    /**
     * The relative humidity during the trip as a percentage.
     * Range: 0 to 100
     */
    private Integer humidity;

    /**
     * The wind speed during the trip in km/h.
     * Range: 0.0 to 999.9
     */
    private Double windSpeed;

}