/*
 * Mapbox configuration and styling constants for the Bike Path application.
 */

// ============================================================================
// MAP CONFIGURATION
// ============================================================================

/**
 * Mapbox map style URL.
 * Using the outdoors style optimized for cycling and outdoor activities.
 */
export const MAPBOX_STYLE = 'mapbox://styles/mapbox/outdoors-v12'

/**
 * Default map center coordinates [longitude, latitude].
 * Centered on Milan, Italy.
 */
export const DEFAULT_MAP_CENTER: [number, number] = [9.19, 45.46]

/**
 * Default zoom level for map initialization.
 */
export const DEFAULT_ZOOM = 12

/**
 * Default pitch angle in degrees (0 = top-down view).
 */
export const DEFAULT_PITCH = 0

/**
 * Default bearing angle in degrees (0 = north up).
 */
export const DEFAULT_BEARING = 0

// ============================================================================
// MAP ANIMATION
// ============================================================================

/**
 * Duration in milliseconds for map fit bounds animation.
 */
export const FIT_BOUNDS_DURATION = 1000

/**
 * Duration in milliseconds for compass reset animation.
 */
export const COMPASS_RESET_DURATION = 500

/**
 * Padding in pixels when fitting bounds around a route.
 */
export const FIT_BOUNDS_PADDING = 50

/**
 * Timeout in milliseconds for map resize after mount.
 */
export const MAP_RESIZE_TIMEOUT = 300

/**
 * Timeout in milliseconds for geolocation request.
 */
export const GEOLOCATION_TIMEOUT = 5000

// ============================================================================
// ROUTE STYLING
// ============================================================================

/**
 * Color for bike path route line.
 */
export const ROUTE_LINE_COLOR = '#3b82f6'

/**
 * Width in pixels for bike path route line.
 */
export const ROUTE_LINE_WIDTH = 4

// ============================================================================
// MARKER COLORS
// ============================================================================

/**
 * Color for origin marker (green).
 */
export const ORIGIN_MARKER_COLOR = '#10b981'

/**
 * Color for destination marker (purple).
 */
export const DESTINATION_MARKER_COLOR = '#a855f7'

/**
 * Obstacle severity color mapping.
 * Maps severity levels to marker colors.
 */
export const OBSTACLE_SEVERITY_COLORS = {
    LOW: '#fde047',      // light yellow
    MEDIUM: '#fb923c',   // orange
    HIGH: '#ef4444',     // red
    CRITICAL: '#7f1d1d'  // dark red
} as const

/**
 * Default obstacle marker color (used as fallback).
 */
export const DEFAULT_OBSTACLE_COLOR = '#ef4444'

// ============================================================================
// MARKER STYLING
// ============================================================================

/**
 * Size in pixels for obstacle markers.
 */
export const OBSTACLE_MARKER_SIZE = 24

/**
 * Border width in pixels for obstacle markers.
 */
export const OBSTACLE_MARKER_BORDER_WIDTH = 2

/**
 * Border color for obstacle markers.
 */
export const OBSTACLE_MARKER_BORDER_COLOR = 'white'

/**
 * Box shadow for obstacle markers.
 */
export const OBSTACLE_MARKER_BOX_SHADOW = '0 2px 4px rgba(0,0,0,0.3)'

// ============================================================================
// POPUP CONFIGURATION
// ============================================================================

/**
 * Offset in pixels for map popups.
 */
export const POPUP_OFFSET = 25

/**
 * CSS class name for obstacle popups.
 */
export const OBSTACLE_POPUP_CLASS = 'obstacle-popup'

/**
 * Minimum width in pixels for popup content.
 */
export const POPUP_MIN_WIDTH = 200

/**
 * Padding in pixels for popup content.
 */
export const POPUP_CONTENT_PADDING = 12