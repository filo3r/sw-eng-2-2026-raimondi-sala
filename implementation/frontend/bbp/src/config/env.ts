/**
 * Centralized environment configuration management.
 * Handles dev/prod environment variable resolution with fallbacks.
 */

type EnvVar = 'MAPBOX_API_KEY' | 'BACKEND_URL'

interface EnvConfig {
    dev: () => string
    prod: () => string
    fallback?: string
    required?: boolean
}

const ENV_MAP: Record<EnvVar, EnvConfig> = {
    MAPBOX_API_KEY: {
        dev: () => import.meta.env.VITE_MAPBOX_API_KEY || '',
        prod: () => window.MAPBOX_API_KEY || '',
        required: true
    },
    BACKEND_URL: {
        dev: () => {
            const port = import.meta.env.VITE_BACKEND_PORT || '8080'
            return `http://localhost:${port}`
        },
        prod: () => window.BACKEND_URL || '',
        fallback: 'http://localhost:8080'
    }
}

/**
 * Retrieves environment variable value based on current environment.
 * @param key - Environment variable key
 * @returns Environment variable value
 * @throws {Error} If required variable is not found
 */
export function getEnv(key: EnvVar): string {
    const config = ENV_MAP[key]
    const isDev = import.meta.env.DEV

    let value = isDev ? config.dev() : config.prod()

    if (!value && config.fallback) {
        value = config.fallback
    }

    if (!value && config.required) {
        throw new Error(`Required environment variable ${key} not found`)
    }

    return value
}