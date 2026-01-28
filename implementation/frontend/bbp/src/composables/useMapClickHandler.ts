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
     * If activeField is set: fills/overwrites that field, then moves to next empty only if it was empty before.
     * Otherwise: auto-fills next empty route address or adds new waypoint.
     * Automatically sets the active field after handling the click.
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
                // Check if the current field was empty before click
                const wasEmpty = !addresses.value[index] || addresses.value[index].trim() === ''

                // Fill/overwrite the current field
                callbacks.onRouteClick(index, lng, lat)

                if (wasEmpty) {
                    // Field was empty, check if there are more empty fields
                    const nextEmptyIndex = addresses.value.findIndex((addr, idx) =>
                        idx > index && (!addr || addr.trim() === '')
                    )

                    if (nextEmptyIndex !== -1) {
                        // Move to next empty field
                        setActiveField('route', nextEmptyIndex)
                    } else {
                        // No more empty fields, deselect all
                        activeField.value = null
                    }
                } else {
                    // Field was already filled, user is modifying it - keep it active
                    setActiveField('route', index)
                }
            } else {
                callbacks.onObstacleClick(index, lng, lat)
                // Deselect obstacle field after click
                activeField.value = null
            }
            return
        }

        // Sequential auto-fill: find next empty address
        const emptyIndex = addresses.value.findIndex(addr => !addr || addr.trim() === '')

        if (emptyIndex !== -1) {
            // Fill first empty address
            callbacks.onRouteClick(emptyIndex, lng, lat)

            // Find next empty field to auto-select
            const nextEmptyIndex = addresses.value.findIndex((addr, idx) =>
                idx > emptyIndex && (!addr || addr.trim() === '')
            )

            if (nextEmptyIndex !== -1) {
                setActiveField('route', nextEmptyIndex)
            } else {
                // No more empty fields, deselect all
                activeField.value = null
            }
        } else {
            // All addresses filled, insert new waypoint before destination
            const newIndex = addresses.value.length - 1
            addresses.value.splice(newIndex, 0, '')
            // Fill the new waypoint immediately
            callbacks.onRouteClick(newIndex, lng, lat)
            // Deselect since it's now filled
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