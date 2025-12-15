/**
 * Gets the Mapbox API key from environment (dev) or window (production).
 * @returns Mapbox API key
 * @throws {Error} If key not found in production
 */
export const getMapboxApiKey = (): string => {
    // Development: from .env
    if (import.meta.env.DEV) {
        return import.meta.env.VITE_MAPBOX_API_KEY || "";
    }

    // Production: injected by server.ts
    const key = window.MAPBOX_API_KEY;

    if (!key) {
        throw new Error(
            "Mapbox API key not found. Make sure the server is configured correctly."
        );
    }

    return key;
};