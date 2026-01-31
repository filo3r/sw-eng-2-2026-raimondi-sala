/**
 * Time formatting and normalization utilities.
 */

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