package it.polimi.se.bbp.dto.mapbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a geocoding operation.
 * Contains the formatted address and its geographic coordinates.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GeocodeResult {

    /**
     * The formatted address returned by the geocoding service.
     * This is the complete, standardized address string.
     */
    private String address;

    /**
     * The geographic coordinates of the address.
     */
    private Coordinate coordinate;

}