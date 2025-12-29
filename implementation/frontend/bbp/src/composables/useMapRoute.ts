/**
 * Composable for drawing bike path or trip routes on Mapbox map.
 */

import { ref, type Ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import {
    ROUTE_LINE_COLOR,
    ROUTE_LINE_WIDTH,
    ORIGIN_MARKER_COLOR,
    DESTINATION_MARKER_COLOR,
    FIT_BOUNDS_DURATION,
    FIT_BOUNDS_PADDING,
    POPUP_OFFSET
} from '@/constants/map'
import type { BikePathPointResponse } from '@/types/bikePath'
import type { TripPointResponse } from '@/types/trip'

type RoutePoint = BikePathPointResponse | TripPointResponse

export function useMapRoute(map: Ref<mapboxgl.Map | null>) {
    const markers = ref<mapboxgl.Marker[]>([])

    /**
     * Draws route on map from points.
     */
    function drawRoute(points: RoutePoint[]) {
        if (!map.value) return

        const mapInstance = map.value

        // Remove existing route
        clearRoute()

        // Sort points by sequential position
        const sortedPoints = [...points].sort((a, b) => a.sequentialPosition - b.sequentialPosition)

        // Extract coordinates
        const coordinates = sortedPoints.map(point => [point.longitude, point.latitude])

        // Add GeoJSON source
        mapInstance.addSource('route', {
            type: 'geojson',
            data: {
                type: 'Feature',
                properties: {},
                geometry: {
                    type: 'LineString',
                    coordinates
                }
            }
        })

        // Add layer
        mapInstance.addLayer({
            id: 'route',
            type: 'line',
            source: 'route',
            layout: {
                'line-join': 'round',
                'line-cap': 'round'
            },
            paint: {
                'line-color': ROUTE_LINE_COLOR,
                'line-width': ROUTE_LINE_WIDTH
            }
        })

        // Fit bounds to route
        fitRouteBounds(coordinates)
    }

    /**
     * Adds origin and destination markers.
     */
    function addMarkers(
        origin: { address: string; latitude: number; longitude: number },
        destination: { address: string; latitude: number; longitude: number }
    ) {
        if (!map.value) return

        const mapInstance = map.value
        const currentMarkers: mapboxgl.Marker[] = []

        // Origin marker (green)
        const originMarker = new mapboxgl.Marker({ color: ORIGIN_MARKER_COLOR })
            .setLngLat([origin.longitude, origin.latitude])
            .setPopup(
                new mapboxgl.Popup({ offset: POPUP_OFFSET, className: 'route-popup' }).setHTML(`
          <div style="padding: 12px; min-width: 200px;">
            <h3 style="font-size: 16px; font-weight: 600; margin: 0 0 12px 0; color: #1f2937;">
              Origin
            </h3>
            <div>
              <span style="font-size: 14px; font-weight: 500; color: #374151;">Address:</span>
              <p style="font-size: 14px; color: #6b7280; margin: 4px 0 0 0;">${origin.address}</p>
            </div>
          </div>
        `)
            )
            .addTo(mapInstance)

        currentMarkers.push(originMarker)

        // Destination marker (purple)
        const destinationMarker = new mapboxgl.Marker({ color: DESTINATION_MARKER_COLOR })
            .setLngLat([destination.longitude, destination.latitude])
            .setPopup(
                new mapboxgl.Popup({ offset: POPUP_OFFSET, className: 'route-popup' }).setHTML(`
          <div style="padding: 12px; min-width: 200px;">
            <h3 style="font-size: 16px; font-weight: 600; margin: 0 0 12px 0; color: #1f2937;">
              Destination
            </h3>
            <div>
              <span style="font-size: 14px; font-weight: 500; color: #374151;">Address:</span>
              <p style="font-size: 14px; color: #6b7280; margin: 4px 0 0 0;">${destination.address}</p>
            </div>
          </div>
        `)
            )
            .addTo(mapInstance)

        currentMarkers.push(destinationMarker)

        markers.value = currentMarkers
    }

    /**
     * Fits map bounds to route coordinates.
     */
    function fitRouteBounds(coordinates: number[][]) {
        if (!map.value || coordinates.length === 0) return

        const bounds = coordinates.reduce(
            (bounds, coord) => bounds.extend(coord as [number, number]),
            new mapboxgl.LngLatBounds(
                coordinates[0] as [number, number],
                coordinates[0] as [number, number]
            )
        )

        map.value.fitBounds(bounds, {
            padding: FIT_BOUNDS_PADDING,
            duration: FIT_BOUNDS_DURATION
        })
    }

    /**
     * Clears route from map.
     */
    function clearRoute() {
        if (!map.value) return

        const mapInstance = map.value

        // Remove layer
        if (mapInstance.getLayer('route')) {
            mapInstance.removeLayer('route')
        }

        // Remove source
        if (mapInstance.getSource('route')) {
            mapInstance.removeSource('route')
        }

        // Remove markers
        markers.value.forEach(marker => marker.remove())
        markers.value = []
    }

    return {
        drawRoute,
        addMarkers,
        clearRoute
    }
}