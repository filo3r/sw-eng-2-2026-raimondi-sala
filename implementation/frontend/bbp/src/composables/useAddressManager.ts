/**
 * Composable for managing route addresses and their associated markers.
 * Centralizes add, remove, reorder, and redraw operations.
 */
import { type Ref, nextTick } from 'vue'
import type * as mapboxgl from 'mapbox-gl'
import { getAddressFromCoordinates } from '@/utils/geocoding'

export interface UseAddressManagerOptions {
    addresses: Ref<string[]>
    routeMarkers: Ref<(mapboxgl.Marker | null)[]>
    activeField: Ref<{ type: string; index: number } | null>
    setActiveField: (type: string, index: number) => void
    removeRouteMarker: (index: number) => void
    setMarker: (index: number, lng: number, lat: number) => void
    updateRoute: (
        markers: (mapboxgl.Marker | null)[],
        debounceMs: number,
        context: string
    ) => void
    debounceMs: number
    context: string
}

/**
 * Creates address management utilities.
 * @param options - Configuration options
 * @returns Methods to manage addresses
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
     * Handles both extending the array and inserting slots for waypoints.
     * @param index - Target index that needs a marker slot
     */
    function ensureMarkerSlot(index: number) {
        // Add missing slots up to index
        while (routeMarkers.value.length <= index) {
            routeMarkers.value.push(null)
        }

        // If inserting a waypoint in the middle, create space
        if (routeMarkers.value.length === addresses.value.length - 1 &&
            index < routeMarkers.value.length) {
            routeMarkers.value.splice(index, 0, null)
        }
    }

    /**
     * Handles click on a route point (origin, destination, or waypoint).
     * Updates address, syncs markers, and redraws.
     * @param index - Index of the route point
     * @param lng - Longitude
     * @param lat - Latitude
     */
    async function handleRoutePointClick(index: number, lng: number, lat: number) {
        addresses.value[index] = await getAddressFromCoordinates(lng, lat, context)
        ensureMarkerSlot(index)
        setMarker(index, lng, lat)
        redrawRouteMarkers()
    }

    /**
     * Handles insertion of a new waypoint at a specific position.
     * @param beforeIndex - Insert before this index
     * @param lng - Longitude
     * @param lat - Latitude
     */
    async function handleWaypointInsert(beforeIndex: number, lng: number, lat: number) {
        addresses.value.splice(beforeIndex, 0, await getAddressFromCoordinates(lng, lat, context))
        routeMarkers.value.splice(beforeIndex, 0, null)
        setMarker(beforeIndex, lng, lat)
        redrawRouteMarkers()
    }

    /**
     * Adds a new waypoint before the destination.
     */
    async function addAddress() {
        // Insert new waypoint before destination
        const insertIndex = addresses.value.length - 1
        addresses.value.splice(insertIndex, 0, '')

        // Insert marker slot at same position
        routeMarkers.value.splice(insertIndex, 0, null)

        setActiveField('route', insertIndex)

        await nextTick()

        // Focus on new waypoint input
        const inputs = document.querySelectorAll('input[type="text"]')
        const input = inputs[insertIndex] as HTMLInputElement
        input?.focus()

        redrawRouteMarkers()
    }

    /**
     * Removes an address and its marker.
     * @param index - Index of address to remove
     */
    function removeAddress(index: number) {
        if (addresses.value.length > 2) {
            removeRouteMarker(index)
            addresses.value.splice(index, 1)
            routeMarkers.value.splice(index, 1)

            if (activeField.value?.type === 'route' && activeField.value.index === index) {
                activeField.value = null
            }

            redrawRouteMarkers()
            void updateRoute(routeMarkers.value, debounceMs, context)
        }
    }

    /**
     * Reorders addresses via drag and drop.
     * @param fromIndex - Source index
     * @param toIndex - Destination index
     */
    function reorderAddresses(fromIndex: number, toIndex: number) {
        const [movedAddress = ''] = addresses.value.splice(fromIndex, 1)
        addresses.value.splice(toIndex, 0, movedAddress)

        const [movedMarker = null] = routeMarkers.value.splice(fromIndex, 1)
        routeMarkers.value.splice(toIndex, 0, movedMarker)

        redrawRouteMarkers()
        void updateRoute(routeMarkers.value, debounceMs, context)
    }

    /**
     * Redraws all route markers with updated configurations.
     */
    function redrawRouteMarkers() {
        routeMarkers.value.forEach((marker, index) => {
            if (marker) {
                const { lng, lat } = marker.getLngLat()
                setMarker(index, lng, lat)
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