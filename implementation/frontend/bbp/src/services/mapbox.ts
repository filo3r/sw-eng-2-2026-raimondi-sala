/**
 * Mapbox API service for geocoding and routing operations.
 * Provides address-to-coordinate conversion, reverse geocoding, and cycling route calculation.
 */
import api from '@/api/axios'
import { validateCoordinates } from '@/utils/validation'
import type {
    ForwardGeocodeRequest,
    ReverseGeocodeRequest,
    CyclingRouteRequest,
    GeocodeResponse,
    CyclingRouteResponse
} from '@/types/mapbox'

/**
 * Retrieves Mapbox access token for client-side map rendering and APIs.
 * Public endpoint that does not require authentication.
 * @returns Promise resolving to object containing Mapbox access token
 * @throws {Error} If token retrieval fails or server error occurs
 */
export async function getMapboxAccessToken(): Promise<{ mapboxAccessToken: string }> {
    const response = await api.get<{ mapboxAccessToken: string }>('/api/mapbox/access-token')
    return response.data
}

/**
 * Converts a human-readable address to geographic coordinates (forward geocoding).
 * Uses Mapbox Geocoding API to find latitude and longitude for the given address.
 * @param request - Request containing address string to geocode
 * @returns Promise resolving to geocoded address with coordinates
 * @throws {Error} If address cannot be geocoded or validation fails
 */
export async function forwardGeocode(request: ForwardGeocodeRequest): Promise<GeocodeResponse> {
    const response = await api.post<GeocodeResponse>('/api/mapbox/geocode/forward', request)
    return response.data
}

/**
 * Converts geographic coordinates to a human-readable address (reverse geocoding).
 * Uses Mapbox Geocoding API to find the nearest address for given coordinates.
 * @param request - Request containing coordinate to reverse geocode
 * @returns Promise resolving to address string with original coordinates
 * @throws {Error} If coordinates are invalid, out of range, or geocoding fails
 */
export async function reverseGeocode(request: ReverseGeocodeRequest): Promise<GeocodeResponse> {
    // Validate coordinates before sending to backend
    validateCoordinates(request.coordinate)
    const response = await api.post<GeocodeResponse>('/api/mapbox/geocode/reverse', request)
    return response.data
}

/**
 * Calculates an optimized cycling route through multiple waypoints.
 * Uses Mapbox Directions API with cycling profile to generate turn-by-turn route.
 * @param request - Request containing ordered list of waypoints (min 2)
 * @returns Promise resolving to calculated route with ordered points
 * @throws {Error} If waypoints are invalid, too few/many, or route calculation fails
 */
export async function calculateCyclingRoute(request: CyclingRouteRequest): Promise<CyclingRouteResponse> {
    // Validate all waypoint coordinates before sending to backend
    request.waypoints.forEach(validateCoordinates)
    const response = await api.post<CyclingRouteResponse>('/api/mapbox/cycling-route', request)
    return response.data
}