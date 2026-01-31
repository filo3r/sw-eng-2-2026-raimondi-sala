/**
 * Composable for managing interactive draggable markers on Mapbox maps.
 * Provides reusable marker creation with custom styling, drag behavior, and lifecycle management.
 * Supports configurable colors, labels, and drag event handlers for interactive map experiences.
 */
import { type Ref, onUnmounted } from 'vue'
import mapboxgl from 'mapbox-gl'
import { createCustomMarkerElement } from '@/utils/mapMarkers'
import { useMarkerManager } from './useMarkerManager'

/**
 * Configuration for creating an interactive marker.
 */
export interface MarkerConfig {
    /** Marker background color (hex format) */
    color: string
    /** Text label displayed inside marker (typically 1-2 characters) */
    label: string
    /** Enable drag functionality for repositioning marker - default: false */
    draggable?: boolean
    /** Callback fired when marker drag completes (receives new coordinates and marker index) */
    onDragEnd?: (lng: number, lat: number, index: number) => void
}

/**
 * Composable that creates interactive marker management utilities for Mapbox maps.
 * Provides methods to create, update, and remove markers with custom styling and drag behavior.
 * Automatically cleans up all markers on component unmount.
 * @param map - Reactive reference to Mapbox map instance (can be null during initialization)
 * @returns Object containing marker creation method, removal methods, and markers array
 */
export function useInteractiveMarkers(map: Ref<mapboxgl.Map | null>) {
    const { markers, setMarker, removeMarker, clearAll, addSlot } = useMarkerManager()
    /**
     * Creates or updates a marker at the specified geographic position.
     * Replaces existing marker at same index if present. Supports custom styling and drag behavior.
     * @param index - Position in markers array (0-indexed)
     * @param lng - Longitude coordinate for marker position
     * @param lat - Latitude coordinate for marker position
     * @param config - Marker configuration with color, label, and optional drag behavior
     * @returns Created Mapbox marker instance or null if map is not available
     */
    function createMarker(
        index: number,
        lng: number,
        lat: number,
        config: MarkerConfig
    ): mapboxgl.Marker | null {
        if (!map.value) return null
        // Create custom styled marker element with configured appearance
        const el = createCustomMarkerElement({
            color: config.color,
            label: config.label,
            draggable: config.draggable
        })
        // Create Mapbox marker instance with custom element and drag settings
        const marker = new mapboxgl.Marker({
            element: el,
            draggable: config.draggable ?? false
        })
            .setLngLat([lng, lat])
            .addTo(map.value)
        // Attach drag end handler if callback provided
        if (config.onDragEnd) {
            marker.on('dragend', () => {
                const { lng, lat } = marker.getLngLat()
                config.onDragEnd!(lng, lat, index) // Pass new coordinates and index to callback
            })
        }
        // Store marker in manager (replaces existing marker at index)
        setMarker(marker, index)
        return marker
    }
    // Automatically cleanup all markers on component unmount
    onUnmounted(() => {
        clearAll()
    })
    return {
        createMarker,
        removeMarker,
        clearAll,
        addSlot,
        markers
    }
}