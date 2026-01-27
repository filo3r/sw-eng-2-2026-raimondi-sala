package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request for forward geocoding (address to coordinates).
 * @param address address to geocode
 */
public record ForwardGeocodeRequest(

        /*
         * Address to convert to coordinates.
         * Must be a valid location string.
         */
        @NotNull(message = "Address cannot be null")
        @NotBlank(message = "Address is required")
        @Size(max = 256, message = "Address must not exceed 256 characters")
        String address

) {}