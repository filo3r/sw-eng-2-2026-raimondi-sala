/**
 * Composable for managing obstacle markers on Mapbox map.
 * Handles rendering of active obstacles with severity-based color styling and popup information.
 * Only displays active obstacles; inactive obstacles are filtered out for cleaner map view.
 */
import { type Ref, onUnmounted } from 'vue'
import mapboxgl from 'mapbox-gl'
import { createCustomMarkerElement } from '@/utils/mapMarkers'
import { createObstaclePopupHTML } from '@/utils/mapPopups'
import { useMarkerManager } from './useMarkerManager'
import {
    OBSTACLE_SEVERITY_COLORS,
    DEFAULT_OBSTACLE_COLOR,
    POPUP_OFFSET,
    OBSTACLE_POPUP_CLASS
} from '@/constants/map'
import type { ObstacleResponse } from '@/types/obstacle'

/**
 * Composable that creates obstacle marker management utilities for Mapbox maps.
 * Provides methods to add severity-colored obstacle markers and clear them from map.
 * Automatically cleans up markers on component unmount.
 * @param map - Reactive reference to Mapbox map instance (can be null during initialization)
 * @returns Object containing methods to add and clear obstacle markers
 */
export function useMapObstacles(map: Ref<mapboxgl.Map | null>) {
    const { markers: obstacleMarkers, clearAll } = useMarkerManager()
    /**
     * Adds obstacle markers to map with severity-based color coding.
     * Filters out inactive obstacles and creates circular markers with exclamation mark labels.
     * Each marker has a popup showing obstacle details (type, location, severity).
     * @param obstacles - Array of obstacle data including active/inactive status and severity
     */
    function addObstacles(obstacles: ObstacleResponse[]) {
        if (!map.value) return
        const mapInstance = map.value
        clearObstacles() // Remove any existing obstacle markers
        // Filter to show only active obstacles on map
        const activeObstacles = obstacles.filter(obstacle => obstacle.active)
        const currentMarkers: mapboxgl.Marker[] = []
        activeObstacles.forEach(obstacle => {
            // Get severity-based color (LOW=yellow, MEDIUM=orange, HIGH=red, CRITICAL=dark red)
            const markerColor = OBSTACLE_SEVERITY_COLORS[obstacle.severity] || DEFAULT_OBSTACLE_COLOR
            // Create custom circular marker with exclamation mark
            const el = createCustomMarkerElement({
                color: markerColor,
                label: '!',
                draggable: false
            })
            // Create info popup with obstacle details
            const popup = new mapboxgl.Popup({
                offset: POPUP_OFFSET,
                className: OBSTACLE_POPUP_CLASS
            }).setHTML(createObstaclePopupHTML(obstacle))
            // Add marker to map at obstacle coordinates
            const marker = new mapboxgl.Marker(el)
                .setLngLat([obstacle.longitude, obstacle.latitude])
                .setPopup(popup)
                .addTo(mapInstance)
            currentMarkers.push(marker)
        })
        obstacleMarkers.value = currentMarkers
    }
    /**
     * Removes all obstacle markers from the map.
     * Cleans up marker instances and clears internal array.
     */
    function clearObstacles() {
        clearAll()
    }
    // Cleanup obstacle markers on component unmount
    onUnmounted(() => {
        clearObstacles()
    })
    return {
        addObstacles,
        clearObstacles
    }
}