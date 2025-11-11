package it.polimi.se.bbp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Trip response.
 * Contains all trip information including route details, timing, and performance metrics.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripResponse {

    /**
     * Unique identifier of the trip.
     */
    private Long id;

    /**
     * ID of the user who recorded this trip.
     */
    private Long recordedBy;

    /**
     * Formatted address of the trip's starting point.
     */
    private String origin;

    /**
     * Latitude of the origin point.
     */
    private Double originLatitude;

    /**
     * Longitude of the origin point.
     */
    private Double originLongitude;

    /**
     * Formatted address of the trip's destination point.
     */
    private String destination;

    /**
     * Latitude of the destination point.
     */
    private Double destinationLatitude;

    /**
     * Longitude of the destination point.
     */
    private Double destinationLongitude;

    /**
     * Optional description or notes about the trip.
     */
    private String description;

    /**
     * Date when the trip took place.
     */
    private LocalDate tripDate;

    /**
     * Date and time when the trip started.
     */
    private LocalDateTime startTime;

    /**
     * Date and time when the trip ended.
     */
    private LocalDateTime endTime;

    /**
     * Total duration of the trip in minutes.
     */
    private Integer totalDuration;

    /**
     * Total distance covered in kilometers.
     */
    private BigDecimal totalDistance;

    /**
     * Average speed maintained during the trip in km/h.
     */
    private BigDecimal averageSpeed;

    /**
     * Maximum speed reached during the trip in km/h.
     * May be null if not provided.
     */
    private BigDecimal maxSpeed;

    /**
     * List of GPS coordinates that form the complete route.
     */
    private List<TripPointResponse> tripPoints;

}