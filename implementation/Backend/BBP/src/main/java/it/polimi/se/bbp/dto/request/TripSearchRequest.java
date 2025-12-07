package it.polimi.se.bbp.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Request for trip search with optional combinable filters.
 * All filters are optional and can be used independently or together.
 * Empty request returns all user trips.
 * @param origin text search for origin location (case-insensitive, partial match)
 * @param destination text search for destination location (case-insensitive, partial match)
 * @param startTimeFrom minimum start time (inclusive)
 * @param startTimeTo maximum start time (inclusive)
 */
public record TripSearchRequest(

        /*
         * Text search for origin location (case-insensitive, partial match).
         * Maximum 256 characters.
         */
        @Size(max = 256, message = "Origin must not exceed 256 characters")
        String origin,

        /*
         * Text search for destination location (case-insensitive, partial match).
         * Maximum 256 characters.
         */
        @Size(max = 256, message = "Destination must not exceed 256 characters")
        String destination,

        /*
         * Minimum start time (inclusive).
         */
        OffsetDateTime startTimeFrom,

        /*
         * Maximum start time (inclusive).
         */
        OffsetDateTime startTimeTo

) {

    /**
     * Validates that startTimeTo is after or equal to startTimeFrom.
     * Returns true if date range is valid or if either date is null.
     * @return true if date range is valid
     */
    @AssertTrue(message = "End date must be after or equal to start date")
    @JsonIgnore
    public boolean isDateRangeValid() {
        if (startTimeFrom == null || startTimeTo == null)
            return true;
        return !startTimeTo.isBefore(startTimeFrom);
    }

    /**
     * Checks if any search filter is specified.
     * Used to determine if this is a filtered search or simple listing.
     * @return true if at least one filter is present
     */
    @JsonIgnore
    public boolean hasFilters() {
        return origin != null || destination != null || startTimeFrom != null || startTimeTo != null;
    }

}