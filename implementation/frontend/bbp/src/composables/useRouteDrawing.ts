/**
 * Composable for managing interactive route drawing on Mapbox maps.
 * Handles route source/layer initialization, route updates with debouncing, and lifecycle cleanup.
 * Calculates cycling routes through Mapbox Directions API and renders them as LineString features.
 */
import { type Ref, watch, onUnmounted } from 'vue'
import type * as mapboxgl from 'mapbox-gl'
import { calculateCyclingRoute } from '@/services/mapbox'
import { catchApiError } from '@/utils/error'
import {
    ROUTE_LINE_COLOR,
    ROUTE_LINE_WIDTH,
    ROUTE_LINE_JOIN,
    ROUTE_LINE_CAP
} from '@/constants/map'
import type { Coordinate } from '@/types/mapbox'

/**
 * Configuration options for route drawing setup.
 */
export interface UseRouteDrawingOptions {
    /** GeoJSON source ID for route data */
    sourceId: string
    /** Layer ID for route line rendering */
    layerId: string
}

/**
 * Composable that provides route drawing utilities for Mapbox maps with debounced updates.
 * Automatically initializes route source/layer, handles route calculation, and cleans up on unmount.
 * @param map - Reactive reference to Mapbox map instance (can be null during initialization)
 * @param options - Configuration with source and layer IDs for the route
 * @returns Object containing methods to update, clear route, and manage click handlers
 */
export function useRouteDrawing(
    map: Ref<mapboxgl.Map | null>,
    options: UseRouteDrawingOptions
) {
    const { sourceId, layerId } = options
    let debounceTimer: ReturnType<typeof setTimeout> | null = null
    let mapClickHandler: ((e: mapboxgl.MapMouseEvent) => void) | null = null
    /**
     * Initializes route GeoJSON source and line layer on the map.
     * Only creates source/layer if they don't already exist.
     * @param mapInstance - Mapbox map instance to add source and layer to
     */
    function initializeRouteLayer(mapInstance: mapboxgl.Map) {
        if (!mapInstance.getSource(sourceId)) {
            // Add empty GeoJSON source for route data
            mapInstance.addSource(sourceId, {
                type: 'geojson',
                data: { type: 'FeatureCollection', features: [] }
            })
            // Add line layer with styling from constants
            mapInstance.addLayer({
                id: layerId,
                type: 'line',
                source: sourceId,
                layout: {
                    'line-join': ROUTE_LINE_JOIN,
                    'line-cap': ROUTE_LINE_CAP
                },
                paint: {
                    'line-color': ROUTE_LINE_COLOR,
                    'line-width': ROUTE_LINE_WIDTH
                }
            })
        }
    }
    /**
     * Updates route on map with debouncing to prevent excessive API calls during marker dragging.
     * Calculates cycling route through all marker positions and renders as LineString.
     * @param markers - Array of Mapbox markers defining route waypoints (nulls are filtered out)
     * @param debounceMs - Debounce delay in milliseconds (default: 300)
     * @param context - Context identifier for error logging (default: 'useRouteDrawing')
     * @returns Promise that resolves when route update is complete
     */
    async function updateRoute(
        markers: (mapboxgl.Marker | null)[],
        debounceMs = 300,
        context = 'useRouteDrawing'
    ) {
        if (!map.value) return
        // Clear existing debounce timer to reset delay
        if (debounceTimer) {
            clearTimeout(debounceTimer)
        }
        // Debounce the update to avoid rapid successive API calls
        return new Promise<void>((resolve) => {
            debounceTimer = setTimeout(async () => {
                await performRouteUpdate(markers, context)
                resolve()
            }, debounceMs)
        })
    }
    /**
     * Performs the actual route calculation and map update without debouncing.
     * Filters out null markers, calculates cycling route, and updates GeoJSON source.
     * @param markers - Array of Mapbox markers defining route waypoints
     * @param context - Context identifier for error logging (default: 'useRouteDrawing')
     */
    async function performRouteUpdate(
        markers: (mapboxgl.Marker | null)[],
        context = 'useRouteDrawing'
    ) {
        if (!map.value) return
        // Extract coordinates from valid markers
        const coordinates = markers
            .filter((m): m is mapboxgl.Marker => m !== null)
            .map(m => m.getLngLat())
        // Clear route if less than 2 waypoints (minimum required for route)
        if (coordinates.length < 2) {
            const source = map.value.getSource(sourceId) as mapboxgl.GeoJSONSource | undefined
            if (source) {
                source.setData({ type: 'FeatureCollection', features: [] })
            }
            return
        }
        try {
            // Convert to Coordinate format for API request
            const waypoints: Coordinate[] = coordinates.map(c => ({
                latitude: c.lat,
                longitude: c.lng
            }))
            // Calculate cycling route through Mapbox Directions API
            const routeResult = await calculateCyclingRoute({ waypoints })
            // Sort points by sequential position and extract coordinates
            const routeCoordinates = routeResult.points
                .sort((a, b) => a.sequentialPosition - b.sequentialPosition)
                .map(point => [point.longitude, point.latitude])
            // Update GeoJSON source with calculated route LineString
            const source = map.value.getSource(sourceId) as mapboxgl.GeoJSONSource | undefined
            if (source) {
                source.setData({
                    type: 'Feature',
                    properties: {},
                    geometry: {
                        type: 'LineString',
                        coordinates: routeCoordinates
                    }
                })
            }
        } catch (e) {
            catchApiError(e, `${context}.updateRoute`)
        }
    }
    /**
     * Clears the route from the map by setting empty GeoJSON data.
     * Removes all route line features from the map display.
     */
    function clearRoute() {
        if (!map.value) return
        const source = map.value.getSource(sourceId) as mapboxgl.GeoJSONSource | undefined
        if (source) {
            source.setData({ type: 'FeatureCollection', features: [] })
        }
    }
    /**
     * Attaches a click handler to the map for interactive coordinate selection.
     * Stores handler reference for cleanup on unmount.
     * @param handler - Click event handler function to attach
     */
    function attachMapClickHandler(handler: (e: mapboxgl.MapMouseEvent) => void) {
        if (map.value) {
            map.value.on('click', handler)
            mapClickHandler = handler
        }
    }
    /**
     * Detaches the previously attached map click handler.
     * Cleans up event listener and reference.
     */
    function detachMapClickHandler() {
        if (map.value && mapClickHandler) {
            map.value.off('click', mapClickHandler)
            mapClickHandler = null
        }
    }
    // Watch for map instance changes and initialize route layer
    watch(map, (newMap, oldMap) => {
        // Cleanup old map event listeners
        if (oldMap && mapClickHandler) {
            oldMap.off('click', mapClickHandler)
        }
        // Setup new map with route layer on load event
        if (newMap) {
            newMap.on('load', () => {
                initializeRouteLayer(newMap)
            })
        }
    })
    // Cleanup on component unmount
    onUnmounted(() => {
        // Clear any pending debounce timer
        if (debounceTimer) {
            clearTimeout(debounceTimer)
        }
        // Remove map click handler
        detachMapClickHandler()
    })
    return {
        updateRoute,
        clearRoute,
        attachMapClickHandler
    }
}