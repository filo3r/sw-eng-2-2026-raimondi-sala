/**
 * Composable for drawing bike path or trip routes on Mapbox map.
 * Handles route rendering, markers, and map bounds fitting.
 */
import { ref, type Ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import {
    ROUTE_LINE_COLOR,
    ROUTE_LINE_WIDTH,
    ROUTE_LINE_JOIN,
    ROUTE_LINE_CAP,
    ORIGIN_MARKER_COLOR,
    DESTINATION_MARKER_COLOR,
    FIT_BOUNDS_DURATION,
    FIT_BOUNDS_PADDING,
    POPUP_OFFSET,
    POPUP_CONTENT_PADDING,
    POPUP_MIN_WIDTH,
    ROUTE_POPUP_CLASS
} from '@/constants/map'
import type { BikePathPointResponse } from '@/types/bikePath'
import type { TripPointResponse } from '@/types/trip'

type RoutePoint = BikePathPointResponse | TripPointResponse

/**
 * Creates route visualization utilities for Mapbox maps.
 * @param map - Reactive reference to Mapbox map instance
 * @returns Methods to draw, clear routes and add markers
 */
export function useMapRoute(map: Ref<mapboxgl.Map | null>) {
    const markers = ref<mapboxgl.Marker[]>([])

    /**
     * Draws route line on map from sequential points.
     * @param points - Array of route points with coordinates
     */
    function drawRoute(points: RoutePoint[]) {
        if (!map.value) return
        const mapInstance = map.value
        clearRoute()
        // Sort by sequential position to ensure correct line order
        const sortedPoints = [...points].sort((a, b) => a.sequentialPosition - b.sequentialPosition)
        const coordinates = sortedPoints.map(point => [point.longitude, point.latitude])
        // Add route as GeoJSON LineString
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
        // Style the route line
        mapInstance.addLayer({
            id: 'route',
            type: 'line',
            source: 'route',
            layout: {
                'line-join': ROUTE_LINE_JOIN,
                'line-cap': ROUTE_LINE_CAP
            },
            paint: {
                'line-color': ROUTE_LINE_COLOR,
                'line-width': ROUTE_LINE_WIDTH
            }
        })
        fitRouteBounds(coordinates)
    }

    /**
     * Adds origin and destination markers with popups.
     * @param origin - Origin location with address and coordinates
     * @param destination - Destination location with address and coordinates
     */
    function addMarkers(
        origin: { address: string; latitude: number; longitude: number },
        destination: { address: string; latitude: number; longitude: number }
    ) {
        if (!map.value)
            return
        const mapInstance = map.value
        const currentMarkers: mapboxgl.Marker[] = []
        // Create origin marker (green)
        const originMarker = new mapboxgl.Marker({ color: ORIGIN_MARKER_COLOR })
            .setLngLat([origin.longitude, origin.latitude])
            .setPopup(
                new mapboxgl.Popup({ offset: POPUP_OFFSET, className: ROUTE_POPUP_CLASS }).setHTML(`
          <div style="padding: ${POPUP_CONTENT_PADDING}px; min-width: ${POPUP_MIN_WIDTH}px;">
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
        // Create destination marker (purple)
        const destinationMarker = new mapboxgl.Marker({ color: DESTINATION_MARKER_COLOR })
            .setLngLat([destination.longitude, destination.latitude])
            .setPopup(
                new mapboxgl.Popup({ offset: POPUP_OFFSET, className: ROUTE_POPUP_CLASS }).setHTML(`
          <div style="padding: ${POPUP_CONTENT_PADDING}px; min-width: ${POPUP_MIN_WIDTH}px;">
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
     * Adjusts map viewport to fit route coordinates.
     * @param coordinates - Array of [lng, lat] coordinates
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
     * Removes route line and markers from map.
     */
    function clearRoute() {
        if (!map.value)
            return
        const mapInstance = map.value
        // Remove route layer and source
        if (mapInstance.getLayer('route')) {
            mapInstance.removeLayer('route')
        }
        if (mapInstance.getSource('route')) {
            mapInstance.removeSource('route')
        }
        // Remove all markers
        markers.value.forEach(marker => marker.remove())
        markers.value = []
    }

    return {
        drawRoute,
        addMarkers,
        clearRoute
    }
}