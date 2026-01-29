/**
 * Gets the Mapbox API key from environment (dev) or window (production).
 * @returns Mapbox API key
 * @throws {Error} If key not found in production
 */
import { getEnv } from './env'

export const getMapboxApiKey = (): string => {
    return getEnv('MAPBOX_API_KEY')
}