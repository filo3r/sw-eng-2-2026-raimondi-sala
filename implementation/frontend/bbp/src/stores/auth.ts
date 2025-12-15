import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(localStorage.getItem('jwt'))
    const userId = ref<number | null>(null)

    const isAuthenticated = computed(() => !!token.value)

    function setAuth(newToken: string, newUserId: number) {
        token.value = newToken
        userId.value = newUserId
        localStorage.setItem('jwt', newToken)
    }

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