/**
 * Maintenance and condition status of a bike path.
 * Each status has an associated score for quality assessment and routing.
 * Can be provided manually or through GPS tracking.
 */
export type BikePathStatus =
/** Path is in excellent condition with no issues (score: 10) */
    | 'EXCELLENT'
    /** Path is in very good condition with minimal issues (score: 9) */
    | 'VERY_GOOD'
    /** Path is in good condition with minor issues (score: 8) */
    | 'GOOD'
    /** Path is in fair condition with some noticeable issues (score: 7) */
    | 'FAIR'
    /** Path is in sufficient condition but requires attention (score: 6) */
    | 'SUFFICIENT'
    /** Path is in mediocre condition with several issues (score: 5) */
    | 'MEDIOCRE'
    /** Path is in poor condition with significant issues (score: 4) */
    | 'POOR'
    /** Path is in very poor condition with major issues (score: 3) */
    | 'VERY_POOR'
    /** Path is in critical condition and may be unsafe (score: 2) */
    | 'CRITICAL'
    /** Path is impassable and cannot be used (score: 1) */
    | 'IMPASSABLE'
    /** Path is currently under maintenance work (no score) */
    | 'UNDER_MAINTENANCE'
    /** Path is temporarily closed to cyclists (no score) */
    | 'TEMPORARILY_CLOSED'
    /** Path is permanently closed and no longer available (no score) */
    | 'PERMANENTLY_CLOSED'

import type { ObstacleCreateRequest, ObstacleUpdateRequest, ObstacleResponse } from './obstacle'

/**
 * Request for searching bike paths within geographic radius.
 * Finds published bike paths where origin and destination are within
 * specified distances from search addresses.
 */
export interface BikePathFinderRequest {
    /** Origin address (will be geocoded) */
    originAddress: string

    /** Radius around origin in kilometers */
    originRadiusKm: number

    /** Destination address (will be geocoded) */
    destinationAddress: string

    /** Radius around destination in kilometers */
    destinationRadiusKm: number
}

/**
 * Request for manually creating a bike path by providing addresses.
 * Addresses will be geocoded and used to calculate the cycling route.
 * Supports optional obstacles that will be validated against the route.
 */
export interface BikePathManualCreateRequest {
    /**
     * Ordered list of addresses defining the bike path route.
     * First is origin, last is destination, intermediate are waypoints.
     * Minimum 2 addresses required (max 256 chars each).
     */
    addresses: string[]

    /**
     * Optional description or notes about the bike path.
     * Maximum 500 characters.
     */
    description?: string

    /**
     * Current condition status of the bike path.
     * Affects score calculation.
     */
    status: BikePathStatus

    /**
     * Visibility flag.
     * True = visible to all and editable by anyone.
     * False = visible and editable only by creator.
     */
    published: boolean

    /**
     * List of obstacles along the bike path.
     * Can be empty if no obstacles.
     * Validated to be within reasonable distance from route.
     */
    obstacles: ObstacleCreateRequest[]
}

/**
 * Request for bike path search with optional combinable filters.
 * All filters are optional and can be used independently or together.
 * Empty request returns all user bike paths.
 */
export interface BikePathSearchRequest {
    /**
     * Text search for origin location (case-insensitive, partial match).
     * Maximum 256 characters.
     */
    origin?: string

    /**
     * Text search for destination location (case-insensitive, partial match).
     * Maximum 256 characters.
     */
    destination?: string

    /** Minimum creation time (inclusive, ISO format) */
    createdAtFrom?: string

    /** Maximum creation time (inclusive, ISO format) */
    createdAtTo?: string
}

/**
 * Request for updating an existing bike path with partial updates.
 * Only non-null fields will be updated, supporting flexible modifications.
 * Permission rules: creator can always update, other users can only update published paths.
 * Note: The bike path route (addresses, waypoints) cannot be updated.
 * To change the route, create a new bike path.
 */
export interface BikePathUpdateRequest {
    /** Version for optimistic locking */
    version?: number

