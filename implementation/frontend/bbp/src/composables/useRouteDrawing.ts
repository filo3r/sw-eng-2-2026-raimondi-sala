/**
 * Composable for managing interactive route drawing on Mapbox maps.
 * Handles route source/layer setup, route updates with debouncing, and cleanup.
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

export interface UseRouteDrawingOptions {
    sourceId: string
    layerId: string
}

/**
 * Creates route drawing utilities for Mapbox maps with debouncing.
 * @param map - Reactive reference to Mapbox map instance
 * @param options - Source and layer IDs for the route
 * @returns Methods to update and clear the route
 */
export function useRouteDrawing(
    map: Ref<mapboxgl.Map | null>,
    options: UseRouteDrawingOptions
) {
    const { sourceId, layerId } = options
    let debounceTimer: ReturnType<typeof setTimeout> | null = null
    let mapClickHandler: ((e: mapboxgl.MapMouseEvent) => void) | null = null

    /**
     * Initializes route source and layer on map load.
     */
    function initializeRouteLayer(mapInstance: mapboxgl.Map) {
        if (!mapInstance.getSource(sourceId)) {
            mapInstance.addSource(sourceId, {
                type: 'geojson',
                data: { type: 'FeatureCollection', features: [] }
            })

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
     * Updates route on map with debouncing to prevent excessive API calls.
     * @param markers - Array of markers defining the route
     * @param debounceMs - Debounce delay in milliseconds (default: 300)
     * @param context - Context string for error logging
     */
    async function updateRoute(
        markers: (mapboxgl.Marker | null)[],
        debounceMs = 300,
        context = 'useRouteDrawing'
    ) {
        if (!map.value) return

        // Clear existing debounce timer
        if (debounceTimer) {
            clearTimeout(debounceTimer)
        }

        // Debounce the update
        return new Promise<void>((resolve) => {
            debounceTimer = setTimeout(async () => {
                await performRouteUpdate(markers, context)
                resolve()
            }, debounceMs)
        })
    }

    /**
     * Performs the actual route update without debouncing.
     * @param markers - Array of markers defining the route
     * @param context - Context string for error logging
     */
    async function performRouteUpdate(
        markers: (mapboxgl.Marker | null)[],
        context = 'useRouteDrawing'
    ) {
        if (!map.value) return

        const coordinates = markers
            .filter((m): m is mapboxgl.Marker => m !== null)
            .map(m => m.getLngLat())

        if (coordinates.length < 2) {
            const source = map.value.getSource(sourceId) as mapboxgl.GeoJSONSource | undefined
            if (source) {
                source.setData({ type: 'FeatureCollection', features: [] })
            }
            return
        }

        try {
            const waypoints: Coordinate[] = coordinates.map(c => ({
                latitude: c.lat,
                longitude: c.lng
            }))

            const routeResult = await calculateCyclingRoute({ waypoints })

            const routeCoordinates = routeResult.points
                .sort((a, b) => a.sequentialPosition - b.sequentialPosition)
                .map(point => [point.longitude, point.latitude])

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
     * Clears the route from the map.
     */
    function clearRoute() {
        if (!map.value) return

        const source = map.value.getSource(sourceId) as mapboxgl.GeoJSONSource | undefined
        if (source) {
            source.setData({ type: 'FeatureCollection', features: [] })
        }
    }

    /**
     * Attaches a map click handler.
     * @param handler - Click event handler
     */
    function attachMapClickHandler(handler: (e: mapboxgl.MapMouseEvent) => void) {
        if (map.value) {
            map.value.on('click', handler)
            mapClickHandler = handler
        }
    }

    /**
     * Detaches the map click handler.
     */
    function detachMapClickHandler() {
        if (map.value && mapClickHandler) {
            map.value.off('click', mapClickHandler)
            mapClickHandler = null
        }
    }

    // Watch for map changes and initialize
    watch(map, (newMap, oldMap) => {
        // Cleanup old map
        if (oldMap && mapClickHandler) {
            oldMap.off('click', mapClickHandler)
        }

        // Setup new map
        if (newMap) {
            newMap.on('load', () => {
                initializeRouteLayer(newMap)
            })
        }
    })

    // Cleanup on unmount
    onUnmounted(() => {
        if (debounceTimer) {
            clearTimeout(debounceTimer)
        }
        detachMapClickHandler()
    })

    return {
        updateRoute,
        clearRoute,
        attachMapClickHandler,
        detachMapClickHandler
    }
}