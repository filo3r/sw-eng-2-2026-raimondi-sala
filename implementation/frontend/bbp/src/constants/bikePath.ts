/**
 * Bike path related constants for search radius and status options.
 * Provides mappings between enum values and human-readable labels for forms and displays.
 */

/**
 * Radius option for bike path geographic search.
 */
export interface RadiusOption {
    /** Radius value in kilometers */
    value: number
    /** Human-readable label with distance unit */
    label: string
}

/** Available radius options for bike path finder search (both origin and destination) */
export const RADIUS_OPTIONS: readonly RadiusOption[] = [
    { value: 0.1, label: '100m' },
    { value: 0.25, label: '250m' },
    { value: 0.5, label: '500m' },
    { value: 1, label: '1 km' },
    { value: 2, label: '2 km' },
    { value: 5, label: '5 km' },
    { value: 10, label: '10 km' }
] as const

/** Default search radius in kilometers for bike path finder */
export const DEFAULT_RADIUS_KM = 0.1

import type { BikePathStatus } from '@/types/bikePath'

/**
 * Bike path status option for UI selection in forms and dropdowns.
 */
export interface BikePathStatusOption {
    /** Status enum value */
    value: BikePathStatus
    /** Human-readable label for display */
    label: string
    /** Numeric quality score (1-10 for quality statuses, null for operational statuses) */
    score: number | null
}

/** Available bike path status options for forms and filters (ordered by score: highest to lowest, then non-scored) */
export const BIKE_PATH_STATUS_OPTIONS: readonly BikePathStatusOption[] = [
    { value: 'EXCELLENT', label: 'Excellent', score: 10 },
    { value: 'VERY_GOOD', label: 'Very Good', score: 9 },
    { value: 'GOOD', label: 'Good', score: 8 },
    { value: 'FAIR', label: 'Fair', score: 7 },
    { value: 'SUFFICIENT', label: 'Sufficient', score: 6 },
    { value: 'MEDIOCRE', label: 'Mediocre', score: 5 },
    { value: 'POOR', label: 'Poor', score: 4 },
    { value: 'VERY_POOR', label: 'Very Poor', score: 3 },
    { value: 'CRITICAL', label: 'Critical', score: 2 },
    { value: 'IMPASSABLE', label: 'Impassable', score: 1 },
    { value: 'UNDER_MAINTENANCE', label: 'Under Maintenance', score: null },
    { value: 'TEMPORARILY_CLOSED', label: 'Temporarily Closed', score: null },
    { value: 'PERMANENTLY_CLOSED', label: 'Permanently Closed', score: null }
] as const