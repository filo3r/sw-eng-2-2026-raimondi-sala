package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request for searching bike paths within geographic radius.
 * Finds published bike paths where origin and destination are within
 * specified distances from search addresses.
 * @param originAddress origin address (will be geocoded)
 * @param originRadiusKm radius around origin in kilometers
 * @param destinationAddress destination address (will be geocoded)
 * @param destinationRadiusKm radius around destination in kilometers
 */
public record BikePathFinderRequest(

        /*
         * Origin address (will be geocoded).
         */
        @NotBlank(message = "Origin address is required")
        String originAddress,

        /*
         * Radius around origin in kilometers.
         */
        @Positive(message = "Origin radius must be positive")
        Double originRadiusKm,

        /*
         * Destination address (will be geocoded).
         */
        @NotBlank(message = "Destination address is required")
        String destinationAddress,

        /*
         * Radius around destination in kilometers.
         */
        @Positive(message = "Destination radius must be positive")
        Double destinationRadiusKm

) {}