import { ref } from 'vue'
import type { Ref } from 'vue'
import { catchApiError } from '@/utils/error'
import { SPINNER_DELAY_MS } from '@/constants/ui'

interface PaginatedResponse<T> {
    content: T[]
    hasNext: boolean
}

export function usePagination<T>(pageSize: number) {
    const items = ref<T[]>([]) as Ref<T[]>
    const currentPage = ref(0)
    const hasMore = ref(false)
    const isLoading = ref(false)
    const isLoadingMore = ref(false)

    let loadMoreTimeout: number | null = null

    async function loadInitial(
        fetchFn: (page: number, size: number) => Promise<PaginatedResponse<T>>,
        spinnerDelay = SPINNER_DELAY_MS
    ) {
        currentPage.value = 0

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

    async function loadMore(
        fetchFn: (page: number, size: number) => Promise<PaginatedResponse<T>>,
        spinnerDelay = SPINNER_DELAY_MS
    ) {
        currentPage.value++

        loadMoreTimeout = window.setTimeout(() => {
            isLoadingMore.value = true
        }, spinnerDelay)

        try {
            const response = await fetchFn(currentPage.value, pageSize)
            items.value.push(...response.content)
            hasMore.value = response.hasNext
        } catch (error) {
            catchApiError(error, 'usePagination.loadMore')
            currentPage.value-- // Rollback page on error
        } finally {
            if (loadMoreTimeout) clearTimeout(loadMoreTimeout)
            isLoadingMore.value = false
        }
    }

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