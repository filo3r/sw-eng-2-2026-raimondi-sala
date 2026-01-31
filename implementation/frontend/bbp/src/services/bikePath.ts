/**
 * Bike path service for CRUD and search operations.
 * Handles bike path creation, retrieval, updates, search, and deletion with pagination support.
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
 * Retrieves a single bike path by its unique identifier.
 * Includes complete route points and all obstacles (active and inactive).
 * @param id - Unique bike path identifier
 * @returns Promise resolving to bike path with points and obstacles
 * @throws {Error} If bike path not found or user lacks permission to view it
 */
export async function getBikePathById(id: number): Promise<BikePathResponse> {
    const response = await api.get<BikePathResponse>(`/api/bike-paths/${id}`)
    return response.data
}

/**
 * Retrieves paginated bike paths created by the authenticated user with sorting options.
 * Returns only bike paths created by the current user (both published and unpublished).
 * @param page - Page number (0-indexed, default: 0)
 * @param size - Number of items per page (default: 6)
 * @param sortBy - Field name to sort by (default: 'createdAt')
 * @param direction - Sort direction ASC or DESC (default: 'DESC')
 * @returns Promise resolving to paginated bike paths with navigation metadata
 * @throws {Error} If user is not authenticated or request fails
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
 * Searches bike paths with optional filters and pagination.
 * All filters are optional and can be combined. Empty request returns all user bike paths.
 * @param searchRequest - Search criteria (origin, destination, date range)
 * @param page - Page number (0-indexed, default: 0)
 * @param size - Number of items per page (default: 6)
 * @param sortBy - Field name to sort by (default: 'createdAt')
 * @param direction - Sort direction ASC or DESC (default: 'DESC')
 * @returns Promise resolving to paginated search results
 * @throws {Error} If user is not authenticated or search validation fails
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
 * Creates a new bike path manually by providing addresses and optional obstacles.
 * Addresses will be geocoded and used to calculate the cycling route.
 * Obstacles are validated to be within reasonable distance from the route.
 * @param request - Bike path creation data with addresses, status, published flag, and obstacles
 * @returns Promise resolving to created bike path with generated route and score
 * @throws {Error} If validation fails, geocoding fails, or user is not authenticated
 */
export async function createBikePathManually(
    request: BikePathManualCreateRequest
): Promise<BikePathResponse> {
    const response = await api.post<BikePathResponse>('/api/bike-paths/manual', request)
    return response.data
}

/**
 * Updates an existing bike path with partial data.
 * Only provided fields will be updated, others remain unchanged.
 * Creator can always update, other users can only update published bike paths.
 * Route cannot be updated; to change route, create a new bike path.
 * @param id - Unique bike path identifier to update
 * @param request - Update data with optional fields and obstacle modifications
 * @returns Promise resolving to updated bike path with recalculated score
 * @throws {Error} If bike path not found, user lacks permission, validation fails, or optimistic lock fails
 */
export async function updateBikePath(
    id: number,
    request: BikePathUpdateRequest
): Promise<BikePathResponse> {
    const response = await api.patch<BikePathResponse>(`/api/bike-paths/${id}`, request)
    return response.data
}

/**
 * Deletes a bike path permanently.
 * Only the bike path creator can delete their own bike paths.
 * @param id - Unique bike path identifier to delete
 * @throws {Error} If bike path not found, user lacks permission, or deletion fails
 */
export async function deleteBikePath(id: number): Promise<void> {
    await api.delete(`/api/bike-paths/${id}`)
}