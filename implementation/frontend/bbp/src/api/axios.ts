/**
 * Axios instance for backend API calls.
 * Automatically configures base URL and handles JWT authentication.
 */
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { getEnv } from '@/config/env'

/**
 * Resolves backend API base URL based on environment.
 * @returns Backend URL string
 */
const getBackendUrl = (): string => {
    return getEnv('BACKEND_URL')
}

const api = axios.create({
    baseURL: getBackendUrl(),
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json'
    }
})

/**
 * Request interceptor: Injects JWT token into Authorization header.
 */
api.interceptors.request.use(
    (config) => {
        const authStore = useAuthStore()
        if (authStore.token) {
            config.headers.Authorization = `Bearer ${authStore.token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

/**
 * Response interceptor: Handles 401 Unauthorized by clearing auth and redirecting.
 */
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