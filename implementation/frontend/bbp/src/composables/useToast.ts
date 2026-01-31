/**
 * Toast notification composable using singleton pattern.
 * Provides global toast notification system with automatic dismissal after timeout.
 * Toasts are managed in a reactive array and automatically removed after display duration.
 */
import { ref } from 'vue'

/**
 * Toast notification structure for display.
 */
interface Toast {
    /** Unique identifier for toast removal (timestamp-based) */
    id: number
    /** Message text to display in toast */
    message: string
    /** Visual style type (success: green, error: red, info: blue) */
    type: 'success' | 'error' | 'info'
}

/** Reactive array of currently displayed toasts (singleton state shared across components) */
const toasts = ref<Toast[]>([])
/** Auto-dismiss timeout duration in milliseconds (5 seconds) */
const TOAST_DURATION = 5000

/**
 * Composable for managing global toast notifications.
 * Returns singleton toast state and methods for displaying notifications.
 * @returns Object containing reactive toasts array and show method
 */
export function useToast() {
    /**
     * Shows a new toast notification with auto-dismiss behavior.
     * Toast is automatically removed after TOAST_DURATION milliseconds.
     * @param message - Text message to display in the toast
     * @param type - Toast visual style type (default: 'error')
     */
    function show(message: string, type: 'success' | 'error' | 'info' = 'error') {
        const id = Date.now() // Generate unique ID based on current timestamp
        toasts.value.push({ id, message, type })
        // Schedule automatic removal after TOAST_DURATION
        setTimeout(() => {
            toasts.value = toasts.value.filter(t => t.id !== id)
        }, TOAST_DURATION)
    }
    return { toasts, show }
}