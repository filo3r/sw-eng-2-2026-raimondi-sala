/**
 * Obstacle related constants for types, severities, and UI options.
 * Provides mappings between enum values and human-readable labels for forms and displays.
 */

import type { ObstacleType, ObstacleSeverity } from '@/types/obstacle'

/**
 * Obstacle type option for UI selection in forms and dropdowns.
 */
export interface ObstacleTypeOption {
    /** Obstacle type enum value */
    value: ObstacleType
    /** Human-readable label for display */
    label: string
}

/** Available obstacle type options for forms and filters (ordered alphabetically by label) */
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
 * Obstacle severity option for UI selection in forms and dropdowns.
 */
export interface ObstacleSeverityOption {
    /** Severity enum value */
    value: ObstacleSeverity
    /** Human-readable label for display */
    label: string
    /** Numeric severity level for sorting and filtering (1-4, higher is more severe) */
    level: number
}

/** Available obstacle severity options for forms and filters (ordered by level: lowest to highest) */
export const OBSTACLE_SEVERITY_OPTIONS: readonly ObstacleSeverityOption[] = [
    { value: 'LOW', label: 'Low', level: 1 },
    { value: 'MEDIUM', label: 'Medium', level: 2 },
    { value: 'HIGH', label: 'High', level: 3 },
    { value: 'CRITICAL', label: 'Critical', level: 4 }
] as const