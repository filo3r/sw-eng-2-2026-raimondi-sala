/**
 * Mapbox configuration and styling constants.
 */

// Map configuration
export const MAPBOX_STYLE = 'mapbox://styles/mapbox/outdoors-v12'
export const DEFAULT_MAP_CENTER: [number, number] = [9.19, 45.46] // Milan, Italy
export const DEFAULT_ZOOM = 12
export const DEFAULT_PITCH = 0
export const DEFAULT_BEARING = 0

// Map settings
export const MAP_LANGUAGE = 'en'
export const MAP_COLLECT_RESOURCE_TIMING = false

// Control positions
export const ATTRIBUTION_POSITION = 'bottom-right' as const
export const NAVIGATION_POSITION = 'top-right' as const

// Attribution settings
export const ATTRIBUTION_COMPACT = true

// Geolocation settings
export const GEOLOCATION_HIGH_ACCURACY = true
export const GEOLOCATION_TRACK_USER = true
export const GEOLOCATION_SHOW_HEADING = true

// Animation timings
export const FIT_BOUNDS_DURATION = 1000
export const COMPASS_RESET_DURATION = 500
export const FIT_BOUNDS_PADDING = 50
export const MAP_RESIZE_TIMEOUT = 300
export const GEOLOCATION_TIMEOUT = 5000

// Route styling
export const ROUTE_LINE_COLOR = '#3b82f6'
export const ROUTE_LINE_WIDTH = 4
export const ROUTE_LINE_JOIN = 'round' as const
export const ROUTE_LINE_CAP = 'round' as const

// Marker colors
export const ORIGIN_MARKER_COLOR = '#10b981'
export const DESTINATION_MARKER_COLOR = '#a855f7'
export const OBSTACLE_SEVERITY_COLORS = {
    LOW: '#fde047',
    MEDIUM: '#fb923c',
    HIGH: '#ef4444',
    CRITICAL: '#7f1d1d'
} as const
export const DEFAULT_OBSTACLE_COLOR = '#ef4444'

// Marker styling
export const OBSTACLE_MARKER_SIZE = 24
export const OBSTACLE_MARKER_BORDER_WIDTH = 2
export const OBSTACLE_MARKER_BORDER_COLOR = 'white'
export const OBSTACLE_MARKER_BOX_SHADOW = '0 2px 4px rgba(0,0,0,0.3)'

// Popup configuration
export const POPUP_OFFSET = 25
export const OBSTACLE_POPUP_CLASS = 'obstacle-popup'
export const ROUTE_POPUP_CLASS = 'route-popup'
export const POPUP_MIN_WIDTH = 200
export const POPUP_CONTENT_PADDING = 12

// Static map configuration
export const STATIC_MAP_WIDTH = 600
export const STATIC_MAP_HEIGHT = 400
export const STATIC_MAP_STYLE_ID = 'outdoors-v12'
export const STATIC_MAP_USERNAME = 'mapbox'
export const STATIC_MAP_STROKE_OPACITY = 0.9
export const STATIC_MAP_MAX_URL_LENGTH = 8192
export const STATIC_MAP_MIN_TOLERANCE = 0.00001
export const STATIC_MAP_MAX_TOLERANCE = 0.01
export const STATIC_MAP_TOLERANCE_STEP = 0.00005

// Static map smart padding configuration
export const STATIC_MAP_MIN_VERTICAL_PADDING = 90
export const STATIC_MAP_MIN_HORIZONTAL_PADDING = 60
export const STATIC_MAP_VERTICAL_PADDING_PERCENT = 0.15
export const STATIC_MAP_HORIZONTAL_PADDING_PERCENT = 0.10
export const STATIC_MAP_MAX_PADDING_PERCENT = 0.45