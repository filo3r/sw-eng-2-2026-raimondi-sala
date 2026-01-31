/**
 * API error parsing utilities with integrated logging.
 * Handles all error response formats from GlobalExceptionHandler.
 */
import { useToast } from '@/composables/useToast'

/**
 * Backend error response structure matching GlobalExceptionHandler format.
 * @property timestamp - ISO timestamp of error occurrence
 * @property status - HTTP status code
 * @property error - HTTP status text (e.g., "Bad Request")
 * @property message - Primary error message
 * @property errors - Validation errors map (field -> error message)
 * @property path - Request path that caused the error
 * @property requestId - Unique identifier for tracking
 * @property supportedMethods - Allowed HTTP methods (for 405 errors)
 */
interface ApiErrorResponse {
    timestamp?: string
    status?: number
    error?: string
    message?: string
    errors?: Record<string, string>
    path?: string
    requestId?: string
    supportedMethods?: string[]
}

/**
 * Log severity levels for error tracking and debugging.
 */
type LogLevel = 'error' | 'warn' | 'info'

/**
 * Structured log entry for console output.
 * @property timestamp - ISO timestamp of log entry
 * @property level - Severity level
 * @property context - Component or function context identifier
 * @property message - Human-readable log message
 * @property requestId - Backend request ID for correlation
 * @property status - HTTP status code
 * @property data - Additional error data
 */
interface LogEntry {
    timestamp: string
    level: LogLevel
    context: string
    message: string
    requestId?: string
    status?: number
    data?: any
}

/**
 * Internal logging function that formats and outputs structured log entries.
 * @param level - Log severity level
 * @param context - Context identifier for the log
 * @param message - Log message
 * @param additionalData - Optional extra data to include in log entry
 */
function log(level: LogLevel, context: string, message: string, additionalData?: any) {
    const entry: LogEntry = {
        timestamp: new Date().toISOString(),
        level,
        context,
        message,
        ...additionalData
    }
    // Output to console with appropriate method based on level
    console[level](`[${entry.level.toUpperCase()}] [${entry.context}]`, entry.message, entry)
}

/**
 * Parses API error responses into user-friendly messages.
 * Handles network errors, validation errors, and standard error responses.
 * @param error - Axios error object from failed request
 * @returns User-friendly error message string
 */
export function parseApiError(error: any): string {
    // Network error (no response from server)
    if (!error?.response) {
        return 'Network error. Please check your connection.'
    }
    const data: ApiErrorResponse = error.response.data
    // Validation errors - combine all error messages (field names excluded)
    if (data?.errors && Object.keys(data.errors).length > 0) {
        return Object.values(data.errors).join('\n')
    }
    // Standard error - return primary message
    if (data?.message) {
        return data.message
    }
    // Fallback to error field or generic message
    if (data?.error) {
        return data.error
    }
    return 'An unexpected error occurred'
}

/**
 * Logs API error with full context including request ID and status code.
 * Does not modify or return the error, only logs it.
 * @param error - Axios error object from failed request
 * @param context - Context identifier (e.g., 'BikePathCreate.submit')
 */
export function logError(error: any, context: string) {
    const requestId = error?.response?.data?.requestId
    const status = error?.response?.status
    // Log with parsed message and additional metadata
    log('error', context, parseApiError(error), {
        requestId,
        status,
        error: error?.response?.data || error?.message
    })
}

/**
 * Parses and logs error, then returns the parsed message.
 * Convenience wrapper combining parseApiError and logError.
 * @param error - Axios error object from failed request
 * @param context - Context identifier for logging
 * @returns Parsed user-friendly error message
 */
export function handleError(error: any, context: string): string {
    logError(error, context)
    return parseApiError(error)
}

/**
 * Comprehensive error handler that parses, logs, displays toast notification,
 * and optionally highlights validation error fields.
 * @param error - Axios error object from failed request
 * @param context - Context identifier for logging
 * @param setFieldError - Optional callback to highlight field with validation error
 * @returns Parsed user-friendly error message
 */
export function catchApiError(
    error: any,
    context: string,
    setFieldError?: (field: string) => void
): string {
    const message = handleError(error, context)
    // Show error toast notification to user
    const { show } = useToast()
    show(message, 'error')
    // Highlight first validation error field if available and callback provided
    if (setFieldError && error?.response?.data?.errors) {
        const firstField = Object.keys(error.response.data.errors)[0]
        if (firstField) setFieldError(firstField)
    }
    return message
}