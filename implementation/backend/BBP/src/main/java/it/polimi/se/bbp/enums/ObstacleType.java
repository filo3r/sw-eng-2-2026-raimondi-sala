package it.polimi.se.bbp.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the different types of obstacles that can be found on bike paths in the BBP system.
 * Obstacles can be detected automatically through accelerometer and gyroscope data during trips,
 * or reported manually by users.
 */
@Getter
@RequiredArgsConstructor
public enum ObstacleType {
    /** A hole or depression in the path surface. */
    POTHOLE("Pothole"),
    /** A visible crack in the path surface. */
    CRACK("Crack"),
    /** General damage to the path surface. */
    DAMAGED_SURFACE("Damaged Surface"),
    /** Irregular or uneven path surface. */
    UNEVEN_SURFACE("Uneven Surface"),
    /** Damage caused by tree roots breaking through the surface. */
    ROOT_DAMAGE("Root Damage"),
    /** Raised bump designed to slow traffic. */
    SPEED_BUMP("Speed Bump"),
    /** Exposed or protruding manhole cover. */
    MANHOLE_COVER("Manhole Cover"),
    /** Missing or damaged drainage grate. */
    MISSING_DRAIN("Missing Drain"),
    /** Loose gravel on the path surface. */
    LOOSE_GRAVEL("Loose Gravel"),
    /** Sand covering the path. */
    SAND("Sand"),
    /** Mud on the path surface. */
    MUD("Mud"),
    /** Broken glass on the path. */
    GLASS("Glass"),
    /** Various debris or litter on the path. */
    DEBRIS("Debris"),
    /** Standing water on the path. */
    PUDDLE("Puddle"),
    /** Significant water accumulation blocking the path. */
    FLOODING("Flooding"),
    /** Ice on the path surface. */
    ICE("Ice"),
    /** Slippery surface due to weather or other conditions. */
    SLIPPERY_SURFACE("Slippery Surface"),
    /** Oil or fuel spill on the path. */
    OIL_SPILL("Oil Spill"),
    /** Fallen tree or large branch blocking the path. */
    FALLEN_TREE("Fallen Tree"),
    /** Physical barrier obstructing the path. */
    BARRIER("Barrier"),
    /** Vehicle parked on or blocking the bike path. */
    PARKED_VEHICLE("Parked Vehicle"),
    /** Vegetation growing into the path area. */
    OVERGROWN_VEGETATION("Overgrown Vegetation"),
    /** Animal on or near the path. */
    ANIMAL("Animal"),
    /** Construction work blocking or affecting the path. */
    CONSTRUCTION("Construction"),
    /** Path width is too narrow for safe cycling. */
    NARROW_PATH("Narrow Path"),
    /** Obstacle type not covered by other categories. */
    OTHER("Other");

    /**
     * The human-readable description of this obstacle type.
     */
    private final String obstacleType;

}