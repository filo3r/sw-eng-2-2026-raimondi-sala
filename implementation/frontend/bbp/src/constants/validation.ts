/**
 * Validation constants and rules for the Bike Path application.
 * Based on backend validation constraints from DTOs and entities.
 * Ensures frontend validation matches backend requirements.
 */

// User field length constraints
/** Maximum length for user's first name (chars) */
export const USER_NAME_MAX_LENGTH = 50;
/** Maximum length for user's last name (chars) */
export const USER_SURNAME_MAX_LENGTH = 50;
/** Maximum length for unique username (chars) */
export const USERNAME_MAX_LENGTH = 50;
/** Maximum length for email address (chars) */
export const EMAIL_MAX_LENGTH = 150;
/** Minimum length for password security (chars) */
export const PASSWORD_MIN_LENGTH = 8;
/** Maximum length for password BCrypt hash (chars) */
export const PASSWORD_MAX_LENGTH = 60;

// Address and description length constraints
/** Maximum length for address or location description (chars) */
export const ADDRESS_MAX_LENGTH = 256;
/** Maximum length for description or notes fields (chars) */
export const DESCRIPTION_MAX_LENGTH = 500;
/** Minimum number of addresses required for bike path or trip route */
export const ADDRESSES_MIN_COUNT = 2;

// Geographic coordinate range constraints
/** Minimum latitude value in decimal degrees (South pole) */
export const LATITUDE_MIN = -90.0;
/** Maximum latitude value in decimal degrees (North pole) */
export const LATITUDE_MAX = 90.0;
/** Minimum longitude value in decimal degrees (West) */
export const LONGITUDE_MIN = -180.0;
/** Maximum longitude value in decimal degrees (East) */
export const LONGITUDE_MAX = 180.0;

// Trip validation constraints
/** Minimum trip duration in minutes (prevents invalid short trips) */
export const TRIP_DURATION_MIN_MINUTES = 1;

// Regex patterns for format validation
/** Email validation pattern (RFC 5322 simplified) */
export const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
/** Decimal number pattern for max speed validation (max 3 integer digits, 2 decimal places) */
export const MAX_SPEED_PATTERN = /^\d{1,3}(\.\d{1,2})?$/;