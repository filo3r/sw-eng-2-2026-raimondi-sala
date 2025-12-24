<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import mapboxgl from 'mapbox-gl'
import { getMapboxApiKey } from '@/config/mapbox'

const mapContainer = ref<HTMLDivElement | null>(null)
let map: mapboxgl.Map | null = null

onMounted(() => {
  document.body.style.overflow = 'hidden'

  if (!mapContainer.value) return

  mapboxgl.accessToken = getMapboxApiKey()

  map = new mapboxgl.Map({
    container: mapContainer.value,
    style: 'mapbox://styles/mapbox/outdoors-v12',
    center: [9.19, 45.46],
    zoom: 12,
    pitch: 0,
    bearing: 0,
    collectResourceTiming: false,
    attributionControl: false
  })

  map.addControl(new mapboxgl.AttributionControl({ compact: true }), 'bottom-right')
  map.addControl(new mapboxgl.NavigationControl(), 'top-right')
  map.addControl(new mapboxgl.GeolocateControl({
    positionOptions: {
      enableHighAccuracy: true
    },
    trackUserLocation: true,
    showUserHeading: true
  }), 'top-right')

  map.on('load', () => {
    const compassButton = document.querySelector('.mapboxgl-ctrl-compass')
    compassButton?.addEventListener('click', () => {
      map?.easeTo({
        pitch: 0,
        bearing: 0,
        duration: 500
      })
    })
  })

  setTimeout(() => {
    map?.resize()
  }, 300)
})

onUnmounted(() => {
  document.body.style.overflow = ''
  map?.remove()
})
</script>

<template>
  <div class="h-full w-full overflow-hidden">
    <div ref="mapContainer" class="h-full w-full"></div>
  </div>
</template>