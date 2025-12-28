/*
 * Bike path related constants and enums.
 */

// ============================================================================
// RADIUS CONFIGURATION
// ============================================================================

/**
 * Radius option for bike path search.
 */
export interface RadiusOption {
    /** Radius value in kilometers */
    value: number
    /** Human-readable label */
    label: string
}

/**
 * Available radius options for bike path finder.
 * Used for both origin and destination radius filters.
 */
export const RADIUS_OPTIONS: readonly RadiusOption[] = [
    { value: 0.1, label: '100m' },
    { value: 0.25, label: '250m' },
    { value: 0.5, label: '500m' },
    { value: 1, label: '1 km' },
    { value: 2, label: '2 km' },
    { value: 5, label: '5 km' },
    { value: 10, label: '10 km' }
] as const

/**
 * Default radius in kilometers for bike path search.
 */
export const DEFAULT_RADIUS_KM = 0.1

// ============================================================================
// BIKE PATH STATUS OPTIONS
// ============================================================================

import type { BikePathStatus } from '@/types/bikePath'

/**
 * Bike path status option for UI selection.
 */
export interface BikePathStatusOption {
    /** Status enum value */
    value: BikePathStatus
    /** Human-readable label */
    label: string
    /** Numeric score (null for non-quality statuses) */
    score: number | null
}

/**
 * Available bike path status options for forms and filters.
 * Ordered by score (highest to lowest), with non-scored statuses at the end.
 */
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

/**
 * Map bike path status to human-readable label.
 */
export const BIKE_PATH_STATUS_LABELS: Record<BikePathStatus, string> = {
    EXCELLENT: 'Excellent',
    VERY_GOOD: 'Very Good',
    GOOD: 'Good',
    FAIR: 'Fair',
    SUFFICIENT: 'Sufficient',
    MEDIOCRE: 'Mediocre',
    POOR: 'Poor',
    VERY_POOR: 'Very Poor',
    CRITICAL: 'Critical',
    IMPASSABLE: 'Impassable',
    UNDER_MAINTENANCE: 'Under Maintenance',
    TEMPORARILY_CLOSED: 'Temporarily Closed',
    PERMANENTLY_CLOSED: 'Permanently Closed'
}