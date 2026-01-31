/**
 * Validation helper functions for form validation.
 * Returns string (error message) or null (validation passed).
 */

import {
    EMAIL_PATTERN,
    EMAIL_MAX_LENGTH,
    USER_NAME_MAX_LENGTH,
    USER_SURNAME_MAX_LENGTH,
    USERNAME_MAX_LENGTH,
    PASSWORD_MIN_LENGTH,
    PASSWORD_MAX_LENGTH,
    ADDRESSES_MIN_COUNT,
    TRIP_DURATION_MIN_MINUTES,
    LATITUDE_MIN,
    LATITUDE_MAX,
    LONGITUDE_MIN,
    LONGITUDE_MAX,
    MAX_SPEED_PATTERN
} from '@/constants/validation'

// ============================================================================
// HELPER FUNCTIONS (return string | null)
// ============================================================================

/**
 * Validates required field.
 */
export function validateRequired(value: string, fieldName: string): string | null {
    if (!value || !value.trim()) {
        return `${fieldName} is required`
    }
    return null
}

/**
 * Validates name field.
 */
export function validateName(value: string): string | null {
    if (!value.trim()) {
        return 'Name is required'
    }
    if (value.length > USER_NAME_MAX_LENGTH) {
        return `Name must not exceed ${USER_NAME_MAX_LENGTH} characters`
    }
    return null
}

/**
 * Validates surname field.
 */
export function validateSurname(value: string): string | null {
    if (!value.trim()) {
        return 'Surname is required'
    }
    if (value.length > USER_SURNAME_MAX_LENGTH) {
        return `Surname must not exceed ${USER_SURNAME_MAX_LENGTH} characters`
    }
    return null
}

/**
 * Validates username field.
 */
export function validateUsername(value: string): string | null {
    if (!value.trim()) {
        return 'Username is required'
    }
    if (value.length > USERNAME_MAX_LENGTH) {
        return `Username must not exceed ${USERNAME_MAX_LENGTH} characters`
    }
    return null
}

/**
 * Validates email format and length.
 */
export function validateEmail(value: string): string | null {
    if (!value.trim()) {
        return 'Email is required'
    }
    if (value.length > EMAIL_MAX_LENGTH) {
        return `Email must not exceed ${EMAIL_MAX_LENGTH} characters`
    }
    if (!EMAIL_PATTERN.test(value)) {
        return 'Please enter a valid email address'
    }
    return null
}

/**
 * Validates password minimum and maximum length.
 */
export function validatePassword(value: string): string | null {
    if (!value) {
        return 'Password is required'
    }
    if (value.length < PASSWORD_MIN_LENGTH) {
        return `Password must be at least ${PASSWORD_MIN_LENGTH} characters`
    }
    if (value.length > PASSWORD_MAX_LENGTH) {
        return `Password must not exceed ${PASSWORD_MAX_LENGTH} characters`
    }
    return null
}

/**
 * Validates optional password (for profile update).
 */
export function validateOptionalPassword(value: string): string | null {
    if (!value) return null
    if (value.length < PASSWORD_MIN_LENGTH) {
        return `Password must be at least ${PASSWORD_MIN_LENGTH} characters`
    }
    if (value.length > PASSWORD_MAX_LENGTH) {
        return `Password must not exceed ${PASSWORD_MAX_LENGTH} characters`
    }
    return null
}

/**
 * Validates optional description field.
 */
export function validateOptionalDescription(value: string, maxLength: number): string | null {
    if (!value) return null
    if (value.length > maxLength) {
        return `Description must not exceed ${maxLength} characters`
    }
    return null
}

/**
 * Validates date range (from/to).
 */
export function validateDateRange(
    fromDate: Date | null,
    toDate: Date | null,
    fieldName: string
): string | null {
    if (!fromDate || !toDate) return null
    if (toDate < fromDate) {
        return `${fieldName} "to" must be after "from"`
    }
    return null
}

/**
 * Validates minimum count for addresses.
 */
export function validateAddresses(addresses: string[]): string | null {
    const validAddresses = addresses.filter(addr => addr.trim() !== '')
    if (validAddresses.length < ADDRESSES_MIN_COUNT) {
        return `At least ${ADDRESSES_MIN_COUNT} addresses are required`
    }
    return null
}

/**
 * Validates end time is after start time.
 */
export function validateEndAfterStart(
    startTime: Date | null,
    endTime: Date | null
): string | null {
    if (!startTime || !endTime) {
        return 'Start and end times are required'
    }
    if (endTime <= startTime) {
        return 'End time must be after start time'
    }
    return null
}

/**
 * Validates trip duration meets minimum requirement.
 */
export function validateTripDuration(
    startTime: Date | null,
    endTime: Date | null
): string | null {
    if (!startTime || !endTime) {
        return 'Start and end times are required'
    }

    const durationMinutes = (endTime.getTime() - startTime.getTime()) / (1000 * 60)

    if (durationMinutes < TRIP_DURATION_MIN_MINUTES) {
        return `Trip must be at least ${TRIP_DURATION_MIN_MINUTES} minute${TRIP_DURATION_MIN_MINUTES > 1 ? 's' : ''} long`
    }
    return null
}

/**
 * Validates optional max speed.
 */
export function validateOptionalMaxSpeed(value: number | string | ''): string | null {
    if (value === '' || value === null || value === undefined) {
        return null
    }

    const speedStr = String(value)
    if (!MAX_SPEED_PATTERN.test(speedStr)) {
        return 'Max speed must be a positive number with at most 3 integer digits and 2 decimal digits'
    }

    const speedNum = Number(value)
    if (isNaN(speedNum) || speedNum <= 0) {
        return 'Max speed must be greater than 0'
    }

    return null
}

/**
 * Validates coordinate object, throws if invalid.
 * Used in services for sanity checking programmatic values.
 */
export function validateCoordinates(coords: { latitude: number; longitude: number }): void {
    if (coords.latitude < LATITUDE_MIN || coords.latitude > LATITUDE_MAX) {
        throw new Error(`Invalid latitude: ${coords.latitude}. Must be between ${LATITUDE_MIN} and ${LATITUDE_MAX}`)
    }
    if (coords.longitude < LONGITUDE_MIN || coords.longitude > LONGITUDE_MAX) {
        throw new Error(`Invalid longitude: ${coords.longitude}. Must be between ${LONGITUDE_MIN} and ${LONGITUDE_MAX}`)
    }
}

// ============================================================================
// COMPACT VALIDATION FUNCTION
// ============================================================================

/**
 * Validates and shows error in one call.
 * Returns true if validation passed, false otherwise.
 */
export function validateAndShow(
    error: string | null,
    field: string,
    setError: (field: string) => void,
    show: (message: string, type: 'error' | 'success') => void
): boolean {
    if (error) {
        setError(field)
        show(error, 'error')
        return false
    }
    return true
}