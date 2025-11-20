package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching bike paths within geographic radius.
 * Finds published bike paths where origin and destination are within
 * specified distances from search addresses.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BikePathSearchRequest {

    /**
     * Origin address (will be geocoded).
     * Example: "Piazza Duomo, Milano, Italy"
     */
    @NotBlank(message = "Origin address is required")
    private String originAddress;

    /**
     * Radius around origin in kilometers.
     * Example: 0.5 (500 meters)
     */
    @Positive(message = "Origin radius must be positive")
    private Double originRadiusKm;

    /**
     * Destination address (will be geocoded).
     * Example: "Stazione Centrale, Milano, Italy"
     */
    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    /**
     * Radius around destination in kilometers.
     * Example: 1.0 (1 kilometer)
     */
    @Positive(message = "Destination radius must be positive")
    private Double destinationRadiusKm;

}