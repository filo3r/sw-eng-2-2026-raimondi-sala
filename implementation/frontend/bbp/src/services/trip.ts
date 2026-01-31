/**
 * Trip service for CRUD and search operations.
 * Handles trip recording, retrieval, search, and deletion with pagination support.
 */
import api from '@/api/axios'
import type {
    TripResponse,
    PagedTripResponse,
    TripManualRecordRequest,
    TripSearchRequest
} from '@/types/trip'

/**
 * Retrieves a single trip by its unique identifier.
 * Includes complete route points and meteorological data if available.
 * @param id - Unique trip identifier
 * @returns Promise resolving to trip with points and weather data
 * @throws {Error} If trip not found or user lacks permission to view it
 */
export async function getTripById(id: number): Promise<TripResponse> {
    const response = await api.get<TripResponse>(`/api/trips/${id}`)
    return response.data
}

/**
 * Retrieves paginated trips for the authenticated user with sorting options.
 * Returns only trips recorded by the current user.
 * @param page - Page number (0-indexed, default: 0)
 * @param size - Number of items per page (default: 6)
 * @param sortBy - Field name to sort by (default: 'startTime')
 * @param direction - Sort direction ASC or DESC (default: 'DESC')
 * @returns Promise resolving to paginated trips with navigation metadata
 * @throws {Error} If user is not authenticated or request fails
 */
export async function getUserTrips(
    page: number = 0,
    size: number = 6,
    sortBy: string = 'startTime',
    direction: string = 'DESC'
): Promise<PagedTripResponse> {
    const response = await api.get<PagedTripResponse>('/api/trips', {
        params: { page, size, sortBy, direction }
    })
    return response.data
}

/**
 * Searches trips with optional filters and pagination.
 * All filters are optional and can be combined. Empty request returns all user trips.
 * @param searchRequest - Search criteria (origin, destination, date range)
 * @param page - Page number (0-indexed, default: 0)
 * @param size - Number of items per page (default: 6)
 * @param sortBy - Field name to sort by (default: 'startTime')
 * @param direction - Sort direction ASC or DESC (default: 'DESC')
 * @returns Promise resolving to paginated search results
 * @throws {Error} If user is not authenticated or search validation fails
 */
export async function searchTrips(
    searchRequest: TripSearchRequest,
    page: number = 0,
    size: number = 6,
    sortBy: string = 'startTime',
    direction: string = 'DESC'
): Promise<PagedTripResponse> {
    const response = await api.post<PagedTripResponse>('/api/trips/search', searchRequest, {
        params: { page, size, sortBy, direction }
    })
    return response.data
}

/**
 * Creates a new trip manually by providing addresses and trip details.
 * Addresses will be geocoded and used to calculate the cycling route.
 * @param request - Trip recording data with addresses, times, and optional max speed
 * @returns Promise resolving to created trip with generated route and weather data
 * @throws {Error} If validation fails, geocoding fails, or user is not authenticated
 */
export async function createTripManually(request: TripManualRecordRequest): Promise<TripResponse> {
    const response = await api.post<TripResponse>('/api/trips/manual', request)
    return response.data
}

/**
 * Deletes a trip permanently.
 * Only the trip creator can delete their own trips.
 * @param id - Unique trip identifier to delete
 * @throws {Error} If trip not found, user lacks permission, or deletion fails
 */
export async function deleteTrip(id: number): Promise<void> {
    await api.delete(`/api/trips/${id}`)
}