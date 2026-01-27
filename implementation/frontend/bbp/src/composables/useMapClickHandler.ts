/**
 * Composable for handling map click interactions with smart routing.
 * Supports both targeted clicks (when input is focused) and sequential auto-fill.
 */
import { ref, type Ref } from 'vue'

export interface ActiveField {
    type: 'route' | 'obstacle'
    index: number
}

export interface MapClickCallbacks {
    onRouteClick: (index: number, lng: number, lat: number) => void
    onObstacleClick: (index: number, lng: number, lat: number) => void
}

/**
 * Creates map click handler with active field tracking and sequential filling.
 * @returns Active field state and click handling methods
 */
export function useMapClickHandler() {
    const activeField = ref<ActiveField | null>(null)

    /**
     * Sets active field when input is focused.
     * @param type - Field type (route or obstacle)
     * @param index - Field index
     */
    function setActiveField(type: 'route' | 'obstacle', index: number) {
        activeField.value = { type, index }
    }

    /**
     * Clears active field (typically on blur).
     */
    function clearActiveField() {
        activeField.value = null
    }

    /**
     * Handles map click with smart routing logic.
     * If activeField is set: fills that specific field.
     * Otherwise: auto-fills next empty route address or adds new waypoint.
     *
     * @param lng - Longitude coordinate
     * @param lat - Latitude coordinate
     * @param addresses - Current route addresses array
     * @param callbacks - Callbacks for route and obstacle clicks
     */
    function handleMapClick(
        lng: number,
        lat: number,
        addresses: Ref<string[]>,
        callbacks: MapClickCallbacks
    ) {
        // Targeted click: user focused specific input
        if (activeField.value) {
            const { type, index } = activeField.value

            if (type === 'route') {
                callbacks.onRouteClick(index, lng, lat)
            } else {
                callbacks.onObstacleClick(index, lng, lat)
            }
            return
        }

        // Sequential auto-fill: find next empty address
        const emptyIndex = addresses.value.findIndex(addr => !addr || addr.trim() === '')

        if (emptyIndex !== -1) {
            // Fill first empty address
            callbacks.onRouteClick(emptyIndex, lng, lat)
        } else {
            // All addresses filled, add new waypoint
            const newIndex = addresses.value.length
            addresses.value.push('')
            callbacks.onRouteClick(newIndex, lng, lat)
        }
    }

    return {
        activeField,
        setActiveField,
        clearActiveField,
        handleMapClick
    }
}