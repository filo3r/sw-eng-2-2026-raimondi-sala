/*
 * Validation constants and rules for the Bike Path application.
 * Based on backend validation constraints from DTOs and entities.
 */

// ============================================================================
// STRING LENGTH CONSTRAINTS
// ============================================================================

/**
 * Maximum length for user name.
 */
export const USER_NAME_MAX_LENGTH = 50;

/**
 * Maximum length for user surname.
 */
export const USER_SURNAME_MAX_LENGTH = 50;

/**
 * Maximum length for username.
 */
export const USERNAME_MAX_LENGTH = 50;

/**
 * Maximum length for email address.
 */
export const EMAIL_MAX_LENGTH = 150;

/**
 * Minimum length for password.
 */
export const PASSWORD_MIN_LENGTH = 8;

/**
 * Maximum length for password (BCrypt hash).
 */
export const PASSWORD_MAX_LENGTH = 60;

/**
 * Maximum length for address or location description.
 */
export const ADDRESS_MAX_LENGTH = 256;

/**
 * Maximum length for description or notes.
 */
export const DESCRIPTION_MAX_LENGTH = 500;

/**
 * Minimum number of addresses required for bike path or trip.
 */
export const ADDRESSES_MIN_COUNT = 2;

// ============================================================================
// NUMERIC RANGE CONSTRAINTS
// ============================================================================

/**
 * Minimum latitude value in decimal degrees.
 */
export const LATITUDE_MIN = -90.0;

/**
 * Maximum latitude value in decimal degrees.
 */
export const LATITUDE_MAX = 90.0;

/**
 * Minimum longitude value in decimal degrees.
 */
export const LONGITUDE_MIN = -180.0;

/**
 * Maximum longitude value in decimal degrees.
 */
export const LONGITUDE_MAX = 180.0;

/**
 * Minimum bike path score.
 */
export const SCORE_MIN = 0.0;

/**
 * Maximum bike path score.
 */
export const SCORE_MAX = 5.0;

/**
 * Minimum temperature in degrees Celsius.
 */
export const TEMPERATURE_MIN = -99.9;

/**
 * Maximum temperature in degrees Celsius.
 */
export const TEMPERATURE_MAX = 99.9;

/**
 * Minimum humidity percentage.
 */
export const HUMIDITY_MIN = 0;

/**
 * Maximum humidity percentage.
 */
export const HUMIDITY_MAX = 100;

/**
 * Minimum wind speed in km/h.
 */
export const WIND_SPEED_MIN = 0.0;

/**
 * Maximum wind speed in km/h.
 */
export const WIND_SPEED_MAX = 999.9;

/**
 * Minimum trip duration in minutes.
 */
export const TRIP_DURATION_MIN_MINUTES = 1;

/**
 * Maximum number of integer digits for speed values.
 * Example: 123.45 has 3 integer digits
 */
export const MAX_SPEED_INTEGER_DIGITS = 3;

/**
 * Maximum number of fraction digits for speed values.
 * Example: 123.45 has 2 fraction digits
 */
export const MAX_SPEED_FRACTION_DIGITS = 2;

/**
 * Minimum sequential position for points.
 */
export const SEQUENTIAL_POSITION_MIN = 1;

// ============================================================================
// REGEX PATTERNS
// ============================================================================

/**
 * Email validation pattern (RFC 5322 simplified).
 */
export const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

/**
 * Decimal number pattern for max speed validation (max 3 integer digits, 2 fraction digits).
 */
export const MAX_SPEED_PATTERN = /^\d{1,3}(\.\d{1,2})?$/;

// ============================================================================
// VALIDATION ERROR MESSAGES
// ============================================================================

/**
 * Validation error messages for user-related fields.
 */
export const USER_VALIDATION_MESSAGES = {
    NAME_REQUIRED: 'Name is required',
    NAME_MAX_LENGTH: `Name must not exceed ${USER_NAME_MAX_LENGTH} characters`,
    SURNAME_REQUIRED: 'Surname is required',
    SURNAME_MAX_LENGTH: `Surname must not exceed ${USER_SURNAME_MAX_LENGTH} characters`,
    USERNAME_REQUIRED: 'Username is required',
    USERNAME_MAX_LENGTH: `Username must not exceed ${USERNAME_MAX_LENGTH} characters`,
    EMAIL_REQUIRED: 'Email is required',
    EMAIL_INVALID: 'Email must be valid',
    EMAIL_MAX_LENGTH: `Email must not exceed ${EMAIL_MAX_LENGTH} characters`,
    PASSWORD_REQUIRED: 'Password is required',
    PASSWORD_MIN_LENGTH: `Password must be at least ${PASSWORD_MIN_LENGTH} characters`,
} as const;

