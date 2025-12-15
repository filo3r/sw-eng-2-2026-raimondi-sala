<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import mapboxgl from 'mapbox-gl'
import { getMapboxApiKey } from '@/config/mapbox'

const mapContainer = ref<HTMLDivElement | null>(null)
let map: mapboxgl.Map | null = null

onMounted(() => {
  if (!mapContainer.value) return

  mapboxgl.accessToken = getMapboxApiKey()

  map = new mapboxgl.Map({
    container: mapContainer.value,
    style: 'mapbox://styles/mapbox/streets-v12',
    center: [9.19, 45.46],
    zoom: 12,
    collectResourceTiming: false
  })
})

onUnmounted(() => {
  map?.remove()
})
</script>

<template>
  <div style="height: calc(100vh - 5rem);">
    <div ref="mapContainer" class="h-full w-full"></div>
  </div>
</template>