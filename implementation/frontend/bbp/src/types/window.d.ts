/**
 * Global window variables injected by server.ts in production.
 * These are NOT available in development mode - use import.meta.env.VITE_* instead.
 */
export {};

declare global {
    interface Window {
        /**
         * Mapbox API access token for map rendering and geocoding services.
         * Injected from --mapbox.api.key CLI argument in production mode.
         */
        MAPBOX_API_KEY?: string;
        /**
         * Backend API base URL for HTTP requests.
         * Injected from --backend.port CLI argument in production mode.
         */
        BACKEND_URL?: string;
    }
}