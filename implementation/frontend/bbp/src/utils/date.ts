/**
 * Date and time formatting utilities.
 */

type LocalesArg = string | string[] | undefined

/**
 * Formats ISO date string to readable date.
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted date string
 * @example formatDate("2024-12-28T14:30:00Z") // e.g. "Dec 28, 2024" (en-US) or "28 dic 2024" (it-IT)
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
 * Formats ISO date string to readable date and time (with seconds).
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted date and time string
 * @example formatDateTime("2024-12-28T14:30:00Z") // e.g. "Dec 28, 2024, 2:30:00 PM"
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
 * Formats ISO date string to time only (with seconds).
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted time string
 * @example formatTime("2024-12-28T14:30:00Z") // e.g. "2:30:00 PM"
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
 * Formats date range from two ISO strings.
 * @param startIso - Start date ISO string
 * @param endIso - End date ISO string
 * @param locale - Locale for formatting (default: browser/runtime default)
 * @returns Formatted date range string
 * @example formatDateRange("2024-12-28T10:00:00Z", "2024-12-28T14:30:15Z") // "Dec 28, 2024, 10:00:00 AM - 2:30:15 PM"
 */
export function formatDateRange(startIso: string, endIso: string, locale?: LocalesArg): string {
    const start = new Date(startIso)
    const end = new Date(endIso)
    // Same day (in the client's time zone)
    if (start.toDateString() === end.toDateString()) {
        return `${formatDate(startIso, locale)}, ${formatTime(startIso, locale)} - ${formatTime(endIso, locale)}`
    }
    // Different days
    return `${formatDateTime(startIso, locale)} - ${formatDateTime(endIso, locale)}`
}