/**
 * Composable for handling map click interactions with smart routing.
 * Supports both targeted clicks (when input is focused) and sequential auto-fill.
 */
import { ref } from 'vue'

export interface ActiveField {
    type: 'route' | 'obstacle'
    index: number
}

export interface MapClickCallbacks {
    onRouteClick: (index: number, lng: number, lat: number) => void
    onObstacleClick: (index: number, lng: number, lat: number) => void
    onAddWaypoint: (beforeIndex: number, lng: number, lat: number) => void
    getCurrentAddresses: () => string[]
}

/**
 * Creates map click handler with active field tracking and sequential filling.
 */
export function useMapClickHandler() {
    const activeField = ref<ActiveField | null>(null)

    /**
     * Sets active field when input is focused.
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
     */
    function handleMapClick(
        lng: number,
        lat: number,
        callbacks: MapClickCallbacks
    ) {
        const addresses = callbacks.getCurrentAddresses()

        // Targeted click: user focused specific input
        if (activeField.value) {
            const { type, index } = activeField.value

            if (type === 'route') {
                const wasEmpty = !addresses[index] || addresses[index].trim() === ''
                callbacks.onRouteClick(index, lng, lat)

                if (wasEmpty) {
                    const nextEmptyIndex = addresses.findIndex((addr, idx) =>
                        idx > index && (!addr || addr.trim() === '')
                    )

                    if (nextEmptyIndex !== -1) {
                        setActiveField('route', nextEmptyIndex)
                    } else {
                        activeField.value = null
                    }
                } else {
                    setActiveField('route', index)
                }
            } else {
                callbacks.onObstacleClick(index, lng, lat)
                activeField.value = null
            }
            return
        }

        // Sequential auto-fill: find next empty address
        const emptyIndex = addresses.findIndex(addr => !addr || addr.trim() === '')

        if (emptyIndex !== -1) {
            callbacks.onRouteClick(emptyIndex, lng, lat)

            const nextEmptyIndex = addresses.findIndex((addr, idx) =>
                idx > emptyIndex && (!addr || addr.trim() === '')
            )

            if (nextEmptyIndex !== -1) {
                setActiveField('route', nextEmptyIndex)
            } else {
                activeField.value = null
            }
        } else {
            // All addresses filled, insert new waypoint before destination
            const newIndex = addresses.length - 1
            callbacks.onAddWaypoint(newIndex, lng, lat)
            activeField.value = null
        }
    }

    return {
        activeField,
        setActiveField,
        clearActiveField,
        handleMapClick
    }
}