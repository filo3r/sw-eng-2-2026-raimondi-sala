/**
 * Types for Mapbox API endpoints.
 * Includes geocoding and routing request/response structures.
 */

/**
 * Geographic coordinate with latitude and longitude in decimal degrees.
 */
export interface Coordinate {
    /** Latitude in decimal degrees (-90.0 to +90.0) */
    latitude: number
    /** Longitude in decimal degrees (-180.0 to +180.0) */
    longitude: number
}

/**
 * Request for forward geocoding to convert address into coordinates.
 */
export interface ForwardGeocodeRequest {
    /** Address string to geocode (e.g., "Piazza del Duomo, Milano") */
    address: string
}

/**
 * Request for reverse geocoding to convert coordinates into address.
 */
export interface ReverseGeocodeRequest {
    /** Geographic coordinate to reverse geocode */
    coordinate: Coordinate
}

/**
 * Response for geocoding operations containing address and coordinates.
 * Used for both forward and reverse geocoding results.
 */
export interface GeocodeResponse {
    /** Formatted address string from geocoding service */
    address: string
    /** Latitude coordinate in decimal degrees */
    latitude: number
    /** Longitude coordinate in decimal degrees */
    longitude: number
}

/**
 * Request for calculating a cycling route between multiple points.
 */
export interface CyclingRouteRequest {
    /** Ordered list of waypoints (min 2). First is origin, last is destination. */
    waypoints: Coordinate[]
}

/**
 * Single point in a calculated cycling route with position information.
 */
export interface CyclingRoutePointResponse {
    /** Latitude coordinate in decimal degrees */
    latitude: number
    /** Longitude coordinate in decimal degrees */
    longitude: number
    /** Sequential position in route (1-indexed, lower values are earlier points) */
    sequentialPosition: number
}

/**
 * Response containing calculated cycling route as ordered list of points.
 */
export interface CyclingRouteResponse {
    /** Ordered list of route points from origin to destination */
    points: CyclingRoutePointResponse[]
}