/**
 * Structured logging utilities for frontend error tracking and debugging.
 * Correlates with backend logs via requestId.
 */

import { getErrorStatus, getErrorRequestId, parseApiError } from './error'

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
 * Internal logging function that formats and outputs log entries.
 * @param level - Log severity level
 * @param context - Context identifier (e.g., 'Profile.fetchUserData')
 * @param message - Human-readable message
 * @param additionalData - Optional extra data (requestId, status, etc.)
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
 * Logs API errors with requestId for backend correlation.
 * @param error - Axios error object
 * @param context - Context identifier
 */
export function logError(error: any, context: string) {
    const requestId = getErrorRequestId(error)
    const status = getErrorStatus(error)

    log('error', context, parseApiError(error), {
        requestId,
        status,
        error: error.response?.data || error.message
    })
}

/**
 * Logs informational messages.
 * @param context - Context identifier
 * @param message - Log message
 * @param data - Optional additional data
 */
export function logInfo(context: string, message: string, data?: any) {
    log('info', context, message, data)
}

/**
 * Logs warning messages.
 * @param context - Context identifier
 * @param message - Log message
 * @param data - Optional additional data
 */
export function logWarn(context: string, message: string, data?: any) {
    log('warn', context, message, data)
}