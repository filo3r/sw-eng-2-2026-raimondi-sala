/**
 * User service for profile management operations.
 */
import api from '@/api/axios'
import type { UserUpdateRequest, UserResponse } from '@/types/user'

/**
 * Retrieves current authenticated user's profile.
 * @returns User profile data
 */
export async function getCurrentUser(): Promise<UserResponse> {
    const response = await api.get<UserResponse>('/api/users/me')
    return response.data
}

/**
 * Updates current authenticated user's profile.
 * Only provided fields will be updated.
 * @param request - Update data with fields to modify
 * @returns Updated user profile
 */
export async function updateCurrentUser(request: UserUpdateRequest): Promise<UserResponse> {
    const response = await api.patch<UserResponse>('/api/users/me', request)
    return response.data
}

/**
 * Deletes current authenticated user's account.
 * All associated data will be deleted according to cascade rules.
 */
export async function deleteCurrentUser(): Promise<void> {
    await api.delete('/api/users/me')
}