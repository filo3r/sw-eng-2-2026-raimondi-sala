/**
 * Composable for managing obstacle markers on Mapbox map.
 * Handles rendering and cleanup of active obstacle markers with severity-based styling.
 */
import { type Ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import { createCustomMarkerElement } from '@/utils/mapMarkers'
import { useMarkerManager } from './useMarkerManager'
import {
    OBSTACLE_SEVERITY_COLORS,
    DEFAULT_OBSTACLE_COLOR,
    POPUP_OFFSET,
    OBSTACLE_POPUP_CLASS,
    POPUP_CONTENT_PADDING,
    POPUP_MIN_WIDTH
} from '@/constants/map'
import type { ObstacleResponse } from '@/types/obstacle'

/**
 * Creates obstacle marker management utilities for Mapbox maps.
 * @param map - Reactive reference to Mapbox map instance
 * @returns Methods to add and clear obstacle markers
 */
export function useMapObstacles(map: Ref<mapboxgl.Map | null>) {
    const { markers: obstacleMarkers, clearAll } = useMarkerManager()

    /**
     * Adds obstacle markers to map with severity-based colors.
     * Only displays active obstacles.
     * @param obstacles - Array of obstacle data
     */
    function addObstacles(obstacles: ObstacleResponse[]) {
        if (!map.value) return

        const mapInstance = map.value
        clearObstacles()

        // Filter only active obstacles
        const activeObstacles = obstacles.filter(obstacle => obstacle.active)
        const currentMarkers: mapboxgl.Marker[] = []

        activeObstacles.forEach(obstacle => {
            const markerColor = OBSTACLE_SEVERITY_COLORS[obstacle.severity] || DEFAULT_OBSTACLE_COLOR

            // Create custom obstacle marker with exclamation mark
            const el = createCustomMarkerElement({
                color: markerColor,
                label: '!',
                draggable: false
            })

            // Create info popup
            const popup = new mapboxgl.Popup({
                offset: POPUP_OFFSET,
                className: OBSTACLE_POPUP_CLASS
            }).setHTML(`
        <div style="padding: ${POPUP_CONTENT_PADDING}px; min-width: ${POPUP_MIN_WIDTH}px;">
          <h3 style="font-size: 16px; font-weight: 600; margin: 0 0 12px 0; color: #1f2937;">
            ${obstacle.typeDescription}
          </h3>
          <div style="margin-bottom: 8px;">
            <span style="font-size: 14px; font-weight: 500; color: #374151;">Location:</span>
            <p style="font-size: 14px; color: #6b7280; margin: 4px 0 0 0;">${obstacle.address}</p>
          </div>
          <div>
            <span style="font-size: 14px; font-weight: 500; color: #374151;">Severity:</span>
            <span style="font-size: 14px; color: #6b7280; font-weight: 600; margin-left: 4px;">
              ${obstacle.severityDescription}
            </span>
          </div>
        </div>
      `)

            // Add marker to map
            const marker = new mapboxgl.Marker(el)
                .setLngLat([obstacle.longitude, obstacle.latitude])
                .setPopup(popup)
                .addTo(mapInstance)

            currentMarkers.push(marker)
        })

        obstacleMarkers.value = currentMarkers
    }

    /**
     * Removes all obstacle markers from map.
     */
    function clearObstacles() {
        clearAll()
    }

    return {
        addObstacles,
        clearObstacles
    }
}