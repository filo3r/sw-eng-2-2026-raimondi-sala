/**
 * Toast notification composable (singleton pattern).
 * Manages global toast notifications with auto-dismiss.
 */
import { ref } from 'vue'

interface Toast {
    id: number
    message: string
    type: 'success' | 'error' | 'info'
}

const toasts = ref<Toast[]>([])
const TOAST_DURATION = 5000 // Auto-dismiss timeout in ms

/**
 * Toast notification manager.
 * @returns Methods to show toasts and reactive toasts array
 */
export function useToast() {
    /**
     * Shows a toast notification.
     * @param message - Toast message text
     * @param type - Toast type (success/error/info)
     */
    function show(message: string, type: 'success' | 'error' | 'info' = 'error') {
        const id = Date.now()
        toasts.value.push({ id, message, type })
        setTimeout(() => {
            toasts.value = toasts.value.filter(t => t.id !== id)
        }, TOAST_DURATION)
    }
    return { toasts, show }
}