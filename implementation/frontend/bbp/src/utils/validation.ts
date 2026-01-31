/**
 * Form validation helpers.
 * Each validator returns an error message string, or `null` when valid.
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

/**
 * Validates that a required string field is non-empty (after trimming).
 * @param value - Raw input value.
 * @param fieldName - Field label used to build the error message.
 * @returns Error message, or `null` if valid.
 */
export function validateRequired(value: string, fieldName: string): string | null {
    if (!value || !value.trim()) {
        return `${fieldName} is required`
    }
    return null
}

/**
 * Validates a "name" field (required, max length).
 * @param value - Name input.
 * @returns Error message, or `null` if valid.
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
 * Validates a "surname" field (required, max length).
 * @param value - Surname input.
 * @returns Error message, or `null` if valid.
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
 * Validates a "username" field (required, max length).
 * @param value - Username input.
 * @returns Error message, or `null` if valid.
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
 * Validates an email field (required, max length, pattern).
 * @param value - Email input.
 * @returns Error message, or `null` if valid.
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
 * Validates a password field (required, min/max length).
 * @param value - Password input.
 * @returns Error message, or `null` if valid.
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
 * Validates an optional password (empty is allowed; otherwise min/max length apply).
 * @param value - Password input (may be empty).
 * @returns Error message, or `null` if valid.
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
 * Validates an optional description (empty is allowed; otherwise max length applies).
 * @param value - Description input (may be empty).
 * @param maxLength - Allowed maximum length.
 * @returns Error message, or `null` if valid.
 */
export function validateOptionalDescription(value: string, maxLength: number): string | null {
    if (!value) return null
    if (value.length > maxLength) {
        return `Description must not exceed ${maxLength} characters`
    }
    return null
}

/**
 * Validates a date range (`toDate` must be >= `fromDate` when both are provided).
 * @param fromDate - Range start.
 * @param toDate - Range end.
 * @param fieldName - Field label used to build the error message.
 * @returns Error message, or `null` if valid / incomplete.
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
 * Validates a minimum count of non-empty addresses.
 * @param addresses - Raw address strings.
 * @returns Error message, or `null` if valid.
 */
export function validateAddresses(addresses: string[]): string | null {
    const validAddresses = addresses.filter(addr => addr.trim() !== '')
    if (validAddresses.length < ADDRESSES_MIN_COUNT) {
        return `At least ${ADDRESSES_MIN_COUNT} addresses are required`
    }
    return null
}

/**
 * Validates that end time is after start time (both required).
 * @param startTime - Start timestamp.
 * @param endTime - End timestamp.
 * @returns Error message, or `null` if valid.
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
 * Validates that trip duration meets the configured minimum (both times required).
 * @param startTime - Trip start time.
 * @param endTime - Trip end time.
 * @returns Error message, or `null` if valid.
 */
export function validateTripDuration(
    startTime: Date | null,
    endTime: Date | null
): string | null {
    if (!startTime || !endTime) {
        return 'Start and end times are required'
    }
    // Duration in minutes
    const durationMinutes = (endTime.getTime() - startTime.getTime()) / (1000 * 60)
    if (durationMinutes < TRIP_DURATION_MIN_MINUTES) {
        return `Trip must be at least ${TRIP_DURATION_MIN_MINUTES} minute${TRIP_DURATION_MIN_MINUTES > 1 ? 's' : ''} long`
    }
    return null
}

/**
 * Validates an optional max speed value.
 * Empty values are considered valid; otherwise it must match the pattern and be > 0.
 * @param value - Speed input (number, string, or empty).
 * @returns Error message, or `null` if valid.
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
 * Validates a coordinate object and throws if invalid.
 * @param coords - Coordinates to validate.
 * @throws Error if latitude/longitude are out of bounds.
 */
export function validateCoordinates(coords: { latitude: number; longitude: number }): void {
    if (coords.latitude < LATITUDE_MIN || coords.latitude > LATITUDE_MAX) {
        throw new Error(`Invalid latitude: ${coords.latitude}. Must be between ${LATITUDE_MIN} and ${LATITUDE_MAX}`)
    }
    if (coords.longitude < LONGITUDE_MIN || coords.longitude > LONGITUDE_MAX) {
        throw new Error(`Invalid longitude: ${coords.longitude}. Must be between ${LONGITUDE_MIN} and ${LONGITUDE_MAX}`)
    }
}

/**
 * Helper to apply a validation result and show an error.
 * @param error - Validation result (error message or `null`).
 * @param field - Field identifier to pass to `setError`.
 * @param setError - Marks a field as invalid (e.g., for UI highlighting).
 * @param show - Displays a toast/notification.
 * @returns `true` if valid, `false` otherwise.
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