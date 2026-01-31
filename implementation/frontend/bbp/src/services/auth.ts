/**
 * Authentication service for user registration and login operations.
 * Handles user account creation and authentication, returning JWT tokens for session management.
 */
import api from '@/api/axios'
import type { UserRegisterRequest, UserLoginRequest } from '@/types/user'
import type { UserAuthResponse } from '@/types/user'

/**
 * Registers a new user account with provided credentials.
 * Creates user account and returns JWT token for immediate authentication.
 * All fields are validated for format and uniqueness (email, username).
 * @param request - Registration data with name, surname, username, email, and password
 * @returns Promise resolving to authentication response with JWT token and user ID
 * @throws {Error} If validation fails (duplicate email/username, weak password, invalid format)
 */
export async function register(request: UserRegisterRequest): Promise<UserAuthResponse> {
    const response = await api.post<UserAuthResponse>('/api/auth/register', request)
    return response.data
}

/**
 * Authenticates a user with email and password credentials.
 * Returns JWT token on successful authentication for subsequent API requests.
 * @param request - Login credentials with email and password
 * @returns Promise resolving to authentication response with JWT token and user ID
 * @throws {Error} If credentials are invalid or user account does not exist
 */
export async function login(request: UserLoginRequest): Promise<UserAuthResponse> {
    const response = await api.post<UserAuthResponse>('/api/auth/login', request)
    return response.data
}