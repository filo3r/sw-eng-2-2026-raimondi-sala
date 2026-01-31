/**
 * Types of obstacles found on bike paths.
 * Can be detected automatically through sensors or reported manually.
 */
export type ObstacleType =
    | 'POTHOLE' // Hole or depression in the path surface
    | 'CRACK' // Visible crack in the path surface
    | 'DAMAGED_SURFACE' // General damage to the path surface
    | 'UNEVEN_SURFACE' // Irregular or uneven path surface
    | 'ROOT_DAMAGE' // Damage caused by tree roots breaking through
    | 'SPEED_BUMP' // Raised bump designed to slow traffic
    | 'MANHOLE_COVER' // Exposed or protruding manhole cover
    | 'MISSING_DRAIN' // Missing or damaged drainage grate
    | 'LOOSE_GRAVEL' // Loose gravel on the path surface
    | 'SAND' // Sand covering the path
    | 'MUD' // Mud on the path surface
    | 'GLASS' // Broken glass on the path
    | 'DEBRIS' // Various debris or litter on the path
    | 'PUDDLE' // Standing water on the path
    | 'FLOODING' // Significant water accumulation blocking the path
    | 'ICE' // Ice on the path surface
    | 'SLIPPERY_SURFACE' // Slippery surface due to weather or conditions
    | 'OIL_SPILL' // Oil or fuel spill on the path
    | 'FALLEN_TREE' // Fallen tree or large branch blocking the path
    | 'BARRIER' // Physical barrier obstructing the path
    | 'PARKED_VEHICLE' // Vehicle parked on or blocking the bike path
    | 'OVERGROWN_VEGETATION' // Vegetation growing into the path area
    | 'ANIMAL' // Animal on or near the path
    | 'CONSTRUCTION' // Construction work blocking or affecting the path
    | 'NARROW_PATH' // Path width is too narrow for safe cycling
    | 'OTHER' // Obstacle type not covered by other categories

/**
 * Severity level of an obstacle on a bike path.
 * Used to assess impact on cycling safety and route quality.
 */
export type ObstacleSeverity =
    | 'LOW' // Minor obstacle with minimal impact (level: 1)
    | 'MEDIUM' // Moderate obstacle requiring caution (level: 2)
    | 'HIGH' // Serious obstacle affecting safety significantly (level: 3)
    | 'CRITICAL' // Critical obstacle posing immediate danger or making path unusable (level: 4)

/**
 * Request for creating a new obstacle on a bike path.
 * Address will be geocoded to obtain coordinates.
 * All newly created obstacles are automatically set to active=true.
 */
export interface ObstacleCreateRequest {
    /** Address or location description (max 256 characters, will be geocoded to obtain coordinates) */
    address: string
    /** Type or category of the obstacle (e.g., POTHOLE, DEBRIS, CONSTRUCTION) */
    type: ObstacleType
    /** Severity level (LOW, MEDIUM, HIGH, or CRITICAL) */
    severity: ObstacleSeverity
}

/**
 * Request for updating an existing obstacle with partial updates.
 * Only provided fields will be updated. Obstacle must belong to the bike path being updated.
 * Note: Location/address cannot be updated. To change location, mark inactive and create new one.
 */
export interface ObstacleUpdateRequest {
    /** Unique identifier of the obstacle to update (must belong to the bike path) */
    id: number
    /** New type or category (optional, e.g., POTHOLE, DEBRIS, CONSTRUCTION) */
    type?: ObstacleType
    /** New severity level (optional, LOW, MEDIUM, HIGH, or CRITICAL) */
    severity?: ObstacleSeverity
    /** Active status (optional, false = resolved or no longer present, kept for historical tracking) */
    active?: boolean
}

/**
 * Obstacle on a bike path with location, type, severity, and status.
 * Includes both active and resolved obstacles for historical tracking.
 */
export interface ObstacleResponse {
    /** Unique obstacle identifier */
    id: number
    /** ID of the user who reported this obstacle (null if user deleted) */
    createdById: number | null
    /** Username of the user who reported this obstacle (null if user deleted) */
    createdByUsername: string | null
    /** Creation timestamp (ISO format) */
    createdAt: string
    /** ID of the user who last updated this obstacle (null if never updated or user deleted) */
    updatedById: number | null
    /** Username of the user who last updated this obstacle (null if never updated or user deleted) */
    updatedByUsername: string | null
    /** Last update timestamp (ISO format, null if never updated) */
    updatedAt: string | null
    /** Formatted address from geocoding service */
    address: string
    /** Latitude coordinate in decimal degrees (-90.0 to +90.0) */
    latitude: number
    /** Longitude coordinate in decimal degrees (-180.0 to +180.0) */
    longitude: number
    /** Type or category of the obstacle */
    type: ObstacleType
    /** Human-readable obstacle type description (e.g., "Pothole", "Debris", "Construction") */
    typeDescription: string
    /** Severity level of the obstacle */
    severity: ObstacleSeverity
    /** Human-readable severity description (e.g., "Low", "Medium", "High", "Critical") */
    severityDescription: string
    /** Active status flag (false = resolved or no longer present, kept for historical tracking) */
    active: boolean
    /** Position of obstacle along the bike path route */
    positionOnPath: number
}