/**
 * Date and time formatting utilities.
 */

/**
 * Formats ISO date string to readable date.
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: 'en-US')
 * @returns Formatted date string
 * @example formatDate("2024-12-28T14:30:00Z") // "Dec 28, 2024"
 */
export function formatDate(isoString: string, locale: string = 'en-US'): string {
    const date = new Date(isoString)
    return date.toLocaleDateString(locale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    })
}

/**
 * Formats ISO date string to readable date and time.
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: 'en-US')
 * @returns Formatted date and time string
 * @example formatDateTime("2024-12-28T14:30:00Z") // "Dec 28, 2024, 2:30 PM"
 */
export function formatDateTime(isoString: string, locale: string = 'en-US'): string {
    const date = new Date(isoString)
    return date.toLocaleString(locale, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
    })
}

/**
 * Formats ISO date string to time only.
 * @param isoString - ISO 8601 date string
 * @param locale - Locale for formatting (default: 'en-US')
 * @returns Formatted time string
 * @example formatTime("2024-12-28T14:30:00Z") // "2:30 PM"
 */
export function formatTime(isoString: string, locale: string = 'en-US'): string {
    const date = new Date(isoString)
    return date.toLocaleTimeString(locale, {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
    })
}

/**
 * Formats ISO date string to relative time.
 * @param isoString - ISO 8601 date string
 * @returns Relative time string
 * @example formatRelativeTime("2024-12-28T12:30:00Z") // "2 hours ago"
 */
export function formatRelativeTime(isoString: string): string {
    const date = new Date(isoString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffSec = Math.floor(diffMs / 1000)
    const diffMin = Math.floor(diffSec / 60)
    const diffHour = Math.floor(diffMin / 60)
    const diffDay = Math.floor(diffHour / 24)
    const diffWeek = Math.floor(diffDay / 7)
    const diffMonth = Math.floor(diffDay / 30)
    const diffYear = Math.floor(diffDay / 365)

    if (diffSec < 60) {
        return 'just now'
    } else if (diffMin < 60) {
        return `${diffMin} ${diffMin === 1 ? 'minute' : 'minutes'} ago`
    } else if (diffHour < 24) {
        return `${diffHour} ${diffHour === 1 ? 'hour' : 'hours'} ago`
    } else if (diffDay < 7) {
        return `${diffDay} ${diffDay === 1 ? 'day' : 'days'} ago`
    } else if (diffWeek < 4) {
        return `${diffWeek} ${diffWeek === 1 ? 'week' : 'weeks'} ago`
    } else if (diffMonth < 12) {
        return `${diffMonth} ${diffMonth === 1 ? 'month' : 'months'} ago`
    } else {
        return `${diffYear} ${diffYear === 1 ? 'year' : 'years'} ago`
    }
}

/**
 * Formats date range from two ISO strings.
 * @param startIso - Start date ISO string
 * @param endIso - End date ISO string
 * @param locale - Locale for formatting (default: 'en-US')
 * @returns Formatted date range string
 * @example formatDateRange("2024-12-28T10:00:00Z", "2024-12-28T14:30:00Z") // "Dec 28, 2024, 10:00 AM - 2:30 PM"
 */
export function formatDateRange(startIso: string, endIso: string, locale: string = 'en-US'): string {
    const start = new Date(startIso)
    const end = new Date(endIso)
    // Same day
    if (start.toDateString() === end.toDateString()) {
        return `${formatDate(startIso, locale)}, ${formatTime(startIso, locale)} - ${formatTime(endIso, locale)}`
    }
    // Different days
    return `${formatDateTime(startIso, locale)} - ${formatDateTime(endIso, locale)}`
}