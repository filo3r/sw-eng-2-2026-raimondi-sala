package it.polimi.se.bbp.exception.mapbox;

/**
 * Thrown when Mapbox API returns unexpected error or malformed response.
 * Covers technical problems: HTTP 4xx/5xx errors (except rate limiting),
 * malformed JSON, or unexpected API behavior.
 * Should result in HTTP 503 Service Unavailable or HTTP 502 Bad Gateway.
 */
public class MapboxApiException extends MapboxServiceException {

    /**
     * Constructs API exception with detail message.
     * @param message detail message explaining the API error
     */
    public MapboxApiException(String message) {
        super(message);
    }

    /**
     * Constructs API exception with detail message and cause.
     * @param message detail message explaining the API error
     * @param cause underlying cause of this exception
     */
    public MapboxApiException(String message, Throwable cause) {
        super(message, cause);
    }

}