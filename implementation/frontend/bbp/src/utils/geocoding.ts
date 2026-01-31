/**
 * Geocoding utility functions.
 * Provides address <-> coordinate conversion with fallback formatting.
 */
import { reverseGeocode } from '@/services/mapbox'
import { catchApiError } from '@/utils/error'

/**
 * Converts coordinates to address using reverse geocoding.
 * Falls back to formatted coordinates if geocoding fails.
 * @param lng - Longitude
 * @param lat - Latitude
 * @param context - Context string for error logging (e.g., component name)
 * @returns Address string or formatted coordinates
 */
export async function getAddressFromCoordinates(
    lng: number,
    lat: number,
    context = 'geocoding'
): Promise<string> {
    try {
        const result = await reverseGeocode({
            coordinate: { latitude: lat, longitude: lng }
        })
        return result.address
    } catch (e) {
        catchApiError(e, `${context}.getAddressFromCoordinates`)
        return `${lat.toFixed(6)}, ${lng.toFixed(6)}`
    }
}