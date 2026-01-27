/**
 * Mapbox API service for geocoding and routing.
 */
import api from '@/api/axios'
import type {
    ForwardGeocodeRequest,
    ReverseGeocodeRequest,
    CyclingRouteRequest,
    GeocodeResponse,
    CyclingRouteResponse
} from '@/types/mapbox'

/**
 * Gets Mapbox access token for client-side usage.
 * Public endpoint - no authentication required.
 * @returns Object containing the Mapbox access token
 */
export async function getMapboxAccessToken(): Promise<{ mapboxAccessToken: string }> {
    const response = await api.get<{ mapboxAccessToken: string }>('/api/mapbox/access-token')
    return response.data
}

/**
 * Converts an address to geographic coordinates (forward geocoding).
 * @param request - Address to geocode
 * @returns Geocode response with formatted address and coordinates
 */
export async function forwardGeocode(request: ForwardGeocodeRequest): Promise<GeocodeResponse> {
    const response = await api.post<GeocodeResponse>('/api/mapbox/geocode/forward', request)
    return response.data
}

/**
 * Converts geographic coordinates to address (reverse geocoding).
 * @param request - Coordinates to convert
 * @returns Geocode response with formatted address and coordinates
 */
export async function reverseGeocode(request: ReverseGeocodeRequest): Promise<GeocodeResponse> {
    const response = await api.post<GeocodeResponse>('/api/mapbox/geocode/reverse', request)
    return response.data
}

/**
 * Calculates cycling route through multiple waypoints.
 * @param request - Waypoints for the route
 * @returns Route response with ordered points
 */
export async function calculateCyclingRoute(request: CyclingRouteRequest): Promise<CyclingRouteResponse> {
    const response = await api.post<CyclingRouteResponse>('/api/mapbox/cycling-route', request)
    return response.data
}