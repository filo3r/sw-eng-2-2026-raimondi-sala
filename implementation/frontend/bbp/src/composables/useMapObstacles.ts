/**
 * Composable for managing obstacle markers on Mapbox map.
 */

import { ref, type Ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import {
    OBSTACLE_SEVERITY_COLORS,
    DEFAULT_OBSTACLE_COLOR,
    OBSTACLE_MARKER_SIZE,
    OBSTACLE_MARKER_BORDER_WIDTH,
    OBSTACLE_MARKER_BORDER_COLOR,
    OBSTACLE_MARKER_BOX_SHADOW,
    POPUP_OFFSET
} from '@/constants/map'
import type { ObstacleResponse } from '@/types/obstacle'

export function useMapObstacles(map: Ref<mapboxgl.Map | null>) {
    const obstacleMarkers = ref<mapboxgl.Marker[]>([])

    /**
     * Adds obstacle markers to map.
     */
    function addObstacles(obstacles: ObstacleResponse[]) {
        if (!map.value) return

        const mapInstance = map.value

        // Clear existing obstacles
        clearObstacles()

        // Filter only active obstacles
        const activeObstacles = obstacles.filter(obstacle => obstacle.active)
        const currentMarkers: mapboxgl.Marker[] = []

        activeObstacles.forEach(obstacle => {
            const markerColor = OBSTACLE_SEVERITY_COLORS[obstacle.severity] || DEFAULT_OBSTACLE_COLOR

            // Create custom marker element
            const el = document.createElement('div')
            el.className = 'obstacle-marker'
            el.style.backgroundColor = markerColor
            el.style.width = `${OBSTACLE_MARKER_SIZE}px`
            el.style.height = `${OBSTACLE_MARKER_SIZE}px`
            el.style.borderRadius = '50%'
            el.style.border = `${OBSTACLE_MARKER_BORDER_WIDTH}px solid ${OBSTACLE_MARKER_BORDER_COLOR}`
            el.style.cursor = 'pointer'
            el.style.boxShadow = OBSTACLE_MARKER_BOX_SHADOW

            // Create popup
            const popup = new mapboxgl.Popup({
                offset: POPUP_OFFSET,
                className: 'obstacle-popup'
            }).setHTML(`
        <div style="padding: 12px; min-width: 200px;">
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

            // Create and add marker
            const marker = new mapboxgl.Marker(el)
                .setLngLat([obstacle.longitude, obstacle.latitude])
                .setPopup(popup)
                .addTo(mapInstance)

            currentMarkers.push(marker)
        })

        obstacleMarkers.value = currentMarkers
    }

    /**
     * Clears all obstacle markers from map.
     */
    function clearObstacles() {
        obstacleMarkers.value.forEach(marker => marker.remove())
        obstacleMarkers.value = []
    }

    return {
        addObstacles,
        clearObstacles
    }
}