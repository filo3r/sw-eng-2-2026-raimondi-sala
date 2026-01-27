/**
 * Composable for calculating route marker configuration.
 * Determines color and label based on position in route.
 */
import {
    ROUTE_MARKER_COLOR,
    ORIGIN_MARKER_COLOR,
    DESTINATION_MARKER_COLOR
} from '@/constants/map'

export interface RouteMarkerConfig {
    color: string
    label: string
}

/**
 * Calculates marker configuration based on position in route.
 * @param index - Current marker index
 * @param totalAddresses - Total number of addresses in route
 * @returns Marker configuration with color and label
 */
export function getRouteMarkerConfig(index: number, totalAddresses: number): RouteMarkerConfig {
    // Origin marker (first)
    if (index === 0) {
        return {
            color: ORIGIN_MARKER_COLOR,
            label: 'O'
        }
    }

    // Destination marker (last)
    if (index === totalAddresses - 1) {
        return {
            color: DESTINATION_MARKER_COLOR,
            label: 'D'
        }
    }

    // Waypoint markers (numbered)
    return {
        color: ROUTE_MARKER_COLOR,
        label: String(index)
    }
}

/**
 * Creates route marker configuration utilities.
 * @returns Method to get marker config
 */
export function useRouteMarkerConfig() {
    return {
        getRouteMarkerConfig
    }
}