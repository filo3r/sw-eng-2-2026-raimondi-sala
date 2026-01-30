import { ref } from 'vue'

export function useFieldError() {
    const errorField = ref<string | null>(null)

    function setError(field: string) {
        errorField.value = field
        setTimeout(() => {
            if (errorField.value === field) errorField.value = null
        }, 5000) // Auto-clear dopo 5s
    }

    function clearError() {
        errorField.value = null
    }

    function hasError(field: string): boolean {
        return errorField.value === field
    }

    return { setError, clearError, hasError }
}