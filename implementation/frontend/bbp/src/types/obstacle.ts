/**
 * Types of obstacles found on bike paths.
 * Can be detected automatically through sensors or reported manually.
 */
export type ObstacleType =
    /** A hole or depression in the path surface */
    | 'POTHOLE'
    /** A visible crack in the path surface */
    | 'CRACK'
    /** General damage to the path surface */
    | 'DAMAGED_SURFACE'
    /** Irregular or uneven path surface */
    | 'UNEVEN_SURFACE'
    /** Damage caused by tree roots breaking through the surface */
    | 'ROOT_DAMAGE'
    /** Raised bump designed to slow traffic */
    | 'SPEED_BUMP'
    /** Exposed or protruding manhole cover */
    | 'MANHOLE_COVER'
    /** Missing or damaged drainage grate */
    | 'MISSING_DRAIN'
    /** Loose gravel on the path surface */
    | 'LOOSE_GRAVEL'
    /** Sand covering the path */
    | 'SAND'
    /** Mud on the path surface */
    | 'MUD'
    /** Broken glass on the path */
    | 'GLASS'
    /** Various debris or litter on the path */
    | 'DEBRIS'
    /** Standing water on the path */
    | 'PUDDLE'
    /** Significant water accumulation blocking the path */
    | 'FLOODING'
    /** Ice on the path surface */
    | 'ICE'
    /** Slippery surface due to weather or other conditions */
    | 'SLIPPERY_SURFACE'
    /** Oil or fuel spill on the path */
    | 'OIL_SPILL'
    /** Fallen tree or large branch blocking the path */
    | 'FALLEN_TREE'
    /** Physical barrier obstructing the path */
    | 'BARRIER'
    /** Vehicle parked on or blocking the bike path */
    | 'PARKED_VEHICLE'
    /** Vegetation growing into the path area */
    | 'OVERGROWN_VEGETATION'
    /** Animal on or near the path */
    | 'ANIMAL'
    /** Construction work blocking or affecting the path */
    | 'CONSTRUCTION'
    /** Path width is too narrow for safe cycling */
    | 'NARROW_PATH'
    /** Obstacle type not covered by other categories */
    | 'OTHER'

/**
 * Severity level of an obstacle on a bike path.
 * Used to assess impact on cycling safety and route quality.
 */
export type ObstacleSeverity =
/** Minor obstacle with minimal impact on cycling safety (level: 1) */
    | 'LOW'
    /** Moderate obstacle requiring caution (level: 2) */
    | 'MEDIUM'
    /** Serious obstacle that significantly affects cycling safety (level: 3) */
    | 'HIGH'
    /** Critical obstacle that poses immediate danger or makes the path unusable (level: 4) */
    | 'CRITICAL'

// ==================== REQUEST DTOs ====================

/**
 * Request for creating a new obstacle on a bike path.
 * Address will be geocoded to obtain coordinates.
 * All newly created obstacles are automatically set to active=true.
 */
export interface ObstacleCreateRequest {
    /**
     * Address or location description of the obstacle.
     * Will be geocoded to obtain coordinates.
     * Maximum 256 characters.
     */
    address: string

    /**
     * Type or category of the obstacle.
     * Examples: POTHOLE, DEBRIS, CONSTRUCTION.
     */
    type: ObstacleType

    /**
     * Severity level of the obstacle.
     * Possible values: LOW, MEDIUM, HIGH, CRITICAL.
     */
    severity: ObstacleSeverity
}

/**
 * Request for updating an existing obstacle with partial updates.
 * Only non-null fields will be updated.
 * Obstacle must belong to the bike path being updated.
 * Note: The location/address of an obstacle cannot be updated.
 * To change an obstacle's location, mark it inactive and create a new one.
 */
export interface ObstacleUpdateRequest {
    /**
     * Unique identifier of the obstacle to update.
     * Must belong to the bike path being updated.
     */
    id: number

    /**
     * New type or category of the obstacle (optional).
     * Examples: POTHOLE, DEBRIS, CONSTRUCTION.
     */
    type?: ObstacleType

    /**
     * New severity level of the obstacle (optional).
     * Possible values: LOW, MEDIUM, HIGH, CRITICAL.
     */
    severity?: ObstacleSeverity

    /**
     * Active status (optional).
     * False = resolved or no longer present, kept for historical tracking.
     */
    active?: boolean
}

// ==================== RESPONSE DTOs ====================

/**
 * Obstacle on a bike path with location, type, severity, and status.
 * Includes both active and resolved obstacles for historical tracking.
 */
export interface ObstacleResponse {
    /** Unique identifier */
    id: number

    /**
     * ID of the user who reported this obstacle.
     * Null if user deleted.
     */
    createdById: number | null

    /**
     * Username of the user who reported this obstacle.
     * Null if user deleted.
     */
    createdByUsername: string | null

    /** Creation timestamp (ISO format) */
    createdAt: string

    /**
     * ID of the user who last updated this obstacle.
     * Null if never updated or user deleted.
     */
    updatedById: number | null

    /**
     * Username of the user who last updated this obstacle.
     * Null if never updated or user deleted.
     */
    updatedByUsername: string | null

    /**
     * Last update timestamp (ISO format).
     * Null if never updated.
     */
    updatedAt: string | null

    /**
     * Formatted address of obstacle location.
     * Geocoded address from mapping service.
     */
    address: string

    /**
     * Latitude coordinate in decimal degrees.
     * Valid range: -90.0 to +90.0
     */
    latitude: number

    /**
     * Longitude coordinate in decimal degrees.
     * Valid range: -180.0 to +180.0
     */
    longitude: number

    /** Type or category of the obstacle */
    type: ObstacleType

    /**
     * Human-readable obstacle type description.
     * Example: "Pothole", "Debris", "Construction"
     */
    typeDescription: string

    /** Severity level of the obstacle */
    severity: ObstacleSeverity

    /**
     * Human-readable severity level description.
     * Example: "Low", "Medium", "High", "Critical"
     */
    severityDescription: string

    /**
     * Active status flag.
     * False = resolved or no longer present, kept for historical tracking.
     */
    active: boolean

    /** Position of obstacle along the bike path route */
    positionOnPath: number
}