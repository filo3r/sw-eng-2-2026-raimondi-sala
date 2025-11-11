package it.polimi.se.bbp.mapper.mapbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.GeocodeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for creating GeocodeResult DTOs from Mapbox Search Box Forward API responses.
 */
@Component
@RequiredArgsConstructor
public class GeocodeResultMapper {

    /**
     * Mapper for converting latitude and longitude values into Coordinate DTOs.
     */
    private final CoordinateMapper coordinateMapper;

    /**
     * Jackson ObjectMapper for parsing JSON responses from Mapbox API.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses a Mapbox Search Box Forward API JSON response and creates a GeocodeResult DTO.
     * Exception handling strategy:
     * - IllegalArgumentException: Invalid address (no results found) → 400 BAD_REQUEST
     * - IllegalStateException: Mapbox service issues (malformed JSON, missing fields) → 503 SERVICE_UNAVAILABLE
     * - Other exceptions: Unexpected errors in our code → 500 INTERNAL_SERVER_ERROR
     * @param jsonResponse the JSON response string from Mapbox Search Box Forward API
     * @return the geocode result DTO
     * @throws IllegalArgumentException if no results are found (invalid address)
     * @throws IllegalStateException if Mapbox service has issues (parsing errors, missing data)
     */
    public GeocodeResult fromJsonResponse(String jsonResponse) {
        try {
            // Parse JSON response
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode features = rootNode.get("features");
            // Validation: Check if address was found
            if (features == null || features.isEmpty())
                throw new IllegalArgumentException("No results found for the provided address");
            // Extract first (most relevant) result
            JsonNode firstFeature = features.get(0);
            // Extract address from full_address field
            String address = firstFeature.get("properties").get("full_address").asText();
            // Extract coordinates from geometry
            // Mapbox returns coordinates as [longitude, latitude] array
            JsonNode coordinatesNode = firstFeature.get("geometry").get("coordinates");
            double longitude = coordinatesNode.get(0).asDouble();
            double latitude = coordinatesNode.get(1).asDouble();
            return toGeocodeResult(address, latitude, longitude);
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors (invalid address) → 400 BAD_REQUEST
            throw e;
        } catch (Exception e) {
            // Wrap technical errors (JSON parsing, missing fields, etc.) → 503 SERVICE_UNAVAILABLE
            throw new IllegalStateException("Mapbox geocoding service is currently unavailable", e);
        }
    }

    /**
     * Creates a GeocodeResult DTO from address and coordinate values.
     * @param address the formatted address string
     * @param latitude the latitude in decimal degrees
     * @param longitude the longitude in decimal degrees
     * @return the geocode result DTO
     */
    private GeocodeResult toGeocodeResult(String address, Double latitude, Double longitude) {
        Coordinate coordinate = coordinateMapper.toCoordinate(latitude, longitude);
        return GeocodeResult.builder()
                .address(address)
                .coordinate(coordinate)
                .build();
    }

}