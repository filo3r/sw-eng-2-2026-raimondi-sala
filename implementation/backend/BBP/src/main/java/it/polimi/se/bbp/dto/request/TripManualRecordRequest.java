package it.polimi.se.bbp.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for manual trip recording request.
 * Contains the user-provided data needed to create a new trip,
 * including addresses that will be geocoded and used to calculate the cycling route.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripManualRecordRequest {

    /**
     * Ordered list of addresses that define the trip route.
     * The first address is the origin, the last is the destination.
     * Intermediate addresses are waypoints.
     * This field is required and must contain at least 2 addresses.
     */
    @NotNull(message = "Addresses list is required")
    @NotEmpty(message = "At least 2 addresses are required (origin and destination)")
    @Size(min = 2, message = "At least 2 addresses are required (origin and destination)")
    private List<@NotBlank(message = "Address cannot be blank") @Size(max = 256, message = "Address must not exceed 256 characters") String> addresses;

    /**
     * Optional description or notes about the trip.
     * Maximum length is 500 characters.
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * The exact date and time when the trip started.
     * This field is required.
     * The trip date will be calculated from this field.
     */
    @NotNull(message = "Start time is required")
    private OffsetDateTime startTime;

    /**
     * The exact date and time when the trip ended.
     * This field is required and must be after the start time.
     */
    @NotNull(message = "End time is required")
    private OffsetDateTime endTime;

    /**
     * The maximum speed reached during the trip in km/h.
     * This field is optional but if provided must be greater than 0.
     * Precision: 5 digits total, 2 after the decimal point (e.g., 123.45).
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Max speed must be greater than 0")
    @Digits(integer = 3, fraction = 2, message = "Max speed must have at most 3 integer digits and 2 decimal digits")
    private BigDecimal maxSpeed;

    /**
     * Validates that the end time is after the start time.
     * This validation ensures the temporal consistency of the trip.
     * @return true if end time is after start time, false otherwise
     */
    @AssertTrue(message = "End time must be after start time")
    private boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null)
            return true;
        return endTime.isAfter(startTime);
    }

    /**
     * Validates that the trip duration is at least 1 minute.
     * This validation prevents the recording of unrealistically short trips.
     * @return true if the trip duration is at least 1 minute, false otherwise
     */
    @AssertTrue(message = "Trip must be at least 1 minute long")
    private boolean isTripDurationValid() {
        if (startTime == null || endTime == null)
            return true;
        return Duration.between(startTime, endTime).toMinutes() >= 1;
    }

}