/**
 * Request for user login authentication.
 */
export interface UserLoginRequest {
    /** User email address for authentication */
    email: string
    /** User password for authentication */
    password: string
}

/**
 * Request for new user registration.
 */
export interface UserRegisterRequest {
    /** User's first name (max 50 characters) */
    name: string
    /** User's last name (max 50 characters) */
    surname: string
    /** Unique username (max 50 characters) */
    username: string
    /** User's email address (max 150 characters) */
    email: string
    /** User's password (min 8 characters) */
    password: string
}

/**
 * Request for updating user information with partial updates.
 * All fields are optional, only provided fields will be updated.
 */
export interface UserUpdateRequest {
    /** User's first name (max 50 characters, optional) */
    name?: string
    /** User's last name (max 50 characters, optional) */
    surname?: string
    /** Unique username (max 50 characters, optional) */
    username?: string
    /** User's email address (max 150 characters, optional) */
    email?: string
    /** User's new password (min 8 characters, optional) */
    password?: string
}

/**
 * Response for successful user authentication.
 * Contains JWT token and user identifier.
 */
export interface UserAuthResponse {
    /** JWT access token for subsequent authenticated requests */
    token: string
    /** Unique user identifier */
    userId: number
}

/**
 * Response containing user information without sensitive data.
 * Password and other sensitive fields are excluded.
 */
export interface UserResponse {
    /** Unique user identifier */
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