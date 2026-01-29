/**
 * Mapbox API service for geocoding and routing.
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
 * Gets Mapbox access token for client-side usage.
 * Public endpoint - no authentication required.
 */
export async function getMapboxAccessToken(): Promise<{ mapboxAccessToken: string }> {
    const response = await api.get<{ mapboxAccessToken: string }>('/api/mapbox/access-token')
    return response.data
}

/**
 * Converts an address to geographic coordinates (forward geocoding).
 */
export async function forwardGeocode(request: ForwardGeocodeRequest): Promise<GeocodeResponse> {
    const response = await api.post<GeocodeResponse>('/api/mapbox/geocode/forward', request)
    return response.data
}

/**
 * Converts geographic coordinates to address (reverse geocoding).
 */
export async function reverseGeocode(request: ReverseGeocodeRequest): Promise<GeocodeResponse> {
    validateCoordinates(request.coordinate)
    const response = await api.post<GeocodeResponse>('/api/mapbox/geocode/reverse', request)
    return response.data
}

/**
 * Calculates cycling route through multiple waypoints.
 */
export async function calculateCyclingRoute(request: CyclingRouteRequest): Promise<CyclingRouteResponse> {
    request.waypoints.forEach(validateCoordinates)
    const response = await api.post<CyclingRouteResponse>('/api/mapbox/cycling-route', request)
    return response.data
}