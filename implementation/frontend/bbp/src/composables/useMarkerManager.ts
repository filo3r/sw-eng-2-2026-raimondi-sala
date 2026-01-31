/**
 * Base composable for managing Mapbox marker collections.
 * Provides common CRUD operations for marker arrays including add, remove, update, and clear.
 * Uses shallowRef for performance optimization with large marker collections.
 */
import { shallowRef } from 'vue'

/**
 * Composable that creates marker collection management utilities.
 * Manages a reactive array of Mapbox markers with methods for safe manipulation.
 * @returns Object containing reactive markers array and management methods
 */
export function useMarkerManager() {
    /** Reactive array of Mapbox marker instances (uses shallow reactivity for performance) */
    const markers = shallowRef<any[]>([])
    /**
     * Adds or updates a marker at the specified index.
     * Removes existing marker at that position before setting new one.
     * @param marker - Mapbox marker instance to add/update
     * @param index - Position in markers array (0-indexed)
     */
    function setMarker(marker: any, index: number) {
        // Remove old marker from map if it exists at this index
        if (markers.value[index]) {
            markers.value[index]?.remove()
        }
        markers.value[index] = marker
    }
    /**
     * Removes marker at the specified index and sets slot to null.
     * Safely handles removal from map before nullifying array slot.
     * @param index - Index of marker to remove (0-indexed)
     */
    function removeMarker(index: number) {
        if (markers.value[index]) {
            markers.value[index]?.remove() // Remove from map
            markers.value[index] = null // Clear array slot
        }
    }
    /**
     * Removes all markers from map and clears the array.
     * Iterates through all markers and removes them from map before resetting array.
     */
    function clearAll() {
        markers.value.forEach(m => m?.remove()) // Remove all markers from map
        markers.value = [] // Reset array to empty
    }
    /**
     * Adds an empty slot (null) to the markers array.
     * Used for pre-allocating slots before markers are created.
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