/**
 * Formatting utilities for numbers and measurements.
 */

/**
 * Formats distance value with unit label.
 * @param km - Distance in kilometers
 * @param decimals - Number of decimal places (default: 3, matches DB precision)
 * @returns Formatted distance string with "km" suffix
 */
export function formatDistance(km: number, decimals: number = 3): string {
    return `${km.toFixed(decimals)} km`
}

/**
 * Formats speed value with unit label.
 * @param kmh - Speed in kilometers per hour
 * @param decimals - Number of decimal places (default: 2, matches DB precision)
 * @returns Formatted speed string with "km/h" suffix
 */
export function formatSpeed(kmh: number, decimals: number = 2): string {
    return `${kmh.toFixed(decimals)} km/h`
}

/**
 * Formats duration in minutes to human-readable hours and minutes format.
 * Shows only minutes if duration is less than 1 hour.
 * @param minutes - Duration in minutes
 * @returns Formatted duration string (e.g., "1h 23m" or "45m")
 */
export function formatDuration(minutes: number): string {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    // Show only minutes if less than 1 hour
    if (hours === 0) {
        return `${mins}m`
    }
    return `${hours}h ${mins}m`
}

/**
 * Formats bike path score to fixed decimal precision.
 * @param score - Score value (0.0 to 5.0)
 * @param decimals - Number of decimal places (default: 2, matches DB precision)
 * @returns Formatted score string without units
 */
export function formatScore(score: number, decimals: number = 2): string {
    return score.toFixed(decimals)
}

/**
 * Formats temperature value with Celsius unit symbol.
 * @param celsius - Temperature in degrees Celsius
 * @param decimals - Number of decimal places (default: 1)
 * @returns Formatted temperature string with "°C" suffix
 */
export function formatTemperature(celsius: number, decimals: number = 1): string {
    return `${celsius.toFixed(decimals)}°C`
}

/**
 * Formats humidity value as percentage.
 * @param humidity - Humidity percentage (0-100)
 * @returns Formatted humidity string with "%" suffix
 */
export function formatHumidity(humidity: number): string {
    return `${humidity}%`
}

/**
 * Formats wind speed value with unit label.
 * @param kmh - Wind speed in kilometers per hour
 * @param decimals - Number of decimal places (default: 1)
 * @returns Formatted wind speed string with "km/h" suffix
 */
export function formatWindSpeed(kmh: number, decimals: number = 1): string {
    return `${kmh.toFixed(decimals)} km/h`
}