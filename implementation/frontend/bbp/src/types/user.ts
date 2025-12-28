/**
 * Request for user login.
 */
export interface UserLoginRequest {
    /** User email for authentication */
    email: string

    /** User password */
    password: string
}

/**
 * Request for user registration.
 */
export interface UserRegisterRequest {
    /**
     * User's first name.
     * Maximum 50 characters.
     */
    name: string

    /**
     * User's last name.
     * Maximum 50 characters.
     */
    surname: string

    /**
     * Unique username.
     * Maximum 50 characters.
     */
    username: string

    /**
     * User's email address.
     * Maximum 150 characters.
     */
    email: string

    /**
     * User's password.
     * Minimum 8 characters.
     */
    password: string
}

/**
 * Request for user update with partial updates.
 * All fields are optional, only non-null fields will be updated.
 */
export interface UserUpdateRequest {
    /**
     * User's first name (optional).
     * Maximum 50 characters.
     */
    name?: string

    /**
     * User's last name (optional).
     * Maximum 50 characters.
     */
    surname?: string

    /**
     * Unique username (optional).
     * Maximum 50 characters.
     */
    username?: string

    /**
     * User's email address (optional).
     * Maximum 150 characters.
     */
    email?: string

    /**
     * User's new password (optional).
     * Minimum 8 characters.
     */
    password?: string
}

// ==================== RESPONSE DTOs ====================

/**
 * Response for user authentication containing JWT token and user ID.
 */
export interface UserAuthResponse {
    /** JWT access token for subsequent authenticated requests */
    token: string

    /** User ID */
    userId: number
}

/**
 * Response for user information without sensitive data.
 * Excludes password and other sensitive fields.
 */
export interface UserResponse {
    /** User ID */
    id: number

    /** User's first name */
    name: string

    /** User's last name */
    surname: string

    /** Unique username */
    username: string

    /** User's email address */
    email: string
}