/**
 * Centralized environment configuration management.
 * Handles dev/prod environment variable resolution with type-safe fallbacks.
 * Supports both Vite environment variables (dev) and window-injected values (prod).
 */

/** Supported environment variable keys for type-safe access */
type EnvVar = 'MAPBOX_API_KEY' | 'BACKEND_URL'

/**
 * Environment configuration for a single variable.
 * Defines how to retrieve values in dev/prod with optional fallback and requirement flag.
 */
interface EnvConfig {
    /** Function to retrieve value in development mode (from import.meta.env) */
    dev: () => string
    /** Function to retrieve value in production mode (from window object) */
    prod: () => string
    /** Optional fallback value if dev/prod retrieval returns empty string */
    fallback?: string
    /** Whether variable is required (throws error if not found, default: false) */
    required?: boolean
}

/** Mapping of environment variable keys to their retrieval and fallback configuration */
const ENV_MAP: Record<EnvVar, EnvConfig> = {
    MAPBOX_API_KEY: {
        dev: () => import.meta.env.VITE_MAPBOX_API_KEY || '',
        prod: () => window.MAPBOX_API_KEY || '',
        required: true // Must be configured in all environments
    },
    BACKEND_URL: {
        dev: () => {
            // In dev, construct URL from configurable port (default: 8080)
            const port = import.meta.env.VITE_BACKEND_PORT || '8080'
            return `http://localhost:${port}`
        },
        prod: () => window.BACKEND_URL || '',
        fallback: 'http://localhost:8080' // Default backend URL if not configured
    }
}

/**
 * Retrieves environment variable value based on current runtime environment.
 * Automatically selects dev or prod retrieval strategy and applies fallbacks.
 * @param key - Environment variable key to retrieve
 * @returns Environment variable value string
 * @throws {Error} If required variable is not found and no fallback is available
 */
export function getEnv(key: EnvVar): string {
    const config = ENV_MAP[key]
    const isDev = import.meta.env.DEV
    // Retrieve value based on environment (dev uses import.meta.env, prod uses window)
    let value = isDev ? config.dev() : config.prod()
    // Apply fallback if value is empty and fallback is configured
    if (!value && config.fallback) {
        value = config.fallback
    }
    // Throw error if value is still empty and variable is required
    if (!value && config.required) {
        throw new Error(`Required environment variable ${key} not found`)
    }
    return value
}