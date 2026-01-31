/**
 * Bike path finder service for geographic search operations.
 * Enables discovery of published bike paths within specified radius from origin and destination.
 */
import api from '@/api/axios'
import type { BikePathFinderRequest, PagedBikePathResponse } from '@/types/bikePath'

/**
 * Finds published bike paths within geographic radius from origin and destination addresses.
 * Searches for paths where origin and destination are within specified distances from search addresses.
 * Only returns published (public) bike paths visible to all users.
 * @param request - Search criteria with origin/destination addresses and search radii in kilometers
 * @param page - Page number (0-indexed, default: 0)
 * @param size - Number of items per page (default: 5)
 * @returns Promise resolving to paginated bike paths matching search criteria with all details
 * @throws {Error} If geocoding fails, validation fails, or search request is invalid
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