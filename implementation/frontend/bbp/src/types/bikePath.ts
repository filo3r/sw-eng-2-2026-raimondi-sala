/**
 * Maintenance and condition status of a bike path.
 * Each status has an associated score for quality assessment and routing.
 * Can be provided manually or through GPS tracking.
 */
export type BikePathStatus =
    | 'EXCELLENT' // Excellent condition with no issues (score: 10)
    | 'VERY_GOOD' // Very good condition with minimal issues (score: 9)
    | 'GOOD' // Good condition with minor issues (score: 8)
    | 'FAIR' // Fair condition with some noticeable issues (score: 7)
    | 'SUFFICIENT' // Sufficient condition but requires attention (score: 6)
    | 'MEDIOCRE' // Mediocre condition with several issues (score: 5)
    | 'POOR' // Poor condition with significant issues (score: 4)
    | 'VERY_POOR' // Very poor condition with major issues (score: 3)
    | 'CRITICAL' // Critical condition and may be unsafe (score: 2)
    | 'IMPASSABLE' // Impassable and cannot be used (score: 1)
    | 'UNDER_MAINTENANCE' // Currently under maintenance work (no score)
    | 'TEMPORARILY_CLOSED' // Temporarily closed to cyclists (no score)
    | 'PERMANENTLY_CLOSED' // Permanently closed and no longer available (no score)

import type { ObstacleCreateRequest, ObstacleUpdateRequest, ObstacleResponse } from './obstacle'

/**
 * Request for searching bike paths within geographic radius from origin and destination.
 * Finds published bike paths where origin and destination are within specified distances.
 */
export interface BikePathFinderRequest {
    /** Origin address (will be geocoded) */
    originAddress: string
    /** Search radius around origin in kilometers */
    originRadiusKm: number
    /** Destination address (will be geocoded) */
    destinationAddress: string
    /** Search radius around destination in kilometers */
    destinationRadiusKm: number
}

/**
 * Request for manually creating a bike path by providing addresses.
 * Addresses will be geocoded and used to calculate the cycling route.
 * Supports optional obstacles that will be validated against the route.
 */
export interface BikePathManualCreateRequest {
    /** Ordered list of addresses (min 2, max 256 chars each). First is origin, last is destination. */
    addresses: string[]
    /** Optional path description or notes (max 500 characters) */
    description?: string
    /** Current condition status affecting score calculation */
    status: BikePathStatus
    /** Visibility flag (true = public and editable by anyone, false = private to creator only) */
    published: boolean
    /** List of obstacles along the path (can be empty, validated against route distance) */
    obstacles: ObstacleCreateRequest[]
}

/**
 * Request for bike path search with optional combinable filters.
 * All filters are optional and can be used independently or together.
 * Empty request returns all user bike paths.
 */
export interface BikePathSearchRequest {
    /** Text search for origin location (case-insensitive, partial match, max 256 characters) */
    origin?: string
    /** Text search for destination location (case-insensitive, partial match, max 256 characters) */
    destination?: string
    /** Minimum creation time (inclusive, ISO format) */
    createdAtFrom?: string
    /** Maximum creation time (inclusive, ISO format) */
    createdAtTo?: string
}

/**
 * Request for updating an existing bike path with partial updates.
 * Only provided fields will be updated. Creator can always update, others only if published.
 * Note: Route (addresses, waypoints) cannot be updated. To change route, create new bike path.
 */
export interface BikePathUpdateRequest {
    /** Version for optimistic locking */
    version?: number
    /** New condition status (optional, affects score calculation) */
    status?: BikePathStatus
    /** New description (optional, empty string clears it, max 500 characters) */
    description?: string
    /** Visibility flag (optional, true = public and editable by anyone, false = private to creator) */
    published?: boolean
    /** New obstacles to add (optional, will be geocoded and validated against route) */
    obstaclesToAdd?: ObstacleCreateRequest[]
    /** Existing obstacles to update (optional, identified by ID, must belong to this bike path) */
    obstaclesToUpdate?: ObstacleUpdateRequest[]
}

/**
 * GPS coordinate point along a bike path route.
 * Points define the complete geometry of the bike path.
 */
export interface BikePathPointResponse {
    /** Latitude coordinate in decimal degrees (-90.0 to +90.0) */
    latitude: number
    /** Longitude coordinate in decimal degrees (-180.0 to +180.0) */
    longitude: number
    /** Timestamp when recorded (ISO format, null for manually created paths) */
    timestamp: string | null
    /** Sequential position in route (1-indexed, lower values are earlier points) */
    sequentialPosition: number
}

/**
 * Complete bike path with route details, status, score, and obstacles.
 */
export interface BikePathResponse {
    /** Unique bike path identifier */
    id: number
    /** Version for optimistic locking */
    version: number
    /** ID of the user who created this bike path */
    createdById: number
    /** Username of the user who created this bike path */
    createdByUsername: string
    /** Creation timestamp (ISO format) */
    createdAt: string
    /** ID of the user who last updated this bike path (null if never updated) */
    updatedById: number | null
    /** Username of the user who last updated this bike path (null if never updated) */
    updatedByUsername: string | null
    /** Last update timestamp (ISO format, null if never updated) */
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
    /** Optional path description or notes */
    description: string | null
    /** Overall quality score (0.0-5.0, computed from status, obstacles, and other factors) */
    score: number
    /** Current condition status */
    status: BikePathStatus
    /** Human-readable status description (e.g., "Excellent", "Good", "Under Maintenance") */
    statusDescription: string
    /** Total distance in kilometers */
    totalDistance: number
    /** Visibility flag (true = public and editable by anyone, false = private to creator only) */
    published: boolean
    /** GPS coordinates forming the complete route (ordered by sequential position) */
    bikePathPoints: BikePathPointResponse[]
    /** Obstacles reported along this path (includes active and inactive for historical tracking) */
    obstacles: ObstacleResponse[]
}

/**
 * Paginated bike paths response with navigation metadata.
 * Returns a subset of bike paths with pagination information for navigation.
 */
export interface PagedBikePathResponse {
    /** List of bike paths in current page (includes all details: points and obstacles) */
    content: BikePathResponse[]
    /** Current page number (0-indexed) */
    currentPage: number
    /** Number of items per page */
    pageSize: number
    /** Total number of bike paths across all pages */
    totalElements: number
    /** Total number of pages (calculated as ceil(totalElements / pageSize)) */
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