package it.polimi.se.bbp.exception.openmeteo;

/**
 * Thrown when weather data is not available for requested time period.
 * Occurs when trip is older than supported historical range (> 90 days)
 * or no weather data exists for specified time and location.
 * Should result in HTTP 400 Bad Request.
 */
public class WeatherDataNotAvailableException extends OpenMeteoClientException {

    /**
     * Constructs exception for unavailable weather data.
     * @param message detail message explaining why data is unavailable
     */
    public WeatherDataNotAvailableException(String message) {
        super(message);
    }

    /**
     * Constructs exception for unavailable weather data with cause.
     * @param message detail message explaining why data is unavailable
     * @param cause underlying cause of this exception
     */
    public WeatherDataNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

}