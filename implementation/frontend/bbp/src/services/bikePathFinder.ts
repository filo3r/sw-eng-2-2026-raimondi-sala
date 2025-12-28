/*
 * Bike path finder service for geographic search operations.
 */

import api from '@/api/axios'
import type { BikePathFinderRequest, PagedBikePathResponse } from '@/types/bikePath'

/**
 * Finds published bike paths within geographic radius.
 * @param request - Search criteria with addresses and radii
 * @param page - Page number (0-indexed)
 * @param size - Items per page
 * @returns Paginated bike paths matching search criteria
 */
export async function findBikePaths(
    request: BikePathFinderRequest,
    page: number = 0,
    size: number = 5
): Promise<PagedBikePathResponse> {
    const response = await api.post<PagedBikePathResponse>('/api/finder/bike-paths', request, {
        params: { page, size }
    })
    return response.data
}