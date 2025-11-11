package it.polimi.se.bbp.mapper.mapbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.se.bbp.dto.mapbox.Coordinate;
import it.polimi.se.bbp.dto.mapbox.CyclingRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for creating CyclingRouteResult DTOs from Mapbox Directions API responses.
 */
@Component
@RequiredArgsConstructor
public class CyclingRouteResultMapper {

    /**
     * Mapper for converting latitude and longitude values into Coordinate DTOs.
     */
    private final CoordinateMapper coordinateMapper;

    /**
     * Jackson ObjectMapper for parsing JSON responses from Mapbox API.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses a Mapbox Directions API JSON response and creates a CyclingRouteResult DTO.
     * Exception handling strategy:
     * - IllegalArgumentException: Invalid waypoints (no route found, no segment) → 400 BAD_REQUEST
     * - IllegalStateException: Mapbox service issues (malformed JSON, missing fields) → 503 SERVICE_UNAVAILABLE
     * @param jsonResponse the JSON response string from Mapbox Directions API
     * @return the cycling route result DTO
     * @throws IllegalArgumentException if no route can be found for the waypoints
     * @throws IllegalStateException if Mapbox service has issues
     */
    public CyclingRouteResult fromJsonResponse(String jsonResponse) {
        try {
            // Parse JSON response
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            // Check response code for errors
            String code = rootNode.has("code") ? rootNode.get("code").asText() : null;
            // Handle error codes
            if ("NoRoute".equals(code))
                throw new IllegalArgumentException("No cycling route found between the provided waypoints");
            if ("NoSegment".equals(code))
                throw new IllegalArgumentException("No route segment found between consecutive waypoints");
            if (code != null && !"Ok".equals(code))
                throw new IllegalArgumentException("Invalid waypoints: " + code);
            // Extract routes array
            JsonNode routes = rootNode.get("routes");
            if (routes == null || routes.isEmpty())
                throw new IllegalArgumentException("No route found for the given waypoints");
            // Extract first route
            JsonNode firstRoute = routes.get(0);
            double distanceInMeters = firstRoute.get("distance").asDouble();
            // Extract geometry coordinates
            JsonNode geometry = firstRoute.get("geometry");
            JsonNode coordinatesNode = geometry.get("coordinates");
            // Parse route coordinates
            List<Coordinate> routeCoordinates = parseRouteCoordinates(coordinatesNode);
            return toCyclingRouteResult(routeCoordinates, distanceInMeters);
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors (invalid waypoints) → 400 BAD_REQUEST
            throw e;
        } catch (Exception e) {
            // Wrap technical errors (JSON parsing, missing fields, etc.) → 503 SERVICE_UNAVAILABLE
            throw new IllegalStateException("Mapbox routing service is currently unavailable", e);
        }
    }

    /**
     * Creates a CyclingRouteResult DTO from route coordinates and distance.
     * @param routeCoordinates the list of coordinates forming the route
     * @param distanceInMeters the total distance in meters
     * @return the cycling route result DTO
     */
    public CyclingRouteResult toCyclingRouteResult(List<Coordinate> routeCoordinates, Double distanceInMeters) {
        return CyclingRouteResult.builder()
                .routeCoordinates(routeCoordinates)
                .distanceInMeters(distanceInMeters)
                .build();
    }

    /**
     * Parses route coordinates from a Mapbox JSON coordinates array node.
     * Mapbox returns coordinates as [longitude, latitude] array.
     * @param coordinatesNode the JSON node containing the coordinates array
     * @return the list of coordinate DTOs
     */
    private List<Coordinate> parseRouteCoordinates(JsonNode coordinatesNode) {
        List<Coordinate> routeCoordinates = new ArrayList<>();
        for (JsonNode coordNode : coordinatesNode) {
            // Mapbox returns coordinates as [longitude, latitude]
            double longitude = coordNode.get(0).asDouble();
            double latitude = coordNode.get(1).asDouble();
            routeCoordinates.add(coordinateMapper.toCoordinate(latitude, longitude));
        }
        return routeCoordinates;
    }

}