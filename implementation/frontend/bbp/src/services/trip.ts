/*
 * Trip service for CRUD and search operations.
 */

import api from '@/api/axios'
import type {
    TripResponse,
    PagedTripResponse,
    TripManualRecordRequest,
    TripSearchRequest
} from '@/types/trip'

/**
 * Retrieves a trip by ID.
 * @param id - Trip ID
 * @returns Trip with points and meteorological data
 */
export async function getTripById(id: number): Promise<TripResponse> {
    const response = await api.get<TripResponse>(`/api/trips/${id}`)
    return response.data
}

/**
 * Retrieves paginated trips for authenticated user.
 * @param page - Page number (0-indexed)
 * @param size - Items per page
 * @param sortBy - Field to sort by
 * @param direction - Sort direction (ASC or DESC)
 * @returns Paginated trips
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
 * Searches trips with filters.
 * @param searchRequest - Search criteria
 * @param page - Page number (0-indexed)
 * @param size - Items per page
 * @param sortBy - Field to sort by
 * @param direction - Sort direction (ASC or DESC)
 * @returns Paginated search results
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
 * Creates a new trip manually.
 * @param request - Trip recording data
 * @returns Created trip
 */
export async function createTripManually(request: TripManualRecordRequest): Promise<TripResponse> {
    const response = await api.post<TripResponse>('/api/trips/manual', request)
    return response.data
}

/**
 * Deletes a trip.
 * @param id - Trip ID
 */
export async function deleteTrip(id: number): Promise<void> {
    await api.delete(`/api/trips/${id}`)
}