/**
 * Global window variables injected by server.ts in production.
 * These are NOT available in development mode - use import.meta.env.VITE_* instead.
 */
export {};

declare global {
    interface Window {
        /**
         * Mapbox API key (injected from --mapbox.api.key CLI argument)
         */
        MAPBOX_API_KEY?: string;

        /**
         * Backend API URL (injected from --backend.port CLI argument)
         */
        BACKEND_URL?: string;
    }
}