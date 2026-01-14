<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Plus, Trash2 } from 'lucide-vue-next'
import { createTripManually } from '@/services/trip'
import { useToast } from '@/composables/useToast'
import { getMapboxApiKey } from '@/config/mapbox'
import { useMap } from '@/composables/useMap'
import { isEndTimeAfterStartTime, isValidTripDuration, isValidMaxSpeed } from '@/utils/validation'
import mapboxgl from 'mapbox-gl'

const router = useRouter()
const { show } = useToast()

const mapContainer = ref<HTMLElement | null>(null)
const { initMap, map } = useMap({
  container: mapContainer,
  accessToken: getMapboxApiKey(),
  interactive: true,
  enableGeolocation: true
})

const addresses = ref<string[]>(['', ''])
const description = ref('')
const startTime = ref('')
const endTime = ref('')
const maxSpeed = ref<number | ''>('')
const loading = ref(false)

const routeMarkers = ref<(mapboxgl.Marker | null)[]>([null, null])
const activeField = ref<number | null>(null)


function onInputFocus(index: number) {
  activeField.value = index
}

async function updateRoute() {
  if (!map.value) return

  const coordinates = routeMarkers.value
    .filter((m): m is mapboxgl.Marker => m !== null)
    .map(m => m.getLngLat())
  
  if (coordinates.length < 2) {
    const source = map.value.getSource('trip-route') as mapboxgl.GeoJSONSource
    if (source) {
      source.setData({ type: 'FeatureCollection', features: [] })
    }
    return
  }

  const coordString = coordinates.map(c => `${c.lng},${c.lat}`).join(';')
  const token = getMapboxApiKey()
  const url = `https://api.mapbox.com/directions/v5/mapbox/driving/${coordString}?geometries=geojson&access_token=${token}`

  try {
    const res = await fetch(url)
    const data = await res.json()

    if (data.routes && data.routes.length > 0) {
      const routeGeoJSON = data.routes[0].geometry
      const source = map.value.getSource('trip-route') as mapboxgl.GeoJSONSource
      if (source) {
        source.setData({
          type: 'Feature',
          properties: {},
          geometry: routeGeoJSON
        })
      }
    }
  } catch (e) {
    console.error('Error fetching route:', e)
  }
}

