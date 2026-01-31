/**
 * Date and time formatting utilities.
 */

/**
 * Locale argument type for Intl formatting APIs.
 * Can be a single locale string, array of locales, or undefined for system default.
 */
type LocalesArg = string | string[] | undefined

/**
 * Formats ISO date string to readable localized date (year, month, day).
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted date string (e.g., "Dec 28, 2024" for en-US or "28 dic 2024" for it-IT)
 */
export function formatDate(isoString: string, locale?: LocalesArg): string {
    const date = new Date(isoString)
    return date.toLocaleDateString(locale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
    })
}

/**
 * Formats ISO date string to readable localized date and time with seconds.
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted date and time string with seconds
 */
export function formatDateTime(isoString: string, locale?: LocalesArg): string {
    const date = new Date(isoString)
    return date.toLocaleString(locale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        second: '2-digit',
    })
}

/**
 * Formats ISO date string to time only with seconds.
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted time string with seconds
 */
export function formatTime(isoString: string, locale?: LocalesArg): string {
    const date = new Date(isoString)
    return date.toLocaleTimeString(locale, {
        hour: 'numeric',
        minute: '2-digit',
        second: '2-digit',
    })
}

/**
 * Formats date range from two ISO strings with smart formatting.
 * Shows date once if same day, otherwise shows full date-time for both.
 * @param startIso - Start date ISO string
 * @param endIso - End date ISO string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted date range string
 */
export function formatDateRange(startIso: string, endIso: string, locale?: LocalesArg): string {
    const start = new Date(startIso)
    const end = new Date(endIso)
    // Same day: show date once with time range
    if (start.toDateString() === end.toDateString()) {
        return `${formatDate(startIso, locale)}, ${formatTime(startIso, locale)} - ${formatTime(endIso, locale)}`
    }
    // Different days: show full date-time for both
    return `${formatDateTime(startIso, locale)} - ${formatDateTime(endIso, locale)}`
}

/**
 * Normalizes time string to HH:mm:ss format.
 * Some browsers return "HH:mm" instead of "HH:mm:ss" for time inputs.
 * @param timeString - Time string in HH:mm or HH:mm:ss format
 * @returns Normalized time string in HH:mm:ss format
 */
export function normalizeTime(timeString: string): string {
    if (!timeString) return ''
    return timeString.length === 5 ? `${timeString}:00` : timeString
}