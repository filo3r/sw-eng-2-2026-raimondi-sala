package it.polimi.se.bbp.dto.request;

import it.polimi.se.bbp.enums.BikePathStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for manually creating a bike path by providing addresses.
 * Contains the user-provided data needed to create a bike path,
 * including addresses that will be geocoded and used to calculate the cycling route,
 * and optional obstacles along the path.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BikePathManualCreateRequest {

    /**
     * Ordered list of addresses that define the bike path route.
     * The first address is the origin, the last is the destination.
     * Intermediate addresses are waypoints.
     * This field is required and must contain at least 2 addresses.
     * Each address will be geocoded to obtain coordinates, and a cycling route
     * will be calculated through all waypoints.
     */
    @NotNull(message = "Addresses list is required")
    @NotEmpty(message = "At least 2 addresses are required (origin and destination)")
    @Size(min = 2, message = "At least 2 addresses are required (origin and destination)")
    private List<@NotBlank(message = "Address cannot be blank") @Size(max = 256, message = "Address must not exceed 256 characters") String> addresses;

    /**
     * Optional description or notes about the bike path.
     * Maximum length is 500 characters.
     * This can include information about the route characteristics,
     * points of interest, or any other relevant details.
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * The current condition status of the bike path.
     * This field is required and indicates the maintenance level and usability.
     * Possible values include: EXCELLENT, GOOD, SUFFICIENT, POOR, UNDER_MAINTENANCE, etc.
     * The status affects the bike path score calculation.
     */
    @NotNull(message = "Status is required")
    private BikePathStatus status;

    /**
     * Flag indicating whether this bike path should be published and visible to all users.
     * This field is required.
     * When true, the path is visible to all users and can be updated by anyone.
     * When false, the path is only visible to its creator and can only be updated by the creator.
     */
    @NotNull(message = "Published flag is required")
    private Boolean published;

    /**
     * List of obstacles along the bike path.
     * This field is required but can be an empty list if there are no obstacles.
     * Each obstacle contains an address (to be geocoded), type, and severity.
     * Obstacles will be validated to ensure they are within a reasonable distance
     * from the calculated bike path route.
     */
    @NotNull(message = "Obstacles list is required (use empty list if no obstacles)")
    private List<@Valid ObstacleCreateRequest> obstacles;

}