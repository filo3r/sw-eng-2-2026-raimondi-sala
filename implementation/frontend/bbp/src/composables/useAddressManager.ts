/**
 * Composable for managing route addresses and their associated markers.
 * Centralizes add, remove, reorder, and redraw operations.
 */
import { type Ref, nextTick } from 'vue'
import type * as mapboxgl from 'mapbox-gl'

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
        redrawRouteMarkers
    }
}