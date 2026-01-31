/**
 * Axios instance configuration for backend API communication.
 * Automatically configures base URL, timeout, and JWT authentication via interceptors.
 * Handles 401 Unauthorized responses by clearing auth state and redirecting to login.
 */
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { getEnv } from '@/config/env'
import router from '@/router'

/**
 * Resolves backend API base URL based on current environment.
 * Development: constructs URL from localhost and configurable port.
 * Production: uses window-injected URL from server configuration.
 * @returns Backend API base URL string (e.g., "http://localhost:8080")
 */
const getBackendUrl = (): string => {
    return getEnv('BACKEND_URL')
}

/** Configured Axios instance with base URL, timeout, and JSON content type */
const api = axios.create({
    baseURL: getBackendUrl(), // Base URL for all API requests
    timeout: 5000, // Request timeout in milliseconds (5 seconds)
    headers: {
        'Content-Type': 'application/json' // Default content type for requests
    }
})

/**
 * Request interceptor that injects JWT token into Authorization header.
 * Automatically adds Bearer token from auth store to all outgoing requests if user is authenticated.
 */
api.interceptors.request.use(
    (config) => {
        const authStore = useAuthStore()
        // Add JWT token to Authorization header if user is authenticated
        if (authStore.token) {
            config.headers.Authorization = `Bearer ${authStore.token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

/**
 * Response interceptor that handles 401 Unauthorized errors.
 * Clears authentication state and redirects to login page via Vue Router when token is invalid or expired.
 * Prevents redirect loop by checking current path before redirecting.
 */
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // Handle 401 Unauthorized (invalid/expired token)
        if (error.response?.status === 401) {
            const authStore = useAuthStore()
            authStore.clearAuth() // Clear token and user data from store and localStorage
            // Use Vue Router for SPA navigation (maintains app state vs window.location.href)
            const currentPath = window.location.pathname
            if (currentPath !== '/login' && currentPath !== '/register') {
                router.push('/login') // Navigate to login while preserving SPA behavior
            }
        }
        return Promise.reject(error)
    }
)

export default api