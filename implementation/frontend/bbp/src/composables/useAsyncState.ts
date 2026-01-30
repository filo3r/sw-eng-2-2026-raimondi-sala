/**
 * Composable for managing async operation state (loading, error, data).
 * Provides automatic error handling with logging and toast notifications.
 */
import { ref } from 'vue'
import { catchApiError } from '@/utils/error'

export function useAsyncState<T>() {
    const data = ref<T | null>(null)
    const isLoading = ref(false)
    const error = ref<string | null>(null)

    /**
     * Executes async function with automatic state management.
     * @param asyncFn - Async function to execute
     * @param context - Context for error logging
     * @param onSuccess - Optional callback on success
     * @param onError - Optional callback on error
     * @param setFieldError
     * @returns Promise with result
     */
    async function execute(
        asyncFn: () => Promise<T>,
        context: string,
        onSuccess?: (data: T) => void,
        onError?: (error: string) => void,
        setFieldError?: (field: string) => void
    ): Promise<T | null> {
        isLoading.value = true
        error.value = null

        try {
            const result = await asyncFn()
            data.value = result
            onSuccess?.(result)
            return result
        } catch (e) {
            const errorMsg = catchApiError(e, context, setFieldError)
            error.value = errorMsg
            onError?.(errorMsg)
            return null
        } finally {
            isLoading.value = false
        }
    }

    return { data, isLoading, error, execute }
}