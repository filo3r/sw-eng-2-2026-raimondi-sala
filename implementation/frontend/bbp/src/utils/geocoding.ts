/**
 * Geocoding utility functions.
 * Provides address <-> coordinate conversion with fallback formatting.
 */
import { reverseGeocode } from '@/services/mapbox'
import { catchApiError } from '@/utils/error'

/**
 * Converts geographic coordinates to a human-readable address using reverse geocoding.
 * If the geocoding service fails, returns formatted coordinates as fallback (6 decimal precision).
 * @param lng - Longitude coordinate (-180 to 180)
 * @param lat - Latitude coordinate (-90 to 90)
 * @param context - Context identifier for error logging (default: 'geocoding')
 * @returns Promise resolving to address string or formatted coordinates on failure
 */
export async function getAddressFromCoordinates(
    lng: number,
    lat: number,
    context = 'geocoding'
): Promise<string> {
    try {
        // Attempt reverse geocoding via Mapbox service
        const result = await reverseGeocode({
            coordinate: { latitude: lat, longitude: lng }
        })
        return result.address
    } catch (e) {
        // Log error and return formatted coordinates as fallback
        catchApiError(e, `${context}.getAddressFromCoordinates`)
        return `${lat.toFixed(6)}, ${lng.toFixed(6)}`
    }
}