/**
 * Validation error messages for bike path-related fields.
 */
export const BIKE_PATH_VALIDATION_MESSAGES = {
    ORIGIN_REQUIRED: 'Origin address is required',
    ORIGIN_MAX_LENGTH: `Origin must not exceed ${ADDRESS_MAX_LENGTH} characters`,
    DESTINATION_REQUIRED: 'Destination address is required',
    DESTINATION_MAX_LENGTH: `Destination must not exceed ${ADDRESS_MAX_LENGTH} characters`,
    ADDRESSES_REQUIRED: 'Addresses list is required',
    ADDRESSES_MIN_COUNT: `At least ${ADDRESSES_MIN_COUNT} addresses are required (origin and destination)`,
    ADDRESS_BLANK: 'Address cannot be blank',
    ADDRESS_MAX_LENGTH: `Address must not exceed ${ADDRESS_MAX_LENGTH} characters`,
    DESCRIPTION_MAX_LENGTH: `Description must not exceed ${DESCRIPTION_MAX_LENGTH} characters`,
    STATUS_REQUIRED: 'Status is required',
    PUBLISHED_REQUIRED: 'Published flag is required',
    OBSTACLES_REQUIRED: 'Obstacles list is required (use empty list if no obstacles)',
    ORIGIN_RADIUS_POSITIVE: 'Origin radius must be positive',
    DESTINATION_RADIUS_POSITIVE: 'Destination radius must be positive',
} as const;

/**
 * Validation error messages for obstacle-related fields.
 */
export const OBSTACLE_VALIDATION_MESSAGES = {
    ADDRESS_REQUIRED: 'Obstacle address is required',
    ADDRESS_MAX_LENGTH: `Obstacle address must not exceed ${ADDRESS_MAX_LENGTH} characters`,
    TYPE_REQUIRED: 'Obstacle type is required',
    SEVERITY_REQUIRED: 'Obstacle severity is required',
    ID_REQUIRED: 'Obstacle ID is required',
} as const;

/**
 * Validation error messages for trip-related fields.
 */
export const TRIP_VALIDATION_MESSAGES = {
    ADDRESSES_REQUIRED: 'Addresses list is required',
    ADDRESSES_MIN_COUNT: `At least ${ADDRESSES_MIN_COUNT} addresses are required (origin and destination)`,
    ADDRESS_BLANK: 'Address cannot be blank',
    ADDRESS_MAX_LENGTH: `Address must not exceed ${ADDRESS_MAX_LENGTH} characters`,
    DESCRIPTION_MAX_LENGTH: `Description must not exceed ${DESCRIPTION_MAX_LENGTH} characters`,
    START_TIME_REQUIRED: 'Start time is required',
    END_TIME_REQUIRED: 'End time is required',
    END_TIME_AFTER_START: 'End time must be after start time',
    TRIP_MIN_DURATION: `Trip must be at least ${TRIP_DURATION_MIN_MINUTES} minute long`,
    MAX_SPEED_POSITIVE: 'Max speed must be greater than 0',
    MAX_SPEED_FORMAT: `Max speed must have at most ${MAX_SPEED_INTEGER_DIGITS} integer digits and ${MAX_SPEED_FRACTION_DIGITS} decimal digits`,
    ORIGIN_MAX_LENGTH: `Origin must not exceed ${ADDRESS_MAX_LENGTH} characters`,
    DESTINATION_MAX_LENGTH: `Destination must not exceed ${ADDRESS_MAX_LENGTH} characters`,
    DATE_RANGE_INVALID: 'End date must be after or equal to start date',
} as const;

/**
 * Validation error messages for coordinate-related fields.
 */
export const COORDINATE_VALIDATION_MESSAGES = {
    LATITUDE_REQUIRED: 'Latitude is required',
    LATITUDE_RANGE: `Latitude must be between ${LATITUDE_MIN} and ${LATITUDE_MAX}`,
    LONGITUDE_REQUIRED: 'Longitude is required',
    LONGITUDE_RANGE: `Longitude must be between ${LONGITUDE_MIN} and ${LONGITUDE_MAX}`,
} as const;