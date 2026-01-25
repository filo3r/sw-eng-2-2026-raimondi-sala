/**
 * Authentication store managing JWT tokens and user state.
 * Persists authentication state in localStorage.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
    // Initialize token from localStorage on app load
    const token = ref<string | null>(localStorage.getItem('jwt'))
    const userId = ref<number | null>(null)
    const isAuthenticated = computed(() => !!token.value)

    /**
     * Sets authentication data and persists to localStorage.
     * @param newToken - JWT token from backend
     * @param newUserId - Authenticated user ID
     */
    function setAuth(newToken: string, newUserId: number) {
        token.value = newToken
        userId.value = newUserId
        localStorage.setItem('jwt', newToken)
    }

    /**
     * Clears authentication data and removes from localStorage.
     */
    function clearAuth() {
        token.value = null
        userId.value = null
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