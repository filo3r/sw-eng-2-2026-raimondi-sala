package it.polimi.se.bbp.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request for manually recording a trip by providing addresses and trip details.
 * Addresses will be geocoded and used to calculate the cycling route.
 * Includes temporal validation for start/end times and minimum duration.
 * @param addresses ordered list of addresses defining the trip route (min 2: origin and destination)
 * @param description optional description or notes about the trip (max 500 chars)
 * @param startTime exact date and time when the trip started
 * @param endTime exact date and time when the trip ended (must be after start time, min 1 minute duration)
 * @param maxSpeed maximum speed reached during the trip in km/h (optional, must be positive if provided)
 */
public record TripManualRecordRequest(

        /*
         * Ordered list of addresses defining the trip route.
         * First is origin, last is destination, intermediate are waypoints.
         * Minimum 2 addresses required.
         */
        @NotNull(message = "Addresses list is required")
        @NotEmpty(message = "At least 2 addresses are required (origin and destination)")
        @Size(min = 2, message = "At least 2 addresses are required (origin and destination)")
        List<@NotBlank(message = "Address cannot be blank") @Size(max = 256, message = "Address must not exceed 256 characters") String> addresses,

        /*
         * Optional description or notes about the trip.
         * Maximum 500 characters.
         */
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        /*
         * Exact date and time when the trip started.
         */
        @NotNull(message = "Start time is required")
        OffsetDateTime startTime,

        /*
         * Exact date and time when the trip ended.
         * Must be after start time with minimum 1 minute duration.
         */
        @NotNull(message = "End time is required")
        OffsetDateTime endTime,

        /*
         * Maximum speed reached during the trip in km/h (optional).
         * Must be greater than 0 if provided.
         * Precision: 3 integer digits, 2 decimal digits (e.g., 123.45).
         */
        @DecimalMin(value = "0.0", inclusive = false, message = "Max speed must be greater than 0")
        @Digits(integer = 3, fraction = 2, message = "Max speed must have at most 3 integer digits and 2 decimal digits")
        BigDecimal maxSpeed

) {

    /**
     * Validates that end time is after start time.
     * @return true if temporal order is valid or if either time is null
     */
    @AssertTrue(message = "End time must be after start time")
    @JsonIgnore
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null)
            return true;
        return endTime.isAfter(startTime);
    }

    /**
     * Validates that trip duration is at least 1 minute.
     * Prevents recording of unrealistically short trips.
     * @return true if duration is valid or if either time is null
     */
    @AssertTrue(message = "Trip must be at least 1 minute long")
    @JsonIgnore
    public boolean isTripDurationValid() {
        if (startTime == null || endTime == null)
            return true;
        return Duration.between(startTime, endTime).toMinutes() >= 1;
    }

}