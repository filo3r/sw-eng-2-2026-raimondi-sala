/**
 * API error parsing utilities.
 * Handles all error response formats from GlobalExceptionHandler.
 */

/**
 * Error response structure from backend.
 */
interface ApiErrorResponse {
    timestamp?: string
    status?: number
    error?: string
    message?: string
    errors?: Record<string, string> // Field validation errors
    path?: string
    requestId?: string
    supportedMethods?: string[] // For HttpRequestMethodNotSupportedException
}

/**
 * Parses API error responses into user-friendly messages.
 * Handles all GlobalExceptionHandler response formats:
 * - Validation errors (MethodArgumentNotValidException, ConstraintViolationException)
 * - Standard errors (all other exceptions with message field)
 * - Network errors (no response)
 *
 * @param error - Axios error object
 * @returns User-friendly error message
 */
export function parseApiError(error: any): string {
    // Network error (no response from server)
    if (!error.response) {
        return 'Network error. Please check your connection.'
    }

    const data: ApiErrorResponse = error.response.data

    // Validation errors - only messages, no field names
    if (data.errors && Object.keys(data.errors).length > 0) {
        return Object.values(data.errors).join('\n')
    }

    // Standard error - single message
    if (data.message) {
        return data.message
    }

    // Fallback to error field or generic message
    if (data.error) {
        return data.error
    }

    // Last resort fallback
    return 'An unexpected error occurred'
}

/**
 * Parses API error and extracts HTTP status code.
 * Useful for conditional error handling based on status.
 *
 * @param error - Axios error object
 * @returns HTTP status code or null if not available
 */
export function getErrorStatus(error: any): number | null {
    return error.response?.status || null
}

/**
 * Parses API error and extracts request ID for support purposes.
 *
 * @param error - Axios error object
 * @returns Request ID or null if not available
 */
export function getErrorRequestId(error: any): string | null {
    return error.response?.data?.requestId || null
}

/**
 * Checks if error is a validation error (400 with errors object).
 *
 * @param error - Axios error object
 * @returns True if validation error, false otherwise
 */
export function isValidationError(error: any): boolean {
    return (
        error.response?.status === 400 &&
        error.response?.data?.errors &&
        Object.keys(error.response.data.errors).length > 0
    )
}

/**
 * Checks if error is an authentication error (401).
 *
 * @param error - Axios error object
 * @returns True if authentication error, false otherwise
 */
export function isAuthenticationError(error: any): boolean {
    return error.response?.status === 401
}

/**
 * Checks if error is an authorization error (403).
 *
 * @param error - Axios error object
 * @returns True if authorization error, false otherwise
 */
export function isAuthorizationError(error: any): boolean {
    return error.response?.status === 403
}

/**
 * Checks if error is a not found error (404).
 *
 * @param error - Axios error object
 * @returns True if not found error, false otherwise
 */
export function isNotFoundError(error: any): boolean {
    return error.response?.status === 404
}

/**
 * Checks if error is a conflict error (409).
 *
 * @param error - Axios error object
 * @returns True if conflict error, false otherwise
 */
export function isConflictError(error: any): boolean {
    return error.response?.status === 409
}