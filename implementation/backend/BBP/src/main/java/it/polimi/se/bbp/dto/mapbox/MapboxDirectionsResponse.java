package it.polimi.se.bbp.dto.mapbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.polimi.se.bbp.exception.mapbox.InvalidRouteException;
import it.polimi.se.bbp.exception.mapbox.MapboxApiException;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Raw JSON response from Mapbox Directions API.
 * Maps essential fields: status code, routes with geometry and distance.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MapboxDirectionsResponse(

        /* Response status code. "Ok" indicates success. */
        @JsonProperty("code")
        String code,

        /* List of routes found. Usually contains only one route. */
        @JsonProperty("routes")
        List<Route> routes

) {

    /**
     * Single route with geometry and distance.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(

            /* Route geometry containing the path coordinates. */
            @JsonProperty("geometry")
            Geometry geometry,

            /* Total route distance in meters. */
            @JsonProperty("distance")
            Double distance

    ) {}

    /**
     * GeoJSON LineString geometry containing route coordinates.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(

            /* List of coordinate pairs [longitude, latitude] forming the route path. */
            @JsonProperty("coordinates")
            List<List<Double>> coordinates

    ) {}

    /**
     * Validates response and extracts cycling route result.
     * Performs strict validation to distinguish user errors from technical errors.
     * Converts Mapbox format [lon, lat] to domain Coordinate objects.
     * @return validated cycling route with path and total distance
     * @throws InvalidRouteException if waypoints cannot be connected or route not found
     * @throws MapboxApiException if API response is malformed or missing required data
     */
    public CyclingRouteResult toCyclingRouteResult() {
        // Validate response code first (distinguishes user errors from technical errors)
        validateResponseCode();
        // Validate that routes array exists and is not empty
        if (routes == null || routes.isEmpty())
            throw new MapboxApiException("No route found in Mapbox API response");
        // Extract primary route and validate structure
        Route primaryRoute = routes.getFirst();
        if (primaryRoute == null || primaryRoute.geometry() == null)
            throw new MapboxApiException("Missing route geometry from Mapbox API");
        // Validate coordinates array exists and has minimum required points
        List<List<Double>> coordinates = primaryRoute.geometry().coordinates();
        if (coordinates == null || coordinates.size() < 2)
            throw new MapboxApiException("Invalid or insufficient route coordinates");
        // Convert from Mapbox format [lon, lat] to domain Coordinate objects
        List<Coordinate> path = coordinates.stream()
                .map(this::convertCoordinate)
                .collect(Collectors.toList());
        // Validate distance is present and non-negative
        Double distance = primaryRoute.distance();
        if (distance == null || distance < 0)
            throw new MapboxApiException("Invalid route distance from Mapbox API");
        return new CyclingRouteResult(path, distance);
    }

    /**
     * Validates Mapbox API response code.
     * Distinguishes user errors (NoRoute, NoSegment, InvalidInput) from technical errors.
     * @throws InvalidRouteException if waypoints are invalid or cannot be connected
     * @throws MapboxApiException if response code is missing or unexpected
     */
    private void validateResponseCode() {
        if (code == null || code.isBlank())
            throw new MapboxApiException("Missing response code from Mapbox API");
        switch (code) {
            case "Ok" -> {}
            case "NoRoute" -> throw new InvalidRouteException("No cycling route found between the provided waypoints");
            case "NoSegment" -> throw new InvalidRouteException("Cannot connect consecutive waypoints");
            case "InvalidInput" -> throw new InvalidRouteException("Invalid waypoint coordinates or parameters");
            default -> throw new MapboxApiException("Unexpected Mapbox API response: " + code);
        }
    }

    /**
     * Converts coordinate pair from Mapbox format to domain Coordinate.
     * Mapbox uses GeoJSON format [longitude, latitude].
     * Validates coordinate values are within valid ranges.
     * @param pair coordinate pair [longitude, latitude]
     * @return domain Coordinate object
     * @throws MapboxApiException if coordinate format is invalid or values out of range
     */
    private Coordinate convertCoordinate(List<Double> pair) {
        if (pair == null || pair.size() < 2)
            throw new MapboxApiException("Invalid coordinate format in API response");
        Double lon = pair.get(0);
        Double lat = pair.get(1);
        if (lon == null || lat == null)
            throw new MapboxApiException("Null coordinate values in API response");
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0)
            throw new MapboxApiException("Invalid coordinates from API");
        return new Coordinate(lat, lon);
    }

}