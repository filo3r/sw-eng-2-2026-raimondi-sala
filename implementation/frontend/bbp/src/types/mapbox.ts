/**
 * Types for Mapbox API endpoints.
 */

/**
 * Coordinate with latitude and longitude.
 */
export interface Coordinate {
    latitude: number
    longitude: number
}

/**
 * Request for forward geocoding (address to coordinates).
 */
export interface ForwardGeocodeRequest {
    address: string
}

/**
 * Request for reverse geocoding (coordinates to address).
 */
export interface ReverseGeocodeRequest {
    coordinate: Coordinate
}

/**
 * Response for geocoding operations (forward and reverse).
 */
export interface GeocodeResponse {
    address: string
    latitude: number
    longitude: number
}

/**
 * Request for calculating a cycling route.
 */
export interface CyclingRouteRequest {
    waypoints: Coordinate[]
}

/**
 * Single point in a calculated route.
 */
export interface CyclingRoutePointResponse {
    latitude: number
    longitude: number
    sequentialPosition: number
}

/**
 * Response for calculated cycling route.
 */
export interface CyclingRouteResponse {
    points: CyclingRoutePointResponse[]
}