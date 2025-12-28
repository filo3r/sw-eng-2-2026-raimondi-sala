/*
 * Authentication service for user registration and login.
 */

import api from '@/api/axios'
import type { UserRegisterRequest, UserLoginRequest } from '@/types/user'
import type { UserAuthResponse } from '@/types/user'

/**
 * Registers a new user.
 * @param request - Registration data
 * @returns Authentication response with JWT token
 */
export async function register(request: UserRegisterRequest): Promise<UserAuthResponse> {
    const response = await api.post<UserAuthResponse>('/api/auth/register', request)
    return response.data
}

/**
 * Authenticates a user.
 * @param request - Login credentials
 * @returns Authentication response with JWT token
 */
export async function login(request: UserLoginRequest): Promise<UserAuthResponse> {
    const response = await api.post<UserAuthResponse>('/api/auth/login', request)
    return response.data
}