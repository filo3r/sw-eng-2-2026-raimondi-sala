/**
 * Composable for handling map click interactions with intelligent routing logic.
 * Supports both targeted clicks (when specific input is focused) and sequential auto-fill mode.
 * Automatically advances to next empty field or inserts waypoints when all fields are filled.
 */
import { ref } from 'vue'

/**
 * Active field tracking structure for targeted map clicks.
 */
export interface ActiveField {
    /** Field type (route address or obstacle location) */
    type: 'route' | 'obstacle'
    /** Field index in the respective array (0-indexed) */
    index: number
}

/**
 * Callback functions for handling different types of map clicks.
 */
export interface MapClickCallbacks {
    /** Called when clicking to set a route address field */
    onRouteClick: (index: number, lng: number, lat: number) => void
    /** Called when clicking to set an obstacle location field */
    onObstacleClick: (index: number, lng: number, lat: number) => void
    /** Called when adding a new waypoint between existing addresses */
    onAddWaypoint: (beforeIndex: number, lng: number, lat: number) => void
    /** Returns current array of route addresses for empty field detection */
    getCurrentAddresses: () => string[]
}

/**
 * Composable that creates map click handler with active field tracking and smart routing.
 * Manages focused field state and implements sequential filling or targeted click behavior.
 * @returns Object containing active field ref and handler methods
 */
export function useMapClickHandler() {
    /** Currently focused field for targeted clicks (null = sequential auto-fill mode) */
    const activeField = ref<ActiveField | null>(null)
    /**
     * Sets the active field when an input receives focus.
     * Enables targeted click mode where map clicks update the focused field.
     * @param type - Type of field being focused (route or obstacle)
     * @param index - Index of the focused field in its array
     */
    function setActiveField(type: 'route' | 'obstacle', index: number) {
        activeField.value = { type, index }
    }
    /**
     * Clears the active field, returning to sequential auto-fill mode.
     * Typically called on input blur events.
     */
    function clearActiveField() {
        activeField.value = null
    }
    /**
     * Handles map click with intelligent routing logic based on active field and address state.
     * Targeted mode: updates focused field and advances to next empty field.
     * Sequential mode: fills next empty address or inserts waypoint if all filled.
     * @param lng - Longitude of clicked map location
     * @param lat - Latitude of clicked map location
     * @param callbacks - Callback functions for different click actions
     */
    function handleMapClick(
        lng: number,
        lat: number,
        callbacks: MapClickCallbacks
    ) {
        const addresses = callbacks.getCurrentAddresses()
        // Targeted click mode: user focused specific input field
        if (activeField.value) {
            const { type, index } = activeField.value
            if (type === 'route') {
                // Check if field was empty before filling
                const wasEmpty = !addresses[index] || addresses[index].trim() === ''
                callbacks.onRouteClick(index, lng, lat)
                // If field was empty, auto-advance to next empty field
                if (wasEmpty) {
                    const nextEmptyIndex = addresses.findIndex((addr, idx) =>
                        idx > index && (!addr || addr.trim() === '')
                    )
                    if (nextEmptyIndex !== -1) {
                        setActiveField('route', nextEmptyIndex) // Focus next empty field
                    } else {
                        activeField.value = null // All fields filled, exit targeted mode
                    }
                } else {
                    setActiveField('route', index) // Keep focus on same field if it was filled
                }
            } else {
                // Obstacle field: fill and clear active field
                callbacks.onObstacleClick(index, lng, lat)
                activeField.value = null
            }
            return
        }
        // Sequential auto-fill mode: find and fill next empty address
        const emptyIndex = addresses.findIndex(addr => !addr || addr.trim() === '')
        if (emptyIndex !== -1) {
            // Fill next empty address field
            callbacks.onRouteClick(emptyIndex, lng, lat)
            // Auto-advance focus to next empty field
            const nextEmptyIndex = addresses.findIndex((addr, idx) =>
                idx > emptyIndex && (!addr || addr.trim() === '')
            )
            if (nextEmptyIndex !== -1) {
                setActiveField('route', nextEmptyIndex) // Focus next empty field
            } else {
                activeField.value = null // All fields filled
            }
        } else {
            // All addresses filled: insert new waypoint before destination
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