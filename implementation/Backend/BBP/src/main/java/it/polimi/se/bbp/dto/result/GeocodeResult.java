package it.polimi.se.bbp.dto.result;

import it.polimi.se.bbp.geo.Coordinate;

import java.util.List;

/**
 * Result of a geocoding operation containing formatted address and coordinates.
 * @param address formatted address returned by geocoding service (standardized)
 * @param coordinate geographic coordinates of the address
 */
public record GeocodeResult(

        /*
         * Formatted address from geocoding service.
         * Complete, standardized address string.
         */
        String address,

        /*
         * Geographic coordinates of the address.
         */
        Coordinate coordinate

) {

    /**
     * Extracts coordinates from a list of geocode results.
     * Useful for converting geocoding output to routing input.
     * @param geocodeResults list of geocode results
     * @return list of coordinates (empty if input is null)
     */
    public static List<Coordinate> extractCoordinates(List<GeocodeResult> geocodeResults) {
        if (geocodeResults == null)
            return List.of();
        return geocodeResults.stream()
                .map(GeocodeResult::coordinate)
                .toList();
    }

}