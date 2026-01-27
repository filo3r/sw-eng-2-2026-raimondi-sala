/**
 * Map marker utilities for creating custom styled markers.
 */
import {
    OBSTACLE_MARKER_SIZE,
    OBSTACLE_MARKER_BORDER_WIDTH,
    OBSTACLE_MARKER_BORDER_COLOR,
    OBSTACLE_MARKER_BOX_SHADOW
} from '@/constants/map'

export interface CustomMarkerOptions {
    color: string
    label: string
    draggable?: boolean
}

/**
 * Creates a custom circular marker element with label.
 * @param options - Marker styling and behavior options
 * @returns Configured HTML element for use with Mapbox Marker
 */
export function createCustomMarkerElement(options: CustomMarkerOptions): HTMLElement {
    const el = document.createElement('div')
    el.style.backgroundColor = options.color
    el.style.width = `${OBSTACLE_MARKER_SIZE}px`
    el.style.height = `${OBSTACLE_MARKER_SIZE}px`
    el.style.borderRadius = '50%'
    el.style.border = `${OBSTACLE_MARKER_BORDER_WIDTH}px solid ${OBSTACLE_MARKER_BORDER_COLOR}`
    el.style.cursor = options.draggable ? 'grab' : 'pointer'
    el.style.boxShadow = OBSTACLE_MARKER_BOX_SHADOW
    el.style.display = 'flex'
    el.style.alignItems = 'center'
    el.style.justifyContent = 'center'
    el.style.color = 'white'
    el.style.fontWeight = '600'
    el.style.fontSize = '12px'
    el.textContent = options.label
    return el
}