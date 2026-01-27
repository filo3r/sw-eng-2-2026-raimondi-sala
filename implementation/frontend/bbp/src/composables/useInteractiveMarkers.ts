/**
 * Composable for managing interactive draggable markers on Mapbox maps.
 * Provides reusable marker creation with custom styling and drag behavior.
 */
import { shallowRef, type Ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import { createCustomMarkerElement } from '@/utils/mapMarkers'

export interface MarkerConfig {
    color: string
    label: string
    draggable?: boolean
    onDragEnd?: (lng: number, lat: number, index: number) => void
}

/**
 * Creates interactive marker management utilities for Mapbox maps.
 * @param map - Reactive reference to Mapbox map instance
 * @returns Methods to create, remove, and manage markers
 */
export function useInteractiveMarkers(map: Ref<mapboxgl.Map | null>) {
    const markers = shallowRef<(mapboxgl.Marker | null)[]>([])

    /**
     * Creates or updates a marker at specified position.
     * @param index - Marker index in the array
     * @param lng - Longitude coordinate
     * @param lat - Latitude coordinate
     * @param config - Marker configuration (color, label, drag behavior)
     * @returns Created marker instance or null if map not available
     */
    function createMarker(
        index: number,
        lng: number,
        lat: number,
        config: MarkerConfig
    ): mapboxgl.Marker | null {
        if (!map.value) return null

        // Remove old marker if exists
        if (markers.value[index]) {
            markers.value[index]?.remove()
        }

        // Create custom marker element
        const el = createCustomMarkerElement({
            color: config.color,
            label: config.label,
            draggable: config.draggable
        })

        // Create Mapbox marker
        const marker = new mapboxgl.Marker({
            element: el,
            draggable: config.draggable ?? false
        })
            .setLngLat([lng, lat])
            .addTo(map.value)

        // Attach drag end handler if provided
        if (config.onDragEnd) {
            marker.on('dragend', () => {
                const { lng, lat } = marker.getLngLat()
                config.onDragEnd!(lng, lat, index)
            })
        }

        markers.value[index] = marker
        return marker
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
     * Removes all markers from map and clears array.
     */
    function clearAll() {
        markers.value.forEach(m => m?.remove())
        markers.value = []
    }

    /**
     * Adds empty slot to markers array.
     * Used when adding new address/obstacle fields.
     */
    function addSlot() {
        markers.value.push(null)
    }

    return {
        createMarker,
        removeMarker,
        clearAll,
        addSlot,
        markers
    }
}