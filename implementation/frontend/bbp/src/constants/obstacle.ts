/*
 * Obstacle related constants.
 */

import type { ObstacleType, ObstacleSeverity } from '@/types/obstacle'

// ============================================================================
// OBSTACLE TYPE OPTIONS
// ============================================================================

/**
 * Obstacle type option for UI selection.
 */
export interface ObstacleTypeOption {
    /** Type enum value */
    value: ObstacleType
    /** Human-readable label */
    label: string
}

/**
 * Available obstacle type options for forms and filters.
 * Ordered alphabetically by label.
 */
export const OBSTACLE_TYPE_OPTIONS: readonly ObstacleTypeOption[] = [
    { value: 'ANIMAL', label: 'Animal' },
    { value: 'BARRIER', label: 'Barrier' },
    { value: 'CONSTRUCTION', label: 'Construction' },
    { value: 'CRACK', label: 'Crack' },
    { value: 'DAMAGED_SURFACE', label: 'Damaged Surface' },
    { value: 'DEBRIS', label: 'Debris' },
    { value: 'FALLEN_TREE', label: 'Fallen Tree' },
    { value: 'FLOODING', label: 'Flooding' },
    { value: 'GLASS', label: 'Glass' },
    { value: 'ICE', label: 'Ice' },
    { value: 'LOOSE_GRAVEL', label: 'Loose Gravel' },
    { value: 'MANHOLE_COVER', label: 'Manhole Cover' },
    { value: 'MISSING_DRAIN', label: 'Missing Drain' },
    { value: 'MUD', label: 'Mud' },
    { value: 'NARROW_PATH', label: 'Narrow Path' },
    { value: 'OIL_SPILL', label: 'Oil Spill' },
    { value: 'OTHER', label: 'Other' },
    { value: 'OVERGROWN_VEGETATION', label: 'Overgrown Vegetation' },
    { value: 'PARKED_VEHICLE', label: 'Parked Vehicle' },
    { value: 'POTHOLE', label: 'Pothole' },
    { value: 'PUDDLE', label: 'Puddle' },
    { value: 'ROOT_DAMAGE', label: 'Root Damage' },
    { value: 'SAND', label: 'Sand' },
    { value: 'SLIPPERY_SURFACE', label: 'Slippery Surface' },
    { value: 'SPEED_BUMP', label: 'Speed Bump' },
    { value: 'UNEVEN_SURFACE', label: 'Uneven Surface' }
] as const

/**
 * Map obstacle type to human-readable label.
 */
export const OBSTACLE_TYPE_LABELS: Record<ObstacleType, string> = {
    POTHOLE: 'Pothole',
    CRACK: 'Crack',
    DAMAGED_SURFACE: 'Damaged Surface',
    UNEVEN_SURFACE: 'Uneven Surface',
    ROOT_DAMAGE: 'Root Damage',
    SPEED_BUMP: 'Speed Bump',
    MANHOLE_COVER: 'Manhole Cover',
    MISSING_DRAIN: 'Missing Drain',
    LOOSE_GRAVEL: 'Loose Gravel',
    SAND: 'Sand',
    MUD: 'Mud',
    GLASS: 'Glass',
    DEBRIS: 'Debris',
    PUDDLE: 'Puddle',
    FLOODING: 'Flooding',
    ICE: 'Ice',
    SLIPPERY_SURFACE: 'Slippery Surface',
    OIL_SPILL: 'Oil Spill',
    FALLEN_TREE: 'Fallen Tree',
    BARRIER: 'Barrier',
    PARKED_VEHICLE: 'Parked Vehicle',
    OVERGROWN_VEGETATION: 'Overgrown Vegetation',
    ANIMAL: 'Animal',
    CONSTRUCTION: 'Construction',
    NARROW_PATH: 'Narrow Path',
    OTHER: 'Other'
}

// ============================================================================
// OBSTACLE SEVERITY OPTIONS
// ============================================================================

/**
 * Obstacle severity option for UI selection.
 */
export interface ObstacleSeverityOption {
    /** Severity enum value */
    value: ObstacleSeverity
    /** Human-readable label */
    label: string
    /** Numeric severity level (1-4) */
    level: number
}

/**
 * Available obstacle severity options for forms and filters.
 * Ordered by severity level (lowest to highest).
 */
export const OBSTACLE_SEVERITY_OPTIONS: readonly ObstacleSeverityOption[] = [
    { value: 'LOW', label: 'Low', level: 1 },
    { value: 'MEDIUM', label: 'Medium', level: 2 },
    { value: 'HIGH', label: 'High', level: 3 },
    { value: 'CRITICAL', label: 'Critical', level: 4 }
] as const

/**
 * Map obstacle severity to human-readable label.
 */
export const OBSTACLE_SEVERITY_LABELS: Record<ObstacleSeverity, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    CRITICAL: 'Critical'
}