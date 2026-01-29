/**
 * API error parsing utilities with integrated logging.
 * Handles all error response formats from GlobalExceptionHandler.
 */
import { useToast } from '@/composables/useToast'

/**
 * Error response structure from backend.
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
 * Log levels for error tracking.
 */
type LogLevel = 'error' | 'warn' | 'info'

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
 * Internal logging function.
 */
function log(level: LogLevel, context: string, message: string, additionalData?: any) {
    const entry: LogEntry = {
        timestamp: new Date().toISOString(),
        level,
        context,
        message,
        ...additionalData
    }

    console[level](`[${entry.level.toUpperCase()}] [${entry.context}]`, entry.message, entry)
}

/**
 * Parses API error responses into user-friendly messages.
 * @param error - Axios error object
 * @returns User-friendly error message
 */
export function parseApiError(error: any): string {
    // Network error (no response from server)
    if (!error?.response) {
        return 'Network error. Please check your connection.'
    }

    const data: ApiErrorResponse = error.response.data

    // Validation errors - only messages, no field names
    if (data?.errors && Object.keys(data.errors).length > 0) {
        return Object.values(data.errors).join('\n')
    }

    // Standard error - single message
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
 * Logs API error with context and request ID.
 * @param error - Axios error object
 * @param context - Context identifier (e.g., 'BikePathCreate.submit')
 */
export function logError(error: any, context: string) {
    const requestId = error?.response?.data?.requestId
    const status = error?.response?.status

    log('error', context, parseApiError(error), {
        requestId,
        status,
        error: error?.response?.data || error?.message
    })
}

/**
 * Parses and logs error, returns message.
 * @param error - Axios error object
 * @param context - Context identifier
 * @returns Parsed error message
 */
export function handleError(error: any, context: string): string {
    logError(error, context)
    return parseApiError(error)
}

/**
 * Parses, logs error, and shows toast notification.
 * Convenience function for common error handling pattern.
 * @param error - Axios error object
 * @param context - Context identifier
 * @returns Parsed error message
 */
export function catchApiError(error: any, context: string): string {
    const message = handleError(error, context)
    const { show } = useToast()
    show(message, 'error')
    return message
}