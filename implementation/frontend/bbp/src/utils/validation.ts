/*
 * Validation utility functions for the Bike Path application.
 * Uses constants from @/constants/validation for validation rules.
 */

import {
    EMAIL_PATTERN,
    LATITUDE_MIN,
    LATITUDE_MAX,
    LONGITUDE_MIN,
    LONGITUDE_MAX,
    SCORE_MIN,
    SCORE_MAX,
    TEMPERATURE_MIN,
    TEMPERATURE_MAX,
    HUMIDITY_MIN,
    HUMIDITY_MAX,
    WIND_SPEED_MIN,
    WIND_SPEED_MAX,
    MAX_SPEED_PATTERN,
    TRIP_DURATION_MIN_MINUTES
} from '@/constants/validation'

/**
 * Validates email format.
 * @param email - Email address to validate
 * @returns True if email is valid, false otherwise
 */
export function isValidEmail(email: string): boolean {
    return EMAIL_PATTERN.test(email)
}

/**
 * Validates latitude value.
 * @param latitude - Latitude value to validate
 * @returns True if latitude is within valid range
 */
export function isValidLatitude(latitude: number): boolean {
    return latitude >= LATITUDE_MIN && latitude <= LATITUDE_MAX
}

/**
 * Validates longitude value.
 * @param longitude - Longitude value to validate
 * @returns True if longitude is within valid range
 */
export function isValidLongitude(longitude: number): boolean {
    return longitude >= LONGITUDE_MIN && longitude <= LONGITUDE_MAX
}

/**
 * Validates bike path score.
 * @param score - Score value to validate
 * @returns True if score is within valid range
 */
export function isValidScore(score: number): boolean {
    return score >= SCORE_MIN && score <= SCORE_MAX
}

/**
 * Validates temperature value.
 * @param temperature - Temperature in Celsius to validate
 * @returns True if temperature is within valid range
 */
export function isValidTemperature(temperature: number): boolean {
    return temperature >= TEMPERATURE_MIN && temperature <= TEMPERATURE_MAX
}

/**
 * Validates humidity percentage.
 * @param humidity - Humidity percentage to validate
 * @returns True if humidity is within valid range
 */
export function isValidHumidity(humidity: number): boolean {
    return humidity >= HUMIDITY_MIN && humidity <= HUMIDITY_MAX
}

/**
 * Validates wind speed.
 * @param windSpeed - Wind speed in km/h to validate
 * @returns True if wind speed is within valid range
 */
export function isValidWindSpeed(windSpeed: number): boolean {
    return windSpeed >= WIND_SPEED_MIN && windSpeed <= WIND_SPEED_MAX
}

/**
 * Validates max speed format and value.
 * @param maxSpeed - Max speed value to validate
 * @returns True if max speed is valid
 */
export function isValidMaxSpeed(maxSpeed: number | string): boolean {
    const speedStr = String(maxSpeed)
    if (!MAX_SPEED_PATTERN.test(speedStr)) {
        return false
    }
    const speedNum = Number(maxSpeed)
    return !isNaN(speedNum) && speedNum > 0
}

/**
 * Validates that end time is after start time.
 * @param startTime - Start timestamp
 * @param endTime - End timestamp
 * @returns True if end time is after start time
 */
export function isEndTimeAfterStartTime(startTime: Date | string, endTime: Date | string): boolean {
    const start = new Date(startTime)
    const end = new Date(endTime)
    return end > start
}

/**
 * Validates trip duration is at least the minimum required.
 * @param startTime - Start timestamp
 * @param endTime - End timestamp
 * @returns True if duration is at least the minimum
 */
export function isValidTripDuration(startTime: Date | string, endTime: Date | string): boolean {
    const start = new Date(startTime)
    const end = new Date(endTime)
    const durationMinutes = (end.getTime() - start.getTime()) / (1000 * 60)
    return durationMinutes >= TRIP_DURATION_MIN_MINUTES
}

/**
 * Validates that a string doesn't exceed maximum length.
 * @param value - String to validate
 * @param maxLength - Maximum allowed length
 * @returns True if string length is valid
 */
export function isValidLength(value: string, maxLength: number): boolean {
    return value.length <= maxLength
}

/**
 * Validates that a string meets minimum length requirement.
 * @param value - String to validate
 * @param minLength - Minimum required length
 * @returns True if string length is valid
 */
export function isValidMinLength(value: string, minLength: number): boolean {
    return value.length >= minLength
}

/**
 * Validates that a value is not blank (null, undefined, or empty string).
 * @param value - Value to validate
 * @returns True if value is not blank
 */
export function isNotBlank(value: string | null | undefined): boolean {
    return value !== null && value !== undefined && value.trim().length > 0
}

/**
 * Validates that a number is positive.
 * @param value - Number to validate
 * @returns True if number is greater than 0
 */
export function isPositive(value: number): boolean {
    return value > 0
}

/**
 * Validates date range for search filters.
 * @param startDate - Start date
 * @param endDate - End date
 * @returns True if end date is after or equal to start date, or either is null
 */
export function isValidDateRange(
    startDate: Date | string | null | undefined,
    endDate: Date | string | null | undefined
): boolean {
    if (!startDate || !endDate) {
        return true
    }
    const start = new Date(startDate)
    const end = new Date(endDate)
    return end >= start
}