/**
 * Mapbox API key retrieval utility.
 * Handles environment-specific key access for development and production modes.
 */
import { getEnv } from './env'

/**
 * Retrieves the Mapbox API access token from environment variables or window object.
 * In development mode, reads from VITE_MAPBOX_API_KEY environment variable.
 * In production mode, reads from window.MAPBOX_API_KEY injected by server.
 * @returns Mapbox API access token for map rendering and API requests
 * @throws {Error} If key is not found or not configured in current environment
 */
export const getMapboxApiKey = (): string => {
    return getEnv('MAPBOX_API_KEY')
}