import type { MeteorologicalDataResponse } from './meteorologicalData.ts'

/**
 * Request for manually recording a trip by providing addresses and trip details.
 * Addresses will be geocoded and used to calculate the cycling route.
 * Includes temporal validation for start/end times and minimum duration.
 */
export interface TripManualRecordRequest {
    /** Ordered list of addresses (min 2, max 256 chars each). First is origin, last is destination. */
    addresses: string[]
    /** Optional trip description or notes (max 500 characters) */
    description?: string
    /** Trip start date and time (ISO format) */
    startTime: string
    /** Trip end date and time (ISO format, must be after start with min 1 minute duration) */
    endTime: string
    /** Maximum speed reached in km/h (optional, must be > 0, precision: 3.2 decimal digits) */
    maxSpeed?: number
}

/**
 * Request for trip search with optional combinable filters.
 * All filters are optional and can be used independently or together.
 * Empty request returns all user trips.
 */
export interface TripSearchRequest {
    /** Text search for origin location (case-insensitive, partial match, max 256 characters) */
    origin?: string
    /** Text search for destination location (case-insensitive, partial match, max 256 characters) */
    destination?: string
    /** Minimum start time (inclusive, ISO format) */
    startTimeFrom?: string
    /** Maximum start time (inclusive, ISO format) */
    startTimeTo?: string
}

/**
 * GPS coordinate point along a trip route.
 * Points define the complete geometry of the trip.
 */
export interface TripPointResponse {
    /** Latitude coordinate in decimal degrees */
    latitude: number
    /** Longitude coordinate in decimal degrees */
    longitude: number
    /** Timestamp when this point was recorded (ISO format, null for manually recorded trips) */
    timestamp: string | null
    /** Sequential position in route (1-indexed, lower values are earlier points) */
    sequentialPosition: number
}

/**
 * Complete trip with route details, timing, performance metrics, and weather data.
 */
export interface TripResponse {
    /** Unique trip identifier */
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
    /** Optional trip description or notes */
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
    /** Maximum speed in km/h (null if not provided) */
    maxSpeed: number | null
    /** GPS coordinates forming the complete route (ordered by sequential position) */
    tripPoints: TripPointResponse[]
    /** Meteorological data (null if unavailable, e.g., trip older than 90 days or API error) */
    meteorologicalData: MeteorologicalDataResponse | null
}

/**
 * Paginated trips response with navigation metadata.
 * Returns a subset of trips with pagination information for navigation.
 */
export interface PagedTripResponse {
    /** List of trips in current page (includes all details: points and meteorological data) */
    content: TripResponse[]
    /** Current page number (0-indexed) */
    currentPage: number
    /** Number of items per page */
    pageSize: number
    /** Total number of trips across all pages */
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