    /**
     * New condition status (optional).
     * Affects score calculation.
     */
    status?: BikePathStatus

    /**
     * New description (optional).
     * Empty string clears the description.
     * Maximum 500 characters.
     */
    description?: string

    /**
     * Visibility flag (optional).
     * True = visible to all and editable by anyone.
     * False = visible and editable only by creator.
     */
    published?: boolean

    /**
     * New obstacles to add to the bike path (optional).
     * Will be geocoded and validated against route distance.
     */
    obstaclesToAdd?: ObstacleCreateRequest[]

    /**
     * Existing obstacles to update (optional).
     * Identified by ID, supports partial updates.
     * Must belong to this bike path.
     */
    obstaclesToUpdate?: ObstacleUpdateRequest[]
}

/**
 * GPS coordinate point along a bike path route.
 * Points define the complete geometry of the bike path.
 */
export interface BikePathPointResponse {
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

    /**
     * Timestamp when this point was recorded (ISO format).
     * Null for manually created bike paths.
     * For GPS-tracked paths, exact time when cyclist passed through this coordinate.
     */
    timestamp: string | null

    /**
     * Sequential position of this point in the route (1-indexed).
     * Lower values indicate earlier points in the path.
     */
    sequentialPosition: number
}

/**
 * Complete bike path with route details, status, score, and obstacles.
 */
export interface BikePathResponse {
    /** Unique identifier */
    id: number

    /** Version for optimistic locking */
    version: number

    /** ID of the user who created this bike path */
    createdById: number

    /** Username of the user who created this bike path */
    createdByUsername: string

    /** Creation timestamp (ISO format) */
    createdAt: string

    /**
     * ID of the user who last updated this bike path.
     * Null if never updated.
     */
    updatedById: number | null

    /**
     * Username of the user who last updated this bike path.
     * Null if never updated.
     */
    updatedByUsername: string | null

    /**
     * Last update timestamp (ISO format).
     * Null if never updated.
     */
    updatedAt: string | null

    /** Formatted address of the starting point */
    origin: string

    /** Latitude of the origin point */
    originLatitude: number

    /** Longitude of the origin point */
    originLongitude: number

    /** Formatted address of the destination point */
    destination: string

    /** Latitude of the destination point */
    destinationLatitude: number

    /** Longitude of the destination point */
    destinationLongitude: number

    /** Optional description or notes */
    description: string | null

    /**
     * Overall quality score.
     * Range: 0.0 to 5.0
     * Computed from status, obstacles, and other factors.
     */
    score: number

    /** Current condition status */
    status: BikePathStatus

    /**
     * Human-readable status description.
     * Example: "Excellent", "Good", "Under Maintenance"
     */
    statusDescription: string

    /** Total distance in kilometers */
    totalDistance: number

    /**
     * Visibility flag.
     * True = public and editable by anyone.
     * False = private and only visible/editable by creator.
     */
    published: boolean

    /**
     * GPS coordinates forming the complete route.
     * Ordered by sequential position.
     */
    bikePathPoints: BikePathPointResponse[]

    /**
     * Obstacles reported along this bike path.
     * Includes active and inactive for historical tracking.
     */
    obstacles: ObstacleResponse[]
}

/**
 * Paginated bike paths with navigation metadata.
 * Returns a subset of bike paths with pagination information.
 */
export interface PagedBikePathResponse {
    /**
     * List of bike paths in current page.
     * Includes all details (points and obstacles).
     */
    content: BikePathResponse[]

    /** Current page number (0-indexed) */
    currentPage: number

    /** Number of items per page */
    pageSize: number

    /** Total number of bike paths across all pages */
    totalElements: number

    /**
     * Total number of pages.
     * Calculated as: ceil(totalElements / pageSize)
     */
    totalPages: number

    /** Flag indicating if next page is available */
    hasNext: boolean

    /** Flag indicating if previous page is available */
    hasPrevious: boolean

    /** Flag indicating if this is the first page */
    firstPage: boolean

    /** Flag indicating if this is the last page */
    lastPage: boolean
}