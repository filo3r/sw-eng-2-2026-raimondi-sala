/**
 * Composable for initializing and managing Mapbox map instances.
 * Handles interactive and static maps with optional geolocation.
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

export interface UseMapOptions {
    container: Ref<HTMLElement | null>
    accessToken: string
    interactive?: boolean
    enableGeolocation?: boolean
    center?: [number, number]
    zoom?: number
}

/**
 * Creates and manages a Mapbox GL map instance.
 * @param options - Map initialization options
 * @returns Map instance, ready state, and initialization method
 */
export function useMap(options: UseMapOptions) {
    const map = shallowRef<mapboxgl.Map | null>(null)
    const isReady = ref(false)

    /**
     * Initializes the Mapbox map with provided configuration.
     */
    function initMap() {
        if (!options.container.value) {
            console.error('Map container not available')
            return
        }
        // Set global Mapbox access token
        mapboxgl.accessToken = options.accessToken
        const interactive = options.interactive ?? true
        // Create map instance with configuration
        const mapInstance = new mapboxgl.Map({
            container: options.container.value,
            style: MAPBOX_STYLE,
            center: options.center ?? DEFAULT_MAP_CENTER,
            zoom: options.zoom ?? DEFAULT_ZOOM,
            pitch: DEFAULT_PITCH,
            bearing: DEFAULT_BEARING,
            language: MAP_LANGUAGE,
            collectResourceTiming: MAP_COLLECT_RESOURCE_TIMING,
            attributionControl: false,
            // Control interaction based on interactive flag
            interactive,
            dragPan: interactive,
            scrollZoom: interactive,
            boxZoom: interactive,
            doubleClickZoom: interactive,
            keyboard: interactive,
            touchZoomRotate: interactive
        })
        map.value = mapInstance
        // Add compact attribution control
        mapInstance.addControl(
            new mapboxgl.AttributionControl({ compact: ATTRIBUTION_COMPACT }),
            ATTRIBUTION_POSITION
        )
        // Setup interactive map features
        if (interactive) {
            // Add navigation controls (zoom, compass)
            mapInstance.addControl(
                new mapboxgl.NavigationControl(),
                NAVIGATION_POSITION
            )
            // Setup geolocation if enabled
            if (options.enableGeolocation) {
                const geolocateControl = new mapboxgl.GeolocateControl({
                    positionOptions: { enableHighAccuracy: GEOLOCATION_HIGH_ACCURACY },
                    trackUserLocation: GEOLOCATION_TRACK_USER,
                    showUserHeading: GEOLOCATION_SHOW_HEADING
                })
                mapInstance.addControl(geolocateControl, NAVIGATION_POSITION)
                // Auto-trigger geolocation on map load
                mapInstance.on('load', () => {
                    navigator.geolocation.getCurrentPosition(
                        () => geolocateControl.trigger(),
                        (error) => console.warn('Geolocation not available:', error.message),
                        { enableHighAccuracy: GEOLOCATION_HIGH_ACCURACY, timeout: GEOLOCATION_TIMEOUT }
                    )
                })
            }
            // Setup compass reset behavior and mark map as ready
            mapInstance.on('load', () => {
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
            // Ensure map renders correctly after mount
            setTimeout(() => mapInstance.resize(), MAP_RESIZE_TIMEOUT)
        } else {
            // For static maps, mark as ready immediately after load
            mapInstance.on('load', () => {
                isReady.value = true
            })
        }
    }

    /**
     * Cleans up map instance and resets state.
     */
    function cleanup() {
        if (map.value) {
            map.value.remove()
            map.value = null
        }
        isReady.value = false
    }

    // Auto-cleanup on component unmount
    onUnmounted(cleanup)

    return { map, isReady, initMap }
}