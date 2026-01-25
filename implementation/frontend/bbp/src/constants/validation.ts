/**
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