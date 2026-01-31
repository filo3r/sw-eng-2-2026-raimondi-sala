/**
 * Map marker utilities for creating custom styled markers.
 */
import {
    OBSTACLE_MARKER_SIZE,
    OBSTACLE_MARKER_BORDER_WIDTH,
    OBSTACLE_MARKER_BORDER_COLOR,
    OBSTACLE_MARKER_BOX_SHADOW
} from '@/constants/map'

/**
 * Configuration options for creating a custom map marker.
 * @property color - Background color of the marker (hex format)
 * @property label - Text label displayed inside the marker
 * @property draggable - Whether the marker can be dragged (default: false, shows grab cursor)
 */
export interface CustomMarkerOptions {
    color: string
    label: string
    draggable?: boolean
}

/**
 * Creates a custom circular marker element with centered label text.
 * Returns a styled div element ready to be used with Mapbox GL JS Marker.
 * @param options - Marker styling and behavior configuration
 * @returns Configured HTML div element with inline styles
 */
export function createCustomMarkerElement(options: CustomMarkerOptions): HTMLElement {
    const el = document.createElement('div')
    // Set size and circular shape
    el.style.backgroundColor = options.color
    el.style.width = `${OBSTACLE_MARKER_SIZE}px`
    el.style.height = `${OBSTACLE_MARKER_SIZE}px`
    el.style.borderRadius = '50%'
    // Add border and shadow styling
    el.style.border = `${OBSTACLE_MARKER_BORDER_WIDTH}px solid ${OBSTACLE_MARKER_BORDER_COLOR}`
    el.style.boxShadow = OBSTACLE_MARKER_BOX_SHADOW
    el.style.cursor = options.draggable ? 'grab' : 'pointer'
    // Configure flexbox for centered label
    el.style.display = 'flex'
    el.style.alignItems = 'center'
    el.style.justifyContent = 'center'
    // Style label text
    el.style.color = 'white'
    el.style.fontWeight = '600'
    el.style.fontSize = '12px'
    el.textContent = options.label
    return el
}