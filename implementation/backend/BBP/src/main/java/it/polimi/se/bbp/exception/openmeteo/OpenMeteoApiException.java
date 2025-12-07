package it.polimi.se.bbp.exception.openmeteo;

/**
 * Thrown when Open-Meteo API returns error or malformed response.
 * Includes HTTP 4xx/5xx errors, JSON parsing errors, or missing/invalid data.
 * Should result in HTTP 503 Service Unavailable.
 */
public class OpenMeteoApiException extends OpenMeteoServiceException {

    /**
     * Constructs API exception with detail message.
     * @param message detail message explaining the API error
     */
    public OpenMeteoApiException(String message) {
        super(message);
    }

    /**
     * Constructs API exception with detail message and cause.
     * @param message detail message explaining the API error
     * @param cause underlying cause of this exception
     */
    public OpenMeteoApiException(String message, Throwable cause) {
        super(message, cause);
    }

}