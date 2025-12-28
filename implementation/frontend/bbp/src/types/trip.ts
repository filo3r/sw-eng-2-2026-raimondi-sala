import type { MeteorologicalDataResponse } from './meteorologicalData.ts'

// ==================== REQUEST DTOs ====================

/**
 * Request for manually recording a trip by providing addresses and trip details.
 * Addresses will be geocoded and used to calculate the cycling route.
 * Includes temporal validation for start/end times and minimum duration.
 */
export interface TripManualRecordRequest {
    /**
     * Ordered list of addresses defining the trip route.
     * First is origin, last is destination, intermediate are waypoints.
     * Minimum 2 addresses required (max 256 chars each).
     */
    addresses: string[]

    /**
     * Optional description or notes about the trip.
     * Maximum 500 characters.
     */
    description?: string

    /**
     * Exact date and time when the trip started (ISO format).
     */
    startTime: string

    /**
     * Exact date and time when the trip ended (ISO format).
     * Must be after start time with minimum 1 minute duration.
     */
    endTime: string

    /**
     * Maximum speed reached during the trip in km/h (optional).
     * Must be greater than 0 if provided.
     * Precision: max 3 integer digits, 2 decimal digits (e.g., 123.45).
     */
    maxSpeed?: number
}

/**
 * Request for trip search with optional combinable filters.
 * All filters are optional and can be used independently or together.
 * Empty request returns all user trips.
 */
export interface TripSearchRequest {
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

    /** Minimum start time (inclusive, ISO format) */
    startTimeFrom?: string

    /** Maximum start time (inclusive, ISO format) */
    startTimeTo?: string
}

// ==================== RESPONSE DTOs ====================

/**
 * GPS coordinate point along a trip route.
 * Points define the complete geometry of the trip.
 */
export interface TripPointResponse {
    /** Latitude coordinate in decimal degrees */
    latitude: number

    /** Longitude coordinate in decimal degrees */
    longitude: number

    /**
     * Timestamp when this point was recorded (ISO format).
     * Null for manually recorded trips.
     */
    timestamp: string | null

    /**
     * Sequential position of this point in the route (1-indexed).
     * Lower values indicate earlier points in the path.
     */
    sequentialPosition: number
}

/**
 * Complete trip with route details, timing, performance metrics, and weather data.
 */
export interface TripResponse {
    /** Unique identifier */
    id: number

    /** ID of the user who recorded this trip */
    recordedById: number

    /** Username of the user who recorded this trip */
    recordedByUsername: string

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

    /** Optional description or notes about the trip */
    description: string | null

    /** Start timestamp (ISO format) */
    startTime: string

    /** End timestamp (ISO format) */
    endTime: string

    /** Total duration in minutes */
    totalDuration: number

    /** Total distance in kilometers */
    totalDistance: number

    /** Average speed in km/h */
    averageSpeed: number

    /**
     * Maximum speed in km/h.
     * Null if not provided.
     */
    maxSpeed: number | null

    /**
     * GPS coordinates forming the complete route.
     * Ordered by sequential position.
     */
    tripPoints: TripPointResponse[]

    /**
     * Meteorological data for this trip.
     * Null if unavailable (e.g., trip older than 90 days, API error).
     */
    meteorologicalData: MeteorologicalDataResponse | null
}

/**
 * Paginated trips with navigation metadata.
 * Returns a subset of trips with pagination information.
 */
export interface PagedTripResponse {
    /**
     * List of trips in current page.
     * Includes all details (points and meteorological data).
     */
    content: TripResponse[]

    /** Current page number (0-indexed) */
    currentPage: number

    /** Number of items per page */
    pageSize: number

    /** Total number of trips across all pages */
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