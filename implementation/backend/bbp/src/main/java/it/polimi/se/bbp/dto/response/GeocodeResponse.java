package it.polimi.se.bbp.dto.response;

/**
 * Response for geocoding operations (forward and reverse).
 * Contains formatted address and geographic coordinates.
 * @param address formatted address from geocoding service
 * @param latitude latitude in decimal degrees (-90 to +90)
 * @param longitude longitude in decimal degrees (-180 to +180)
 */
public record GeocodeResponse(

        /*
         * Formatted address from geocoding service.
         * Complete, standardized address string.
         */
        String address,

        /*
         * Latitude in decimal degrees.
         */
        Double latitude,

        /*
         * Longitude in decimal degrees.
         */
        Double longitude

) {}