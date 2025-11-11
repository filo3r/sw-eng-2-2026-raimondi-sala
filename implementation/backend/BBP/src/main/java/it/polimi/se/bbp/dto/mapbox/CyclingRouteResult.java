package it.polimi.se.bbp.dto.mapbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a route calculation operation.
 * Contains the detailed route geometry and total distance.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CyclingRouteResult {

    /**
     * Ordered list of coordinates that form the complete route.
     * Each coordinate represents a point along the path.
     */
    private List<Coordinate> routeCoordinates;

    /**
     * Total distance of the route in meters.
     */
    private Double distanceInMeters;

}