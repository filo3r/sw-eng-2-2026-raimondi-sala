/**
 * Base composable for managing Mapbox marker collections.
 * Provides common operations for adding, removing, and clearing markers.
 */
import { shallowRef } from 'vue'

/**
 * Creates marker collection management utilities.
 * @returns Methods to manage marker array
 */
export function useMarkerManager() {
    const markers = shallowRef<any[]>([])

    /**
     * Adds or updates a marker at specified index.
     * @param marker - Mapbox marker instance
     * @param index - Position in array
     */
    function setMarker(marker: any, index: number) {
        // Remove old marker if exists
        if (markers.value[index]) {
            markers.value[index]?.remove()
        }
        markers.value[index] = marker
    }

    /**
     * Removes marker at specified index.
     * @param index - Marker index to remove
     */
    function removeMarker(index: number) {
        if (markers.value[index]) {
            markers.value[index]?.remove()
            markers.value[index] = null
        }
    }

    /**
     * Removes all markers and clears array.
     */
    function clearAll() {
        markers.value.forEach(m => m?.remove())
        markers.value = []
    }

    /**
     * Adds empty slot to markers array.
     */
    function addSlot() {
        markers.value.push(null)
    }

    return {
        markers,
        setMarker,
        removeMarker,
        clearAll,
        addSlot
    }
}