package it.polimi.se.bbp.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maintenance and condition status of a bike path.
 * Each status has an associated score for quality assessment and routing.
 * Can be provided manually or through GPS tracking.
 */
@Getter
@RequiredArgsConstructor
public enum BikePathStatus {
    /** Path is in excellent condition with no issues. */
    EXCELLENT(10, "Excellent"),
    /** Path is in very good condition with minimal issues. */
    VERY_GOOD(9, "Very Good"),
    /** Path is in good condition with minor issues. */
    GOOD(8, "Good"),
    /** Path is in fair condition with some noticeable issues. */
    FAIR(7, "Fair"),
    /** Path is in sufficient condition but requires attention. */
    SUFFICIENT(6, "Sufficient"),
    /** Path is in mediocre condition with several issues. */
    MEDIOCRE(5, "Mediocre"),
    /** Path is in poor condition with significant issues. */
    POOR(4, "Poor"),
    /** Path is in very poor condition with major issues. */
    VERY_POOR(3, "Very Poor"),
    /** Path is in critical condition and may be unsafe. */
    CRITICAL(2, "Critical"),
    /** Path is impassable and cannot be used. */
    IMPASSABLE(1, "Impassable"),
    /** Path is currently under maintenance work. */
    UNDER_MAINTENANCE(null, "Under Maintenance"),
    /** Path is temporarily closed to cyclists. */
    TEMPORARILY_CLOSED(null, "Temporarily Closed"),
    /** Path is permanently closed and no longer available. */
    PERMANENTLY_CLOSED(null, "Permanently Closed");

    /**
     * Numeric score for this status.
     * Higher scores indicate better conditions.
     * Null for non-quality statuses (closed or under maintenance).
     */
    private final Integer statusScore;

    /**
     * Human-readable description of this status.
     */
    private final String statusDescription;

    /**
     * Immutable map for quick lookup by score.
     * Only contains statuses with non-null scores.
     */
    private static final Map<Integer, BikePathStatus> SCORE_MAP =
            Stream.of(values())
                    .filter(status -> status.statusScore != null)
                    .collect(Collectors.toUnmodifiableMap(
                            BikePathStatus::getStatusScore,
                            status -> status
                    ));

    /**
     * Retrieves bike path status by score.
     * @param score numeric status score (1-10)
     * @return BikePathStatus matching the score
     * @throws IllegalArgumentException if no status exists for the given score
     */
    public static BikePathStatus fromStatusScore(int score) {
        BikePathStatus status = SCORE_MAP.get(score);
        if (status == null)
            throw new IllegalArgumentException("Unknown status score: " + score);
        return status;
    }

}