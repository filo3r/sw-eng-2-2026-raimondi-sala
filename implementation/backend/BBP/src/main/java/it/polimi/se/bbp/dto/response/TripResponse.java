package it.polimi.se.bbp.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response for Trip containing route details, timing, performance metrics, and weather data.
 * @param id unique identifier
 * @param recordedById ID of user who recorded this trip
 * @param recordedByUsername username of user who recorded this trip
 * @param origin formatted address of starting point
 * @param originLatitude latitude of origin point
 * @param originLongitude longitude of origin point
 * @param destination formatted address of destination point
 * @param destinationLatitude latitude of destination point
 * @param destinationLongitude longitude of destination point
 * @param description optional description or notes
 * @param startTime start timestamp
 * @param endTime end timestamp
 * @param totalDuration total duration in minutes
 * @param totalDistance total distance in kilometers
 * @param averageSpeed average speed in km/h
 * @param maxSpeed maximum speed in km/h (null if not provided)
 * @param tripPoints GPS coordinates forming the complete route
 * @param meteorologicalData weather conditions during trip (null if unavailable or trip older than 90 days)
 */
public record TripResponse(

        /*
         * Unique identifier of the trip.
         */
        Long id,

        /*
         * ID of the user who recorded this trip.
         */
        Long recordedById,

        /*
         * Username of the user who recorded this trip.
         */
        String recordedByUsername,

        /*
         * Formatted address of the starting point.
         */
        String origin,

        /*
         * Latitude of the origin point.
         */
        Double originLatitude,

        /*
         * Longitude of the origin point.
         */
        Double originLongitude,

        /*
         * Formatted address of the destination point.
         */
        String destination,

        /*
         * Latitude of the destination point.
         */
        Double destinationLatitude,

        /*
         * Longitude of the destination point.
         */
        Double destinationLongitude,

        /*
         * Optional description or notes about the trip.
         */
        String description,

        /*
         * Start timestamp.
         */
        OffsetDateTime startTime,

        /*
         * End timestamp.
         */
        OffsetDateTime endTime,

        /*
         * Total duration in minutes.
         */
        Integer totalDuration,

        /*
         * Total distance in kilometers.
         */
        BigDecimal totalDistance,

        /*
         * Average speed in km/h.
         */
        BigDecimal averageSpeed,

        /*
         * Maximum speed in km/h.
         * Null if not provided.
         */
        BigDecimal maxSpeed,

        /*
         * GPS coordinates forming the complete route.
         */
        List<TripPointResponse> tripPoints,

        /*
         * Meteorological data for this trip.
         * Null if unavailable (e.g., trip older than 90 days, API error).
         */
        MeteorologicalDataResponse meteorologicalData

) {}