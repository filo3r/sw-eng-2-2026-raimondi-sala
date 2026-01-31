/**
 * Composable for initializing and managing Mapbox GL JS map instances.
 * Handles both interactive and static maps with optional controls, geolocation, and lifecycle management.
 * Supports custom center/zoom or defaults to Milan with configurable interaction modes.
 */
import { shallowRef, ref, onUnmounted, type Ref } from 'vue'
import mapboxgl from 'mapbox-gl'
import {
    MAPBOX_STYLE,
    DEFAULT_MAP_CENTER,
    DEFAULT_ZOOM,
    DEFAULT_PITCH,
    DEFAULT_BEARING,
    COMPASS_RESET_DURATION,
    MAP_RESIZE_TIMEOUT,
    GEOLOCATION_TIMEOUT,
    MAP_LANGUAGE,
    MAP_COLLECT_RESOURCE_TIMING,
    ATTRIBUTION_POSITION,
    ATTRIBUTION_COMPACT,
    NAVIGATION_POSITION,
    GEOLOCATION_HIGH_ACCURACY,
    GEOLOCATION_TRACK_USER,
    GEOLOCATION_SHOW_HEADING
} from '@/constants/map'

/**
 * Configuration options for map initialization.
 */
export interface UseMapOptions {
    /** Reactive reference to HTML container element for map rendering */
    container: Ref<HTMLElement | null>
    /** Mapbox API access token for authentication */
    accessToken: string
    /** Enable user interactions (pan, zoom, rotate, etc.) - default: true */
    interactive?: boolean
    /** Enable geolocation control with auto-trigger - default: false */
    enableGeolocation?: boolean
    /** Initial map center as [lng, lat] - default: Milan coordinates */
    center?: [number, number]
    /** Initial zoom level - default: 12 (city level) */
    zoom?: number
}

/**
 * Composable that creates and manages a Mapbox GL JS map instance with lifecycle handling.
 * Configures map with controls, geolocation, and interaction settings based on options.
 * Automatically cleans up map resources on component unmount.
 * @param options - Map initialization configuration
 * @returns Object containing map instance ref, ready state, and initialization method
 */
export function useMap(options: UseMapOptions) {
    /** Reactive reference to Mapbox map instance (shallow for performance) */
    const map = shallowRef<mapboxgl.Map | null>(null)
    /** Flag indicating if map has loaded and is ready for use */
    const isReady = ref(false)
    /**
     * Initializes the Mapbox map instance with provided configuration.
     * Sets up controls, geolocation, and interaction modes based on options.
     * Should be called after container element is mounted in DOM.
     */
    function initMap() {
        if (!options.container.value) {
            console.error('Map container not available')
            return
        }
        // Set global Mapbox access token for API authentication
        mapboxgl.accessToken = options.accessToken
        const interactive = options.interactive ?? true
        // Create map instance with base configuration
        const mapInstance = new mapboxgl.Map({
            container: options.container.value,
            style: MAPBOX_STYLE,
            center: options.center ?? DEFAULT_MAP_CENTER,
            zoom: options.zoom ?? DEFAULT_ZOOM,
            pitch: DEFAULT_PITCH,
            bearing: DEFAULT_BEARING,
            language: MAP_LANGUAGE,
            collectResourceTiming: MAP_COLLECT_RESOURCE_TIMING,
            attributionControl: false, // Custom attribution added below
            // Enable/disable all interactions based on interactive flag
            interactive,
            dragPan: interactive,
            scrollZoom: interactive,
            boxZoom: interactive,
            doubleClickZoom: interactive,
            keyboard: interactive,
            touchZoomRotate: interactive
        })
        map.value = mapInstance
        // Add compact attribution control (required by Mapbox terms)
        mapInstance.addControl(
            new mapboxgl.AttributionControl({ compact: ATTRIBUTION_COMPACT }),
            ATTRIBUTION_POSITION
        )
        // Setup interactive map features (controls and geolocation)
        if (interactive) {
            // Add navigation controls (zoom buttons and compass)
            mapInstance.addControl(
                new mapboxgl.NavigationControl(),
                NAVIGATION_POSITION
            )
            // Setup geolocation control if enabled in options
            if (options.enableGeolocation) {
                const geolocateControl = new mapboxgl.GeolocateControl({
                    positionOptions: { enableHighAccuracy: GEOLOCATION_HIGH_ACCURACY },
                    trackUserLocation: GEOLOCATION_TRACK_USER,
                    showUserHeading: GEOLOCATION_SHOW_HEADING
                })
                mapInstance.addControl(geolocateControl, NAVIGATION_POSITION)
                // Auto-trigger geolocation after map loads
                mapInstance.on('load', () => {
                    navigator.geolocation.getCurrentPosition(
                        () => geolocateControl.trigger(), // Success: activate geolocation control
                        (error) => console.warn('Geolocation not available:', error.message), // Error: log warning
                        { enableHighAccuracy: GEOLOCATION_HIGH_ACCURACY, timeout: GEOLOCATION_TIMEOUT }
                    )
                })
            }
            // Setup compass reset behavior and mark map as ready
            mapInstance.on('load', () => {
                // Make compass button reset pitch and bearing instead of just bearing
                const compassButton = document.querySelector('.mapboxgl-ctrl-compass')
                compassButton?.addEventListener('click', () => {
                    mapInstance.easeTo({
                        pitch: DEFAULT_PITCH,
                        bearing: DEFAULT_BEARING,
                        duration: COMPASS_RESET_DURATION
                    })
                })
                isReady.value = true
            })
            // Ensure map renders correctly after mount with delayed resize
            setTimeout(() => mapInstance.resize(), MAP_RESIZE_TIMEOUT)
        } else {
            // For static maps, mark as ready immediately after load (no controls needed)
            mapInstance.on('load', () => {
                isReady.value = true
            })
        }
    }
    /**
     * Cleans up map instance and resets component state.
     * Removes map from DOM and frees up resources.
     */
    function cleanup() {
        if (map.value) {
            map.value.remove() // Remove map and all event listeners
            map.value = null
        }
        isReady.value = false
    }
    // Automatically cleanup map resources on component unmount
    onUnmounted(cleanup)
    return { map, isReady, initMap }
}