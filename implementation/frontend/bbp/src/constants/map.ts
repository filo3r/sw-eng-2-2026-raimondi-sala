/**
 * Mapbox configuration and styling constants.
 * Defines map appearance, controls, animations, markers, popups, and static map generation settings.
 */

// Base map configuration
export const MAPBOX_STYLE = 'mapbox://styles/mapbox/outdoors-v12' // Mapbox outdoor style optimized for cycling
export const DEFAULT_MAP_CENTER: [number, number] = [9.19, 45.46] // Milan, Italy (lng, lat)
export const DEFAULT_ZOOM = 12 // City-level zoom
export const DEFAULT_PITCH = 0 // 2D view (0 = flat, max 85)
export const DEFAULT_BEARING = 0 // North orientation (0-359 degrees)

// Map settings
export const MAP_LANGUAGE = 'en' // Map labels language
export const MAP_COLLECT_RESOURCE_TIMING = false // Disable performance metrics collection

// Control positions
export const ATTRIBUTION_POSITION = 'bottom-right' as const // Mapbox attribution control position
export const NAVIGATION_POSITION = 'top-right' as const // Zoom/compass control position

// Attribution settings
export const ATTRIBUTION_COMPACT = true // Use compact attribution control

// Geolocation settings
export const GEOLOCATION_HIGH_ACCURACY = true // Request high-accuracy GPS positioning
export const GEOLOCATION_TRACK_USER = true // Continuously track user location
export const GEOLOCATION_SHOW_HEADING = true // Show direction indicator

// Animation timings (milliseconds)
export const FIT_BOUNDS_DURATION = 1000 // Duration for fitBounds animation
export const COMPASS_RESET_DURATION = 500 // Duration for compass reset animation
export const FIT_BOUNDS_PADDING = 100 // Padding around features when fitting bounds (pixels)
export const MAP_RESIZE_TIMEOUT = 300 // Debounce timeout for map resize events
export const GEOLOCATION_TIMEOUT = 5000 // Timeout for geolocation requests

// Route line styling
export const ROUTE_LINE_COLOR = '#3b82f6' // Blue-500 for cycling routes
export const ROUTE_LINE_WIDTH = 4 // Line width in pixels
export const ROUTE_LINE_JOIN = 'round' as const // Smooth line joins
export const ROUTE_LINE_CAP = 'round' as const // Rounded line ends

// Marker colors (hex format)
export const ORIGIN_MARKER_COLOR = '#10b981' // Green-500 for route origin
export const DESTINATION_MARKER_COLOR = '#a855f7' // Purple-500 for route destination
export const OBSTACLE_SEVERITY_COLORS = {
    LOW: '#fde047', // Yellow-300
    MEDIUM: '#fb923c', // Orange-400
    HIGH: '#ef4444', // Red-500
    CRITICAL: '#7f1d1d' // Red-900
} as const
export const DEFAULT_OBSTACLE_COLOR = '#ef4444' // Red-500 fallback

// Obstacle marker styling
export const OBSTACLE_MARKER_SIZE = 24 // Marker diameter in pixels
export const OBSTACLE_MARKER_BORDER_WIDTH = 2 // Border width in pixels
export const OBSTACLE_MARKER_BORDER_COLOR = 'white' // White border for contrast
export const OBSTACLE_MARKER_BOX_SHADOW = '0 2px 4px rgba(0,0,0,0.3)' // Subtle shadow

// Popup configuration
export const POPUP_OFFSET = 25 // Vertical offset from marker in pixels
export const OBSTACLE_POPUP_CLASS = 'obstacle-popup' // CSS class for obstacle popups
export const ROUTE_POPUP_CLASS = 'route-popup' // CSS class for route popups
export const POPUP_MIN_WIDTH = 200 // Minimum popup width in pixels
export const POPUP_CONTENT_PADDING = 12 // Inner padding in pixels

// Static map generation configuration
export const STATIC_MAP_WIDTH = 600 // Generated image width in pixels
export const STATIC_MAP_HEIGHT = 400 // Generated image height in pixels
export const STATIC_MAP_STYLE_ID = 'outdoors-v12' // Mapbox style ID for static maps
export const STATIC_MAP_USERNAME = 'mapbox' // Mapbox username for static API
export const STATIC_MAP_STROKE_OPACITY = 0.9 // Route line opacity (0-1)
export const STATIC_MAP_MAX_URL_LENGTH = 8192 // Mapbox static API URL length limit

// Douglas-Peucker simplification parameters
export const STATIC_MAP_MIN_TOLERANCE = 0.00001 // Initial tolerance for route simplification
export const STATIC_MAP_MAX_TOLERANCE = 0.01 // Maximum tolerance before giving up
export const STATIC_MAP_TOLERANCE_STEP = 0.00005 // Increment step for tolerance

// Static map smart padding configuration
export const STATIC_MAP_MIN_VERTICAL_PADDING = 90 // Minimum top/bottom padding in pixels
export const STATIC_MAP_MIN_HORIZONTAL_PADDING = 60 // Minimum left/right padding in pixels
export const STATIC_MAP_VERTICAL_PADDING_PERCENT = 0.15 // Percentage-based vertical padding (15%)
export const STATIC_MAP_HORIZONTAL_PADDING_PERCENT = 0.10 // Percentage-based horizontal padding (10%)
export const STATIC_MAP_MAX_PADDING_PERCENT = 0.45 // Maximum padding as percentage of dimension (45%)

// Autocomplete configuration
export const AUTOCOMPLETE_DEBOUNCE_MS = 300 // Debounce delay for search input
export const AUTOCOMPLETE_MIN_CHARS = 3 // Minimum characters to trigger search
export const AUTOCOMPLETE_LIMIT = 5 // Maximum number of suggestions
export const AUTOCOMPLETE_BLUR_DELAY_MS = 200 // Delay before hiding suggestions on blur

// Route update configuration
export const ROUTE_UPDATE_DEBOUNCE_MS = 300 // Debounce delay for route recalculation

// Interactive marker colors
export const ROUTE_MARKER_COLOR = '#3b82f6' // Blue-500 for draggable route markers

// Map cursor styles
export const MAP_CURSOR_CROSSHAIR = 'crosshair' // Cursor for coordinate selection mode
export const MAP_CURSOR_DEFAULT = 'default' // Default cursor style

// Mapbox source and layer identifiers
export const ROUTE_SOURCE_ID = 'route' // GeoJSON source ID for bike path routes
export const ROUTE_LAYER_ID = 'route' // Layer ID for rendering bike path routes
export const TRIP_ROUTE_SOURCE_ID = 'trip-route' // GeoJSON source ID for trip routes
export const TRIP_ROUTE_LAYER_ID = 'trip-route' // Layer ID for rendering trip routes