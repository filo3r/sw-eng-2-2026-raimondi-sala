/**
 * Composable for calculating route marker visual configuration.
 * Determines marker color and label based on position in route (origin, destination, or waypoint).
 * Origin uses green with 'O', destination uses purple with 'D', waypoints use blue with numbers.
 */
import {
    ROUTE_MARKER_COLOR,
    ORIGIN_MARKER_COLOR,
    DESTINATION_MARKER_COLOR
} from '@/constants/map'

/**
 * Route marker visual configuration for map display.
 */
export interface RouteMarkerConfig {
    /** Marker background color (hex format) */
    color: string
    /** Marker label text ('O' for origin, 'D' for destination, number for waypoints) */
    label: string
}

/**
 * Calculates marker visual configuration based on position in the route.
 * First marker is origin (green, 'O'), last is destination (purple, 'D'), others are waypoints (blue, numbered).
 * @param index - Current marker index in route (0-based)
 * @param totalAddresses - Total number of addresses in the route (minimum 2)
 * @returns Marker configuration with appropriate color and label
 */
export function getRouteMarkerConfig(index: number, totalAddresses: number): RouteMarkerConfig {
    // Origin marker: first address (green with 'O' label)
    if (index === 0) {
        return {
            color: ORIGIN_MARKER_COLOR,
            label: 'O'
        }
    }
    // Destination marker: last address (purple with 'D' label)
    if (index === totalAddresses - 1) {
        return {
            color: DESTINATION_MARKER_COLOR,
            label: 'D'
        }
    }
    // Waypoint markers: intermediate addresses (blue with numeric label)
    return {
        color: ROUTE_MARKER_COLOR,
        label: String(index)
    }
}