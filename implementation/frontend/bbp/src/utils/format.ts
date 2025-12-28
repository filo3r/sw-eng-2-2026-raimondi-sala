/*
 * Formatting utilities for numbers and measurements.
 */

/**
 * Formats distance in kilometers.
 * @param km - Distance in kilometers
 * @param decimals - Number of decimal places (default: 3, matches DB precision)
 * @returns Formatted distance string
 * @example formatDistance(12.567) // "12.567 km"
 */
export function formatDistance(km: number, decimals: number = 3): string {
    return `${km.toFixed(decimals)} km`
}

/**
 * Formats speed in km/h.
 * @param kmh - Speed in kilometers per hour
 * @param decimals - Number of decimal places (default: 2, matches DB precision)
 * @returns Formatted speed string
 * @example formatSpeed(23.456) // "23.46 km/h"
 */
export function formatSpeed(kmh: number, decimals: number = 2): string {
    return `${kmh.toFixed(decimals)} km/h`
}

/**
 * Formats duration in minutes to hours and minutes.
 * @param minutes - Duration in minutes
 * @returns Formatted duration string
 * @example formatDuration(83) // "1h 23m"
 * @example formatDuration(45) // "45m"
 */
export function formatDuration(minutes: number): string {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60

    if (hours === 0) {
        return `${mins}m`
    }

    return `${hours}h ${mins}m`
}

/**
 * Formats bike path score.
 * @param score - Score value (0.0 to 5.0)
 * @param decimals - Number of decimal places (default: 2, matches DB precision)
 * @returns Formatted score string
 * @example formatScore(4.567) // "4.57"
 */
export function formatScore(score: number, decimals: number = 2): string {
    return score.toFixed(decimals)
}

/**
 * Formats temperature in Celsius.
 * @param celsius - Temperature in degrees Celsius
 * @param decimals - Number of decimal places (default: 1)
 * @returns Formatted temperature string
 * @example formatTemperature(12.56) // "12.6°C"
 */
export function formatTemperature(celsius: number, decimals: number = 1): string {
    return `${celsius.toFixed(decimals)}°C`
}

/**
 * Formats humidity percentage.
 * @param humidity - Humidity percentage (0-100)
 * @returns Formatted humidity string
 * @example formatHumidity(75) // "75%"
 */
export function formatHumidity(humidity: number): string {
    return `${humidity}%`
}

/**
 * Formats wind speed in km/h.
 * @param kmh - Wind speed in kilometers per hour
 * @param decimals - Number of decimal places (default: 1)
 * @returns Formatted wind speed string
 * @example formatWindSpeed(12.56) // "12.6 km/h"
 */
export function formatWindSpeed(kmh: number, decimals: number = 1): string {
    return `${kmh.toFixed(decimals)} km/h`
}