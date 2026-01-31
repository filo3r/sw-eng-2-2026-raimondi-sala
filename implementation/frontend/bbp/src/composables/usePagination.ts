/**
 * Composable for managing paginated data with infinite scroll support.
 * Handles initial loading, load more functionality, loading states, and spinner delays.
 * Supports generic types for flexible use with any paginated API response.
 */
import { ref } from 'vue'
import type { Ref } from 'vue'
import { catchApiError } from '@/utils/error'
import { SPINNER_DELAY_MS } from '@/constants/ui'

/**
 * Paginated API response structure with content and navigation flag.
 * @template T - Type of items in the paginated response
 */
interface PaginatedResponse<T> {
    /** Array of items in current page */
    content: T[]
    /** Flag indicating if more pages are available */
    hasNext: boolean
}

/**
 * Composable that provides pagination logic with loading states and infinite scroll support.
 * Manages items list, current page, loading states, and provides methods to load data.
 * @template T - Type of items being paginated
 * @param pageSize - Number of items per page to request
 * @returns Object containing reactive state and methods for pagination management
 */
export function usePagination<T>(pageSize: number) {
    /** Reactive array of all loaded items across all pages */
    const items = ref<T[]>([]) as Ref<T[]>
    /** Current page number (0-indexed) */
    const currentPage = ref(0)
    /** Flag indicating if more pages are available to load */
    const hasMore = ref(false)
    /** Loading state for initial data fetch */
    const isLoading = ref(false)
    /** Loading state for load more operation */
    const isLoadingMore = ref(false)
    /** Timeout handle for delayed spinner display on load more */
    let loadMoreTimeout: number | null = null
    /**
     * Loads initial page of data and resets pagination state.
     * Shows loading spinner after delay to avoid flash on fast requests.
     * @param fetchFn - Function that fetches paginated data (receives page and size)
     * @param spinnerDelay - Delay in ms before showing spinner (default: SPINNER_DELAY_MS)
     */
    async function loadInitial(
        fetchFn: (page: number, size: number) => Promise<PaginatedResponse<T>>,
        spinnerDelay = SPINNER_DELAY_MS
    ) {
        currentPage.value = 0
        // Delay spinner to avoid flash on fast network responses
        const timeout = window.setTimeout(() => {
            isLoading.value = true
        }, spinnerDelay)
        try {
            const response = await fetchFn(0, pageSize)
            items.value = response.content
            hasMore.value = response.hasNext
        } catch (error) {
            catchApiError(error, 'usePagination.loadInitial')
            items.value = []
            hasMore.value = false
        } finally {
            clearTimeout(timeout)
            isLoading.value = false
        }
    }
    /**
     * Loads next page of data and appends to existing items.
     * Shows loading spinner after delay and rolls back page on error.
     * @param fetchFn - Function that fetches paginated data (receives page and size)
     * @param spinnerDelay - Delay in ms before showing spinner (default: SPINNER_DELAY_MS)
     */
    async function loadMore(
        fetchFn: (page: number, size: number) => Promise<PaginatedResponse<T>>,
        spinnerDelay = SPINNER_DELAY_MS
    ) {
        currentPage.value++
        // Delay spinner to avoid flash on fast network responses
        loadMoreTimeout = window.setTimeout(() => {
            isLoadingMore.value = true
        }, spinnerDelay)
        try {
            const response = await fetchFn(currentPage.value, pageSize)
            items.value.push(...response.content)
            hasMore.value = response.hasNext
        } catch (error) {
            catchApiError(error, 'usePagination.loadMore')
            currentPage.value-- // Rollback page increment on error
        } finally {
            if (loadMoreTimeout) clearTimeout(loadMoreTimeout)
            isLoadingMore.value = false
        }
    }
    /**
     * Resets pagination state to initial values.
     * Clears items, resets page counter, and cancels pending spinners.
     */
    function reset() {
        items.value = []
        currentPage.value = 0
        hasMore.value = false
        isLoading.value = false
        isLoadingMore.value = false
        if (loadMoreTimeout) clearTimeout(loadMoreTimeout)
    }
    return {
        items,
        currentPage,
        hasMore,
        isLoading,
        isLoadingMore,
        loadInitial,
        loadMore,
        reset
    }
}