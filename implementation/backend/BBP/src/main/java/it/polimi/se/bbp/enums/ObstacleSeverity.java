package it.polimi.se.bbp.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Severity level of an obstacle on a bike path.
 * Used to assess impact on cycling safety and route quality.
 * Can be detected automatically or reported manually.
 */
@Getter
@RequiredArgsConstructor
public enum ObstacleSeverity {
    /** Minor obstacle with minimal impact on cycling safety. */
    LOW(1, "Low"),
    /** Moderate obstacle requiring caution. */
    MEDIUM(2, "Medium"),
    /** Serious obstacle that significantly affects cycling safety. */
    HIGH(3, "High"),
    /** Critical obstacle that poses immediate danger or makes the path unusable. */
    CRITICAL(4, "Critical");

    /**
     * Numeric level for this severity.
     * Higher values indicate more severe obstacles.
     */
    private final int severityLevel;

    /**
     * Human-readable description of this severity level.
     */
    private final String severityDescription;

    /**
     * Immutable map for quick lookup by level.
     */
    private static final Map<Integer, ObstacleSeverity> LEVEL_MAP =
            Stream.of(values())
                    .collect(Collectors.toUnmodifiableMap(
                            ObstacleSeverity::getSeverityLevel,
                            severity -> severity
                    ));

    /**
     * Retrieves obstacle severity by level.
     * @param level numeric severity level (1-4)
     * @return ObstacleSeverity matching the level
     * @throws IllegalArgumentException if no severity exists for the given level
     */
    public static ObstacleSeverity fromSeverityLevel(int level) {
        ObstacleSeverity severity = LEVEL_MAP.get(level);
        if (severity == null)
            throw new IllegalArgumentException("Unknown severity level: " + level);
        return severity;
    }

}