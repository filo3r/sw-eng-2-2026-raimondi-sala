/**
 * Composable for drawing bike path or trip routes on Mapbox map.
 * Handles route rendering, markers, and map bounds fitting.
 */
import { type Ref, onUnmounted } from 'vue'
import mapboxgl from 'mapbox-gl'
import { createCustomMarkerElement } from '@/utils/mapMarkers'
import { createOriginPopupHTML, createDestinationPopupHTML } from '@/utils/mapPopups'
import { useMarkerManager } from './useMarkerManager'
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
    ROUTE_POPUP_CLASS,
    ROUTE_SOURCE_ID,
    ROUTE_LAYER_ID
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
    const { markers, clearAll } = useMarkerManager()

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
        mapInstance.addSource(ROUTE_SOURCE_ID, {
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
            id: ROUTE_LAYER_ID,
            type: 'line',
            source: ROUTE_SOURCE_ID,
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
        if (!map.value) return

        const mapInstance = map.value
        const currentMarkers: mapboxgl.Marker[] = []

        // Create custom origin marker element
        const originEl = createCustomMarkerElement({
            color: ORIGIN_MARKER_COLOR,
            label: 'O',
            draggable: false
        })

        // Create origin marker (green with 'O' label)
        const originMarker = new mapboxgl.Marker({ element: originEl })
            .setLngLat([origin.longitude, origin.latitude])
            .setPopup(
                new mapboxgl.Popup({
                    offset: POPUP_OFFSET,
                    className: ROUTE_POPUP_CLASS
                }).setHTML(createOriginPopupHTML(origin.address))
            )
            .addTo(mapInstance)

        currentMarkers.push(originMarker)

        // Create custom destination marker element
        const destinationEl = createCustomMarkerElement({
            color: DESTINATION_MARKER_COLOR,
            label: 'D',
            draggable: false
        })

        // Create destination marker (purple with 'D' label)
        const destinationMarker = new mapboxgl.Marker({ element: destinationEl })
            .setLngLat([destination.longitude, destination.latitude])
            .setPopup(
                new mapboxgl.Popup({
                    offset: POPUP_OFFSET,
                    className: ROUTE_POPUP_CLASS
                }).setHTML(createDestinationPopupHTML(destination.address))
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
        if (!map.value) return

        const mapInstance = map.value

        // Remove route layer and source
        if (mapInstance.getLayer(ROUTE_LAYER_ID)) {
            mapInstance.removeLayer(ROUTE_LAYER_ID)
        }
        if (mapInstance.getSource(ROUTE_SOURCE_ID)) {
            mapInstance.removeSource(ROUTE_SOURCE_ID)
        }

        // Remove all markers
        clearAll()
    }

    // Cleanup on unmount
    onUnmounted(() => {
        clearRoute()
    })

    return {
        drawRoute,
        addMarkers,
        clearRoute
    }
}