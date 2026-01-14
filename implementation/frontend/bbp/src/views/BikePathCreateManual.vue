<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Plus, Trash2, ChevronDown } from 'lucide-vue-next'
import { createBikePathManually } from '@/services/bikePath'
import { useToast } from '@/composables/useToast'
import { getMapboxApiKey } from '@/config/mapbox'
import { useMap } from '@/composables/useMap'
import { BIKE_PATH_STATUS_OPTIONS } from '@/constants/bikePath'
import { OBSTACLE_TYPE_OPTIONS, OBSTACLE_SEVERITY_OPTIONS } from '@/constants/obstacle'
import type { BikePathStatus } from '@/types/bikePath'
import type { ObstacleType, ObstacleSeverity } from '@/types/obstacle'
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
const status = ref<BikePathStatus>('GOOD')
const published = ref(false)
const loading = ref(false)

const routeMarkers = ref<(mapboxgl.Marker | null)[]>([null, null])
const obstacleMarkers = ref<(mapboxgl.Marker | null)[]>([])

interface ObstacleForm {
  address: string
  type: ObstacleType
  severity: ObstacleSeverity
}

const obstacles = ref<ObstacleForm[]>([])

const activeField = ref<{ type: 'route' | 'obstacle', index: number } | null>(null)

function onInputFocus(type: 'route' | 'obstacle', index: number) {
  activeField.value = { type, index }
}

