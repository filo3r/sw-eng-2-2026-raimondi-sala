/**
 * Authentication store managing JWT tokens and user state.
 * Persists authentication state in localStorage for session persistence across page reloads.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Pinia store for authentication management.
 * Handles JWT token storage, user ID tracking, and authentication state.
 * Uses composition API style with setup function.
 */
export const useAuthStore = defineStore('auth', () => {
    // Initialize token from localStorage on app load (persists across page reloads)
    const token = ref<string | null>(localStorage.getItem('jwt'))
    /** Currently authenticated user ID (null if not authenticated) */
    const userId = ref<number | null>(null)
    /** Computed flag indicating if user is currently authenticated (true if token exists) */
    const isAuthenticated = computed(() => !!token.value)
    /**
     * Sets authentication data and persists to localStorage.
     * Called after successful login or registration.
     * @param newToken - JWT access token from backend authentication response
     * @param newUserId - Authenticated user's unique identifier
     */
    function setAuth(newToken: string, newUserId: number) {
        token.value = newToken
        userId.value = newUserId
        // Persist token to localStorage for session persistence
        localStorage.setItem('jwt', newToken)
    }
    /**
     * Clears authentication data and removes from localStorage.
     * Called on logout or when token becomes invalid.
     */
    function clearAuth() {
        token.value = null
        userId.value = null
        // Remove token from localStorage to complete logout
        localStorage.removeItem('jwt')
    }
    return {
        token,
        userId,
        isAuthenticated,
        setAuth,
        clearAuth
    }
})