async function getAddressFromCoordinates(lng: number, lat: number): Promise<string> {
  const token = getMapboxApiKey()
  const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?access_token=${token}&types=address,poi`
  try {
    const res = await fetch(url)
    const data = await res.json()
    return data.features?.[0]?.place_name || `${lat.toFixed(6)}, ${lng.toFixed(6)}`
  } catch (e) {
    return `${lat.toFixed(6)}, ${lng.toFixed(6)}`
  }
}

function setMarker(index: number, lng: number, lat: number) {
  if (!map.value) return

  if (routeMarkers.value[index]) {
    routeMarkers.value[index]?.remove()
  }

  const newMarker = new mapboxgl.Marker({ color: '#10b981', draggable: true }) 
    .setLngLat([lng, lat])
    .addTo(map.value)

  newMarker.on('dragend', async () => {
    const { lng, lat } = newMarker.getLngLat()
    const address = await getAddressFromCoordinates(lng, lat)
    addresses.value[index] = address
    updateRoute()
  })

  routeMarkers.value[index] = newMarker
  
  updateRoute()
}

async function handleMapClick(e: mapboxgl.MapMouseEvent) {
  if (activeField.value === null) return

  const { lng, lat } = e.lngLat
  const index = activeField.value

  const address = await getAddressFromCoordinates(lng, lat)
  addresses.value[index] = address

  setMarker(index, lng, lat)
}

watch(map, (newMap) => {
  if (newMap) {
    newMap.on('load', () => {
      if (!newMap.getSource('trip-route')) {
        newMap.addSource('trip-route', {
          type: 'geojson',
          data: { type: 'FeatureCollection', features: [] }
        })
        newMap.addLayer({
          id: 'trip-route',
          type: 'line',
          source: 'trip-route',
          layout: { 'line-join': 'round', 'line-cap': 'round' },
          paint: {
            'line-color': '#10b981',
            'line-width': 4,
            'line-opacity': 0.75
          }
        })
      }
    })
    
    newMap.on('click', handleMapClick)
    newMap.getCanvas().style.cursor = 'crosshair'
  }
})

function addAddress() {
  addresses.value.push('')
  routeMarkers.value.push(null)
}

function removeAddress(index: number) {
  if (addresses.value.length > 2) {
    routeMarkers.value[index]?.remove()
    
    addresses.value.splice(index, 1)
    routeMarkers.value.splice(index, 1)
    if (activeField.value === index) activeField.value = null

    updateRoute()
  }
}


async function handleSubmit() {
  const validAddresses = addresses.value.filter(addr => addr.trim() !== '')

  if (validAddresses.length < 2) {
    show('At least 2 addresses are required', 'error')
    return
  }

  if (!startTime.value) {
    show('Start time is required', 'error')
    return
  }

  if (!endTime.value) {
    show('End time is required', 'error')
    return
  }

  if (!isEndTimeAfterStartTime(startTime.value, endTime.value)) {
    show('End time must be after start time', 'error')
    return
  }

  if (!isValidTripDuration(startTime.value, endTime.value)) {
    show('Trip must be at least 1 minute long', 'error')
    return
  }

  if (maxSpeed.value !== '' && !isValidMaxSpeed(maxSpeed.value)) {
    show('Max speed must be a positive number with at most 3 integer digits and 2 decimal digits', 'error')
    return
  }

  loading.value = true

  try {
    const tripId = await createTripManually({
      addresses: validAddresses,
      description: description.value || undefined,
      startTime: new Date(startTime.value).toISOString(),
      endTime: new Date(endTime.value).toISOString(),
      maxSpeed: maxSpeed.value !== '' ? Number(maxSpeed.value) : undefined
    })

    show('Trip created successfully', 'success')
    router.push(`/trips/`)
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to create trip'
    show(message, 'error')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  initMap()
})
</script>

<template>
  <div class="p-6 overflow-x-hidden">
    <div class="flex items-center gap-4 mb-6">
      <button @click="goBack" class="btn btn-ghost btn-circle shrink-0">
        <ArrowLeft :size="20" />
      </button>
      <h1 class="text-3xl font-bold">Record Trip</h1>
    </div>

    <div class="card bg-base-100 shadow-xl mb-6">
      <div class="card-body p-0">
        <div ref="mapContainer" class="w-full h-96 rounded-lg"></div>
      </div>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-6">
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">Route Addresses</h2>
          <div class="text-sm text-gray-500 mb-2">
            Click on an input field, then click on the map to set the point.
          </div>

          <div class="space-y-4">
            <div v-for="(_address, index) in addresses" :key="index" class="flex items-start gap-2">
              <div class="flex-1">
                <label class="label">
                  <span class="label-text">
                    {{ index === 0 ? 'Origin' : index === addresses.length - 1 ? 'Destination' : `Waypoint ${index}` }}
                  </span>
                </label>
                <input
                    v-model="addresses[index]"
                    type="text"
                    placeholder="Select field then click on map"
                    class="input input-bordered w-full transition-colors"
                    :class="{ 'input-success border-2': activeField === index }"
                    @focus="onInputFocus(index)"
                    readonly
                    required
                    maxlength="256"
                />
              </div>
              <button
                  v-if="addresses.length > 2"
                  type="button"
                  @click="removeAddress(index)"
                  class="btn btn-ghost btn-sm mt-9"
              >
                <Trash2 :size="16" />
              </button>
            </div>
          </div>

          <button
              type="button"
              @click="addAddress"
              class="btn btn-ghost btn-sm mt-4"
          >
            <Plus :size="16" />
            Add Waypoint
          </button>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">Trip Details</h2>

          <div>
            <label class="label">
              <span class="label-text">Description (optional)</span>
            </label>
            <textarea
                v-model="description"
                placeholder="Add notes or description"
                class="textarea textarea-bordered w-full"
                rows="3"
                maxlength="500"
            ></textarea>
            <label class="label">
              <span class="label-text-alt">{{ description.length }}/500</span>
            </label>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="label">
                <span class="label-text">Start Time</span>
              </label>
              <input
                  v-model="startTime"
                  type="datetime-local"
                  class="input input-bordered w-full"
                  required
              />
            </div>

            <div>
              <label class="label">
                <span class="label-text">End Time</span>
              </label>
              <input
                  v-model="endTime"
                  type="datetime-local"
                  class="input input-bordered w-full"
                  required
              />
            </div>
          </div>

          <div>
            <label class="label">
              <span class="label-text">Max Speed (optional)</span>
            </label>
            <input
                v-model="maxSpeed"
                type="number"
                placeholder="Enter maximum speed in km/h"
                class="input input-bordered w-full"
                step="0.01"
                min="0.01"
                max="999.99"
            />
            <label class="label">
              <span class="label-text-alt">Maximum speed reached during the trip (km/h)</span>
            </label>
          </div>
        </div>
      </div>

      <div class="flex gap-2 justify-end">
        <button
            type="button"
            @click="goBack"
            class="btn btn-ghost"
        >
          Cancel
        </button>
        <button
            type="submit"
            class="btn btn-neutral"
            :disabled="loading"
        >
          {{ loading ? 'Recording...' : 'Record' }}
        </button>
      </div>
    </form>
  </div>
</template>