async function updateRoute() {
  if (!map.value) return

  const coordinates = routeMarkers.value
    .filter((m): m is mapboxgl.Marker => m !== null)
    .map(m => m.getLngLat())
  
  if (coordinates.length < 2) {
    const source = map.value.getSource('route') as mapboxgl.GeoJSONSource
    if (source) {
      source.setData({ type: 'FeatureCollection', features: [] })
    }
    return
  }

  const coordString = coordinates.map(c => `${c.lng},${c.lat}`).join(';')
  const token = getMapboxApiKey()
  
  const url = `https://api.mapbox.com/directions/v5/mapbox/cycling/${coordString}?geometries=geojson&access_token=${token}`

  try {
    const res = await fetch(url)
    const data = await res.json()

    if (data.routes && data.routes.length > 0) {
      const routeGeoJSON = data.routes[0].geometry

      const source = map.value.getSource('route') as mapboxgl.GeoJSONSource
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

function setMarker(type: 'route' | 'obstacle', index: number, lng: number, lat: number) {
  if (!map.value) return

  const markersArray = type === 'route' ? routeMarkers : obstacleMarkers
  const color = type === 'obstacle' ? '#ef4444' : '#3b82f6'

  if (markersArray.value[index]) {
    markersArray.value[index]?.remove()
  }

  const newMarker = new mapboxgl.Marker({ color, draggable: true })
    .setLngLat([lng, lat])
    .addTo(map.value)

  newMarker.on('dragend', async () => {
    const { lng, lat } = newMarker.getLngLat()
    const address = await getAddressFromCoordinates(lng, lat)
    
    if (type === 'route') {
      addresses.value[index] = address
      updateRoute() 
    } else {
      if (obstacles.value[index]) obstacles.value[index].address = address
    }
  })

  markersArray.value[index] = newMarker

  if (type === 'route') {
    updateRoute()
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

async function handleMapClick(e: mapboxgl.MapMouseEvent) {
  if (!activeField.value) return

  const { lng, lat } = e.lngLat
  const { type, index } = activeField.value

  const address = await getAddressFromCoordinates(lng, lat)
  if (type === 'route') {
    addresses.value[index] = address
  } else {
    if (obstacles.value[index]) obstacles.value[index].address = address
  }

  setMarker(type, index, lng, lat)
}

watch(map, (newMap) => {
  if (newMap) {
    newMap.on('load', () => {
      if (!newMap.getSource('route')) {
        newMap.addSource('route', {
          type: 'geojson',
          data: { type: 'FeatureCollection', features: [] }
        })

        newMap.addLayer({
          id: 'route',
          type: 'line',
          source: 'route',
          layout: {
            'line-join': 'round',
            'line-cap': 'round'
          },
          paint: {
            'line-color': '#3b82f6',
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
    
    routeMarkers.value.splice(index, 1)
    addresses.value.splice(index, 1)

    if (activeField.value?.type === 'route' && activeField.value.index === index) {
      activeField.value = null
    }

    updateRoute()
  }
}

function addObstacle() {
  obstacles.value.push({ address: '', type: 'POTHOLE', severity: 'LOW' })
  obstacleMarkers.value.push(null)
}

function removeObstacle(index: number) {
  obstacleMarkers.value[index]?.remove()
  obstacleMarkers.value.splice(index, 1)
  obstacles.value.splice(index, 1)

  if (activeField.value?.type === 'obstacle' && activeField.value.index === index) {
    activeField.value = null
  }
}

function selectStatus(value: BikePathStatus) {
  status.value = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function selectObstacleType(index: number, value: ObstacleType) {
  if (obstacles.value[index]) obstacles.value[index].type = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function selectObstacleSeverity(index: number, value: ObstacleSeverity) {
  if (obstacles.value[index]) obstacles.value[index].severity = value
  ;(document.activeElement as HTMLElement)?.blur()
}

async function handleSubmit() {
  const validAddresses = addresses.value.filter(addr => addr.trim() !== '')

  if (validAddresses.length < 2) {
    show('At least 2 addresses are required', 'error')
    return
  }

  for (let i = 0; i < obstacles.value.length; i++) {
    if (!obstacles.value[i].address.trim()) {
      show(`Obstacle ${i + 1}: Address is required`, 'error')
      return
    }
  }

  loading.value = true

  try {
    const bikePathId = await createBikePathManually({
      addresses: validAddresses,
      description: description.value || undefined,
      status: status.value,
      published: published.value,
      obstacles: obstacles.value.map(o => ({
        address: o.address,
        type: o.type,
        severity: o.severity
      }))
    })

    show('Bike path created successfully', 'success')
    router.push(`/bike-paths/`)
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to create bike path'
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
      <h1 class="text-3xl font-bold">Create Bike Path</h1>
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
                    :class="{ 'input-primary border-2': activeField?.type === 'route' && activeField?.index === index }"
                    @focus="onInputFocus('route', index)"
                    readonly
                    required
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

          <button type="button" @click="addAddress" class="btn btn-ghost btn-sm mt-4">
            <Plus :size="16" />
            Add Waypoint
          </button>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">Details</h2>
          
          <div>
            <label class="label"><span class="label-text">Description (optional)</span></label>
            <textarea
                v-model="description"
                placeholder="Add notes..."
                class="textarea textarea-bordered w-full"
                rows="3"
                maxlength="500"
            ></textarea>
          </div>

          <div>
            <label class="label"><span class="label-text">Status</span></label>
            <div class="dropdown w-full">
              <div tabindex="0" role="button" class="btn btn-bordered w-full justify-between font-normal">
                {{ BIKE_PATH_STATUS_OPTIONS.find(o => o.value === status)?.label || 'Select status' }}
                <ChevronDown :size="16" />
              </div>
              <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-10 w-full p-2 shadow max-h-60 overflow-y-auto">
                <li v-for="option in BIKE_PATH_STATUS_OPTIONS" :key="option.value">
                  <a @click="selectStatus(option.value)">{{ option.label }}</a>
                </li>
              </ul>
            </div>
          </div>

          <div class="form-control">
            <label class="label cursor-pointer justify-start gap-4">
              <input v-model="published" type="checkbox" class="checkbox" />
              <span class="block">
                <span class="label-text font-medium block">Public</span>
                <span class="text-sm text-gray-500 block">Visible to everyone</span>
              </span>
            </label>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title">Obstacles (optional)</h2>
            <button type="button" @click="addObstacle" class="btn btn-sm btn-ghost">
              <Plus :size="16" /> Add Obstacle
            </button>
          </div>

          <div v-if="obstacles.length === 0" class="text-center text-gray-500 py-4 text-sm">
            No obstacles added
          </div>

          <div v-else class="space-y-6">
            <div v-for="(obstacle, index) in obstacles" :key="index" class="p-4 border rounded-lg space-y-4">
              <div class="flex items-center justify-between">
                <h3 class="font-medium">Obstacle {{ index + 1 }}</h3>
                <button type="button" @click="removeObstacle(index)" class="btn btn-ghost btn-sm text-error">
                  <Trash2 :size="16" />
                </button>
              </div>

              <div>
                <label class="label"><span class="label-text">Address</span></label>
                <input
                    v-model="obstacle.address"
                    type="text"
                    placeholder="Location or click on map"
                    class="input input-bordered w-full transition-colors"
                    :class="{ 'input-primary border-2': activeField?.type === 'obstacle' && activeField?.index === index }"
                    @focus="onInputFocus('obstacle', index)"
                    readonly
                    required
                />
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label class="label"><span class="label-text">Type</span></label>
                  <div class="dropdown w-full">
                    <div tabindex="0" role="button" class="btn btn-bordered w-full justify-between font-normal">
                      {{ OBSTACLE_TYPE_OPTIONS.find(o => o.value === obstacle.type)?.label }}
                      <ChevronDown :size="16" />
                    </div>
                    <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-10 w-full p-2 shadow">
                      <li v-for="option in OBSTACLE_TYPE_OPTIONS" :key="option.value">
                        <a @click="selectObstacleType(index, option.value)">{{ option.label }}</a>
                      </li>
                    </ul>
                  </div>
                </div>

                <div>
                  <label class="label"><span class="label-text">Severity</span></label>
                  <div class="dropdown w-full">
                    <div tabindex="0" role="button" class="btn btn-bordered w-full justify-between font-normal">
                      {{ OBSTACLE_SEVERITY_OPTIONS.find(o => o.value === obstacle.severity)?.label }}
                      <ChevronDown :size="16" />
                    </div>
                    <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow">
                      <li v-for="option in OBSTACLE_SEVERITY_OPTIONS" :key="option.value">
                        <a @click="selectObstacleSeverity(index, option.value)">{{ option.label }}</a>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-2 justify-end">
        <button type="button" @click="goBack" class="btn btn-ghost">Cancel</button>
        <button type="submit" class="btn btn-neutral" :disabled="loading">
          {{ loading ? 'Creating...' : 'Create' }}
        </button>
      </div>
    </form>
  </div>
</template>