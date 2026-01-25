/**
 * Bike path service for CRUD and search operations.
 */
import api from '@/api/axios'
import type {
    BikePathResponse,
    PagedBikePathResponse,
    BikePathManualCreateRequest,
    BikePathSearchRequest,
    BikePathUpdateRequest
} from '@/types/bikePath'

/**
 * Retrieves a bike path by ID.
 * @param id - Bike path ID
 * @returns Bike path with points and obstacles
 */
export async function getBikePathById(id: number): Promise<BikePathResponse> {
    const response = await api.get<BikePathResponse>(`/api/bike-paths/${id}`)
    return response.data
}

/**
 * Retrieves paginated bike paths created by authenticated user.
 * @param page - Page number (0-indexed)
 * @param size - Items per page
 * @param sortBy - Field to sort by
 * @param direction - Sort direction (ASC or DESC)
 * @returns Paginated bike paths
 */
export async function getUserBikePaths(
    page: number = 0,
    size: number = 6,
    sortBy: string = 'createdAt',
    direction: string = 'DESC'
): Promise<PagedBikePathResponse> {
    const response = await api.get<PagedBikePathResponse>('/api/bike-paths', {
        params: { page, size, sortBy, direction }
    })
    return response.data
}

/**
 * Searches bike paths with filters.
 * @param searchRequest - Search criteria
 * @param page - Page number (0-indexed)
 * @param size - Items per page
 * @param sortBy - Field to sort by
 * @param direction - Sort direction (ASC or DESC)
 * @returns Paginated search results
 */
export async function searchBikePaths(
    searchRequest: BikePathSearchRequest,
    page: number = 0,
    size: number = 6,
    sortBy: string = 'createdAt',
    direction: string = 'DESC'
): Promise<PagedBikePathResponse> {
    const response = await api.post<PagedBikePathResponse>('/api/bike-paths/search', searchRequest, {
        params: { page, size, sortBy, direction }
    })
    return response.data
}

/**
 * Creates a new bike path manually.
 * @param request - Bike path creation data
 * @returns Created bike path
 */
export async function createBikePathManually(
    request: BikePathManualCreateRequest
): Promise<BikePathResponse> {
    const response = await api.post<BikePathResponse>('/api/bike-paths/manual', request)
    return response.data
}

/**
 * Updates an existing bike path.
 * @param id - Bike path ID
 * @param request - Update data
 * @returns Updated bike path
 */
export async function updateBikePath(
    id: number,
    request: BikePathUpdateRequest
): Promise<BikePathResponse> {
    const response = await api.patch<BikePathResponse>(`/api/bike-paths/${id}`, request)
    return response.data
}

/**
 * Deletes a bike path.
 * @param id - Bike path ID
 */
export async function deleteBikePath(id: number): Promise<void> {
    await api.delete(`/api/bike-paths/${id}`)
}