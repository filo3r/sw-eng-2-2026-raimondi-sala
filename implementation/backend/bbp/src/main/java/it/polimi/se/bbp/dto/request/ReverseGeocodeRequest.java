package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.geo.Coordinate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request for reverse geocoding (coordinates to address).
 * @param coordinate geographic coordinates to convert to address
 */
public record ReverseGeocodeRequest(

        /*
         * Geographic coordinates to convert to address.
         * Coordinate object contains latitude and longitude with built-in validations.
         */
        @NotNull(message = "Coordinate cannot be null")
        @Valid
        Coordinate coordinate

) {}