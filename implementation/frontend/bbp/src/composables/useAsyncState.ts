/**
 * Composable for managing async operation state with automatic error handling.
 * Tracks loading, error, and data states while providing centralized error handling with logging and toast notifications.
 * Supports optional success/error callbacks and field-level error highlighting for forms.
 */
import { ref } from 'vue'
import { catchApiError } from '@/utils/error'

/**
 * Composable that provides async operation state management with automatic error handling.
 * Manages loading, error, and data states for async operations with consistent error handling.
 * @template T - Type of data returned by async operations
 * @returns Object containing reactive state refs and execute method
 */
export function useAsyncState<T>() {
    /** Result data from successful async operation (null if not yet executed or failed) */
    const data = ref<T | null>(null)
    /** Loading state flag (true during async operation execution) */
    const isLoading = ref(false)
    /** Error message from failed operation (null if no error or operation in progress) */
    const error = ref<string | null>(null)
    /**
     * Executes async function with automatic state management and error handling.
     * Updates loading state, handles errors with logging/toast, and triggers callbacks.
     * @param asyncFn - Async function to execute (should return data of type T)
     * @param context - Context identifier for error logging (e.g., 'BikePathCreate.submit')
     * @param onSuccess - Optional callback executed with result data on success
     * @param onError - Optional callback executed with error message on failure
     * @param setFieldError - Optional callback to highlight specific field with validation error
     * @returns Promise resolving to result data on success or null on error
     */
    async function execute(
        asyncFn: () => Promise<T>,
        context: string,
        onSuccess?: (data: T) => void,
        onError?: (error: string) => void,
        setFieldError?: (field: string) => void
    ): Promise<T | null> {
        isLoading.value = true
        error.value = null // Clear previous error before new operation
        try {
            // Execute async operation and store result
            const result = await asyncFn()
            data.value = result
            onSuccess?.(result) // Trigger success callback if provided
            return result
        } catch (e) {
            // Handle error with centralized error handling (logging + toast)
            const errorMsg = catchApiError(e, context, setFieldError)
            error.value = errorMsg
            onError?.(errorMsg) // Trigger error callback if provided
            return null
        } finally {
            // Always reset loading state after operation completes
            isLoading.value = false
        }
    }
    return { data, isLoading, error, execute }
}