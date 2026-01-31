/**
 * Composable for managing route addresses and their associated Mapbox markers.
 * Centralizes address and marker operations including add, remove, reorder, and redraw with route updates.
 * Handles marker slot synchronization and maintains consistency between addresses and markers arrays.
 */
import { type Ref, nextTick } from 'vue'
import type * as mapboxgl from 'mapbox-gl'
import { getAddressFromCoordinates } from '@/utils/geocoding'

/**
 * Configuration options for address manager.
 * Provides refs and callbacks needed for address and marker management.
 */
export interface UseAddressManagerOptions {
    /** Reactive array of route addresses (origin, waypoints, destination) */
    addresses: Ref<string[]>
    /** Reactive array of Mapbox markers corresponding to addresses */
    routeMarkers: Ref<(mapboxgl.Marker | null)[]>
    /** Currently focused field for targeted map clicks */
    activeField: Ref<{ type: string; index: number } | null>
    /** Callback to set active field when input receives focus */
    setActiveField: (type: string, index: number) => void
    /** Callback to remove marker at specific index */
    removeRouteMarker: (index: number) => void
    /** Callback to create/update marker at coordinates */
    setMarker: (index: number, lng: number, lat: number) => void
    /** Callback to recalculate and redraw route line */
    updateRoute: (
        markers: (mapboxgl.Marker | null)[],
        debounceMs: number,
        context: string
    ) => void
    /** Debounce delay for route updates in milliseconds */
    debounceMs: number
    /** Context identifier for error logging */
    context: string
}

/**
 * Composable that creates address management utilities for route planning.
 * Synchronizes addresses array with markers, handles geocoding, and manages route updates.
 * @param options - Configuration with refs and callbacks for address/marker operations
 * @returns Object containing methods to manage addresses and markers
 */
export function useAddressManager(options: UseAddressManagerOptions) {
    const {
        addresses,
        routeMarkers,
        activeField,
        setActiveField,
        removeRouteMarker,
        setMarker,
        updateRoute,
        debounceMs,
        context
    } = options
    /**
     * Ensures marker slots exist up to the specified index.
     * Extends array or inserts slots to maintain synchronization with addresses.
     * @param index - Target index that requires a marker slot
     */
    function ensureMarkerSlot(index: number) {
        // Extend array with null slots if index is beyond current length
        while (routeMarkers.value.length <= index) {
            routeMarkers.value.push(null)
        }
        // If inserting waypoint in middle, create space by inserting null slot
        if (routeMarkers.value.length === addresses.value.length - 1 &&
            index < routeMarkers.value.length) {
            routeMarkers.value.splice(index, 0, null)
        }
    }
    /**
     * Handles map click on route point (origin, waypoint, or destination).
     * Geocodes coordinates to address, updates arrays, and redraws markers with new styling.
     * @param index - Index of the route point being updated (0-indexed)
     * @param lng - Longitude coordinate from map click
     * @param lat - Latitude coordinate from map click
     */
    async function handleRoutePointClick(index: number, lng: number, lat: number) {
        // Reverse geocode coordinates to address and update array
        addresses.value[index] = await getAddressFromCoordinates(lng, lat, context)
        ensureMarkerSlot(index) // Ensure marker slot exists
        setMarker(index, lng, lat) // Create/update marker at coordinates
        redrawRouteMarkers() // Redraw all markers with updated configurations
    }
    /**
     * Handles insertion of a new waypoint at specific position in the route.
     * Geocodes coordinates, inserts into arrays, and redraws markers.
     * @param beforeIndex - Insert new waypoint before this index
     * @param lng - Longitude coordinate for new waypoint
     * @param lat - Latitude coordinate for new waypoint
     */
    async function handleWaypointInsert(beforeIndex: number, lng: number, lat: number) {
        // Reverse geocode and insert address at specified position
        addresses.value.splice(beforeIndex, 0, await getAddressFromCoordinates(lng, lat, context))
        routeMarkers.value.splice(beforeIndex, 0, null) // Insert null marker slot
        setMarker(beforeIndex, lng, lat) // Create marker at new position
        redrawRouteMarkers() // Redraw all markers with updated indices
    }
    /**
     * Adds a new empty waypoint before the destination.
     * Inserts empty address, creates marker slot, and focuses new input field.
     */
    async function addAddress() {
        // Insert new waypoint before destination (last element)
        const insertIndex = addresses.value.length - 1
        addresses.value.splice(insertIndex, 0, '')
        // Insert corresponding null marker slot
        routeMarkers.value.splice(insertIndex, 0, null)
        // Set focus to new waypoint for immediate input
        setActiveField('route', insertIndex)
        await nextTick() // Wait for DOM update
        // Focus on new waypoint input field
        const inputs = document.querySelectorAll('input[type="text"]')
        const input = inputs[insertIndex] as HTMLInputElement
        input?.focus()
        redrawRouteMarkers() // Update marker configurations for new indices
    }
    /**
     * Removes an address and its associated marker from the route.
     * Prevents removal if only origin and destination remain (minimum 2 addresses).
     * @param index - Index of address to remove (0-indexed)
     */
    function removeAddress(index: number) {
        // Only allow removal if more than 2 addresses (minimum: origin + destination)
        if (addresses.value.length > 2) {
            removeRouteMarker(index) // Remove marker from map
            addresses.value.splice(index, 1) // Remove address from array
            routeMarkers.value.splice(index, 1) // Remove marker slot from array
            // Clear active field if removing the focused field
            if (activeField.value?.type === 'route' && activeField.value.index === index) {
                activeField.value = null
            }
            redrawRouteMarkers() // Update remaining markers with new indices
            void updateRoute(routeMarkers.value, debounceMs, context) // Recalculate route
        }
    }
    /**
     * Reorders addresses and markers via drag and drop operation.
     * Moves address and marker from source to target index and updates route.
     * @param fromIndex - Source index being dragged (0-indexed)
     * @param toIndex - Destination index for drop (0-indexed)
     */
    function reorderAddresses(fromIndex: number, toIndex: number) {
        // Move address from source to target index
        const [movedAddress = ''] = addresses.value.splice(fromIndex, 1)
        addresses.value.splice(toIndex, 0, movedAddress)
        // Move corresponding marker to match address order
        const [movedMarker = null] = routeMarkers.value.splice(fromIndex, 1)
        routeMarkers.value.splice(toIndex, 0, movedMarker)
        redrawRouteMarkers() // Update all marker styles with new positions
        void updateRoute(routeMarkers.value, debounceMs, context) // Recalculate route
    }
    /**
     * Redraws all route markers with updated configurations.
     * Updates marker colors and labels based on current positions (origin, waypoints, destination).
     */
    function redrawRouteMarkers() {
        // Iterate through all markers and update their configurations
        routeMarkers.value.forEach((marker, index) => {
            if (marker) {
                const { lng, lat } = marker.getLngLat()
                setMarker(index, lng, lat) // Recreate marker with updated config
            }
        })
    }
    return {
        addAddress,
        removeAddress,
        reorderAddresses,
        redrawRouteMarkers,
        ensureMarkerSlot,
        handleRoutePointClick,
        handleWaypointInsert
    }
}