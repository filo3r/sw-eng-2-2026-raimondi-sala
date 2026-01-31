/**
 * Composable for managing field-level error highlighting with auto-clear functionality.
 * Tracks a single error field at a time and automatically clears it after timeout.
 * Used for highlighting invalid fields after form submission errors.
 */
import { ref } from 'vue'

/**
 * Composable that provides field error state management with auto-clear behavior.
 * Maintains single active error field and automatically clears it after 5 seconds.
 * @returns Object containing methods to set, clear, and check field error state
 */
export function useFieldError() {
    /** Currently highlighted error field name (null if no error) */
    const errorField = ref<string | null>(null)
    /**
     * Sets the error field and schedules auto-clear after 5 seconds.
     * Only one field can be highlighted at a time (replaces previous error).
     * @param field - Name of the field to highlight as error
     */
    function setError(field: string) {
        errorField.value = field
        // Auto-clear error after 5 seconds if it hasn't changed
        setTimeout(() => {
            if (errorField.value === field) errorField.value = null
        }, 5000)
    }
    /**
     * Manually clears the current error field immediately.
     * Useful for clearing error before timeout expires.
     */
    function clearError() {
        errorField.value = null
    }
    /**
     * Checks if a specific field is currently marked as error.
     * Used for conditionally applying error styling to form inputs.
     * @param field - Name of the field to check
     * @returns True if the field is currently marked as error
     */
    function hasError(field: string): boolean {
        return errorField.value === field
    }
    return { setError, clearError, hasError }
}