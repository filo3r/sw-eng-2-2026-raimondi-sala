package it.polimi.se.bbp.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Request for bike path search with optional combinable filters.
 * All filters are optional and can be used independently or together.
 * Empty request returns all user bike paths.
 * @param origin text search for origin location (case-insensitive, partial match)
 * @param destination text search for destination location (case-insensitive, partial match)
 * @param createdAtFrom minimum creation time (inclusive)
 * @param createdAtTo maximum creation time (inclusive)
 */
public record BikePathSearchRequest(

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
         * Minimum creation time (inclusive).
         */
        OffsetDateTime createdAtFrom,

        /*
         * Maximum creation time (inclusive).
         */
        OffsetDateTime createdAtTo

) {

    /**
     * Validates that createdAtTo is after or equal to createdAtFrom.
     * Returns true if date range is valid or if either date is null.
     * @return true if date range is valid
     */
    @AssertTrue(message = "End date must be after or equal to start date")
    @JsonIgnore
    public boolean isDateRangeValid() {
        if (createdAtFrom == null || createdAtTo == null)
            return true;
        return !createdAtTo.isBefore(createdAtFrom);
    }

    /**
     * Checks if any search filter is specified.
     * Used to determine if this is a filtered search or simple listing.
     * @return true if at least one filter is present
     */
    @JsonIgnore
    public boolean hasFilters() {
        return origin != null || destination != null || createdAtFrom != null || createdAtTo != null;
    }

}