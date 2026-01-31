/**
 * Composable for drawing bike path or trip routes on Mapbox map.
 * Handles route line rendering, origin/destination markers with popups, and automatic viewport fitting.
 * Supports both BikePathPointResponse and TripPointResponse for flexible route visualization.
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

/** Union type for route points supporting both bike path and trip routes */
type RoutePoint = BikePathPointResponse | TripPointResponse

/**
 * Composable that creates route visualization utilities for Mapbox maps.
 * Provides methods to draw routes, add markers, and manage map viewport.
 * Automatically cleans up route and markers on component unmount.
 * @param map - Reactive reference to Mapbox map instance (can be null during initialization)
 * @returns Object containing methods to draw, clear routes, and add origin/destination markers
 */
export function useMapRoute(map: Ref<mapboxgl.Map | null>) {
    const { markers, clearAll } = useMarkerManager()
    /**
     * Draws route line on map from sequential coordinate points.
     * Sorts points by sequential position, creates GeoJSON LineString, and fits map bounds.
     * @param points - Array of route points with coordinates and sequential positions
     */
    function drawRoute(points: RoutePoint[]) {
        if (!map.value) return
        const mapInstance = map.value
        clearRoute() // Remove any existing route before drawing new one
        // Sort by sequential position to ensure correct line order
        const sortedPoints = [...points].sort((a, b) => a.sequentialPosition - b.sequentialPosition)
        const coordinates = sortedPoints.map(point => [point.longitude, point.latitude])
        // Add route as GeoJSON LineString source
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
        // Add line layer with styling from constants
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
        // Automatically fit map bounds to show entire route
        fitRouteBounds(coordinates)
    }
    /**
     * Adds origin and destination markers with custom styling and address popups.
     * Origin marker is green with 'O' label, destination is purple with 'D' label.
     * @param origin - Origin location with formatted address and coordinates
     * @param destination - Destination location with formatted address and coordinates
     */
    function addMarkers(
        origin: { address: string; latitude: number; longitude: number },
        destination: { address: string; latitude: number; longitude: number }
    ) {
        if (!map.value) return
        const mapInstance = map.value
        const currentMarkers: mapboxgl.Marker[] = []
        // Create custom origin marker element (green circular marker)
        const originEl = createCustomMarkerElement({
            color: ORIGIN_MARKER_COLOR,
            label: 'O',
            draggable: false
        })
        // Create origin marker with popup showing address
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
        // Create custom destination marker element (purple circular marker)
        const destinationEl = createCustomMarkerElement({
            color: DESTINATION_MARKER_COLOR,
            label: 'D',
            draggable: false
        })
        // Create destination marker with popup showing address
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
     * Adjusts map viewport to fit all route coordinates with padding and animation.
     * Calculates bounding box from coordinates and animates map to fit bounds.
     * @param coordinates - Array of [longitude, latitude] coordinate pairs
     */
    function fitRouteBounds(coordinates: number[][]) {
        if (!map.value || coordinates.length === 0) return
        // Calculate bounding box from all coordinates
        const bounds = coordinates.reduce(
            (bounds, coord) => bounds.extend(coord as [number, number]),
            new mapboxgl.LngLatBounds(
                coordinates[0] as [number, number],
                coordinates[0] as [number, number]
            )
        )
        // Animate map to fit calculated bounds with padding
        map.value.fitBounds(bounds, {
            padding: FIT_BOUNDS_PADDING,
            duration: FIT_BOUNDS_DURATION
        })
    }
    /**
     * Removes route line and all markers from the map.
     * Cleans up route layer, source, and marker instances.
     */
    function clearRoute() {
        if (!map.value) return
        const mapInstance = map.value
        // Remove route layer if exists
        if (mapInstance.getLayer(ROUTE_LAYER_ID)) {
            mapInstance.removeLayer(ROUTE_LAYER_ID)
        }
        // Remove route source if exists
        if (mapInstance.getSource(ROUTE_SOURCE_ID)) {
            mapInstance.removeSource(ROUTE_SOURCE_ID)
        }
        // Remove all markers from map
        clearAll()
    }
    // Cleanup route and markers on component unmount
    onUnmounted(() => {
        clearRoute()
    })
    return {
        drawRoute,
        addMarkers,
        clearRoute
    }
}