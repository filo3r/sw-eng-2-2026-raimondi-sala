/**
 * User service for profile management operations.
 * Handles authenticated user profile retrieval, updates, and account deletion.
 */
import api from '@/api/axios'
import type { UserUpdateRequest, UserResponse } from '@/types/user'

/**
 * Retrieves the current authenticated user's profile information.
 * Requires valid JWT token in request headers.
 * @returns Promise resolving to user profile data without sensitive fields
 * @throws {Error} If user is not authenticated or request fails
 */
export async function getCurrentUser(): Promise<UserResponse> {
    const response = await api.get<UserResponse>('/api/users/me')
    return response.data
}

/**
 * Updates the current authenticated user's profile with partial data.
 * Only provided fields will be updated, others remain unchanged.
 * @param request - Update request containing fields to modify (all optional)
 * @returns Promise resolving to updated user profile data
 * @throws {Error} If validation fails or user is not authenticated
 */
export async function updateCurrentUser(request: UserUpdateRequest): Promise<UserResponse> {
    const response = await api.patch<UserResponse>('/api/users/me', request)
    return response.data
}

/**
 * Deletes the current authenticated user's account permanently.
 * All associated data (bike paths, trips, obstacles) will be handled according to cascade rules.
 * This action cannot be undone.
 * @throws {Error} If user is not authenticated or deletion fails
 */
export async function deleteCurrentUser(): Promise<void> {
    await api.delete('/api/users/me')
}