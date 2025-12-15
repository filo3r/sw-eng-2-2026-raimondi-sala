/**
 * Axios instance for backend API calls.
 * Base URL is automatically configured based on environment.
 */

import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

/**
 * Gets backend API base URL (dev: from env, prod: from window).
 */
const getBackendUrl = (): string => {
    // Development: from VITE_BACKEND_PORT
    if (import.meta.env.DEV) {
        const port = import.meta.env.VITE_BACKEND_PORT || '8080'
        return `http://localhost:${port}`
    }

    // Production: from window.BACKEND_URL
    if (window.BACKEND_URL) {
        return window.BACKEND_URL
    }

    // Fallback
    return 'http://localhost:8080'
}

const api = axios.create({
    baseURL: getBackendUrl(),
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// Request interceptor: Add JWT token to all requests
api.interceptors.request.use(
    (config) => {
        const authStore = useAuthStore()
        if (authStore.token) {
            config.headers.Authorization = `Bearer ${authStore.token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response interceptor: Handle 401 Unauthorized
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            const authStore = useAuthStore()
            authStore.clearAuth()

            // Only redirect if NOT already on login/register
            const currentPath = window.location.pathname
            if (currentPath !== '/login' && currentPath !== '/register') {
                window.location.href = '/login'
            }
        }
        return Promise.reject(error)
    }
)

export default api