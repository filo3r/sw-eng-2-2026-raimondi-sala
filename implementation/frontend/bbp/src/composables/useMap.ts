/**
 * Composable for initializing and managing Mapbox map instance.
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
    GEOLOCATION_TIMEOUT
} from '@/constants/map'

export interface UseMapOptions {
    /** Container element ref for the map */
    container: Ref<HTMLElement | null>
    /** Mapbox access token */
    accessToken: string
    /** Enable user interaction (zoom, pan, etc.) */
    interactive?: boolean
    /** Enable geolocation control */
    enableGeolocation?: boolean
    /** Initial center coordinates [lng, lat] */
    center?: [number, number]
    /** Initial zoom level */
    zoom?: number
}

export function useMap(options: UseMapOptions) {
    const map = shallowRef<mapboxgl.Map | null>(null)
    const isReady = ref(false)

    /**
     * Initializes the Mapbox map with provided options.
     */
    function initMap() {
        if (!options.container.value) {
            console.error('Map container not available')
            return
        }

        mapboxgl.accessToken = options.accessToken

        const interactive = options.interactive ?? true

        const mapInstance = new mapboxgl.Map({
            container: options.container.value,
            style: MAPBOX_STYLE,
            center: options.center ?? DEFAULT_MAP_CENTER,
            zoom: options.zoom ?? DEFAULT_ZOOM,
            pitch: DEFAULT_PITCH,
            bearing: DEFAULT_BEARING,
            collectResourceTiming: false,
            attributionControl: false,
            // Disable interaction if static map
            interactive,
            dragPan: interactive,
            scrollZoom: interactive,
            boxZoom: interactive,
            doubleClickZoom: interactive,
            keyboard: interactive,
            touchZoomRotate: interactive
        })

        map.value = mapInstance

        // Add attribution control (compact)
        mapInstance.addControl(
            new mapboxgl.AttributionControl({ compact: true }) as mapboxgl.IControl,
            'bottom-right'
        )

        // Add navigation and geolocation controls only for interactive maps
        if (interactive) {
            mapInstance.addControl(
                new mapboxgl.NavigationControl() as mapboxgl.IControl,
                'top-right'
            )

            if (options.enableGeolocation) {
                const geolocateControl = new mapboxgl.GeolocateControl({
                    positionOptions: {
                        enableHighAccuracy: true
                    },
                    trackUserLocation: true,
                    showUserHeading: true
                })

                mapInstance.addControl(geolocateControl as mapboxgl.IControl, 'top-right')

                // Trigger geolocation on load
                mapInstance.on('load', () => {
                    navigator.geolocation.getCurrentPosition(
                        () => {
                            geolocateControl.trigger()
                        },
                        (error) => {
                            console.warn('Geolocation not available:', error.message)
                        },
                        {
                            enableHighAccuracy: true,
                            timeout: GEOLOCATION_TIMEOUT
                        }
                    )
                })
            }

            // Setup compass reset behavior
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

            // Resize map after initialization
            setTimeout(() => {
                mapInstance.resize()
            }, MAP_RESIZE_TIMEOUT)
        } else {
            // For static maps, mark as ready immediately after load
            mapInstance.on('load', () => {
                isReady.value = true
            })
        }
    }

    /**
     * Cleans up map instance.
     */
    function cleanup() {
        if (map.value) {
            map.value.remove()
            map.value = null
        }
        isReady.value = false
    }

    onUnmounted(cleanup)

    return {
        map,
        isReady,
        initMap,
        cleanup
    }
}