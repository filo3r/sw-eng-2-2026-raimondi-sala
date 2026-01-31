<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Plus, Trash2, ChevronDown, GripVertical } from 'lucide-vue-next'
import { createBikePathManually } from '@/services/bikePath'
import { forwardGeocode, calculateCyclingRoute } from '@/services/mapbox'
import { useToast } from '@/composables/useToast'
import { useFieldError } from '@/composables/useFieldError'
import { useMap } from '@/composables/useMap'
import { useInteractiveMarkers } from '@/composables/useInteractiveMarkers'
import { useMapboxAutocomplete, type AutocompleteSuggestion } from '@/composables/useMapboxAutocomplete'
import { useDraggableList } from '@/composables/useDraggableList'
import { useMapClickHandler } from '@/composables/useMapClickHandler'
import { getRouteMarkerConfig } from '@/composables/useRouteMarkerConfig'
import { getMapboxApiKey } from '@/config/mapbox'
import { catchApiError } from '@/utils/error'
import { getAddressFromCoordinates } from '@/utils/geocoding'
import {
  validateAddresses,
  validateOptionalDescription,
  validateRequired,
  validateAndShow
} from '@/utils/validation'
import { BIKE_PATH_STATUS_OPTIONS } from '@/constants/bikePath'
import { OBSTACLE_TYPE_OPTIONS, OBSTACLE_SEVERITY_OPTIONS } from '@/constants/obstacle'
import { DESCRIPTION_MAX_LENGTH } from '@/constants/validation'
import {
  MAP_CURSOR_CROSSHAIR,
  ROUTE_LINE_COLOR,
  ROUTE_LINE_WIDTH,
  ROUTE_LINE_JOIN,
  ROUTE_LINE_CAP,
  OBSTACLE_SEVERITY_COLORS,
  DEFAULT_OBSTACLE_COLOR
} from '@/constants/map'
import type { BikePathStatus } from '@/types/bikePath'
import type { ObstacleType, ObstacleSeverity } from '@/types/obstacle'
import type { Coordinate } from '@/types/mapbox'
import type { MarkerConfig } from '@/composables/useInteractiveMarkers'

const router = useRouter()
const { show } = useToast()
const { hasError, setError } = useFieldError()

const mapContainer = ref<HTMLElement | null>(null)
const { initMap, map } = useMap({
  container: mapContainer,
  accessToken: getMapboxApiKey(),
  interactive: true,
  enableGeolocation: true
})

const {
  createMarker: createRouteMarker,
  removeMarker: removeRouteMarker,
  markers: routeMarkers
} = useInteractiveMarkers(map)

const {
  createMarker: createObstacleMarker,
  removeMarker: removeObstacleMarker,
  addSlot: addObstacleSlot,
  markers: obstacleMarkers
} = useInteractiveMarkers(map)

const { suggestions, showSuggestions, onInput: onAutocompleteInput, onBlur: onAutocompleteBlur } =
    useMapboxAutocomplete()

const { draggedIndex, dragOverIndex, onDragStart, onDragOver, onDragLeave, onDrop, onDragEnd } =
    useDraggableList()

const { activeField, setActiveField, handleMapClick: handleMapClickBase } = useMapClickHandler()

const addresses = ref<string[]>(['', ''])
const description = ref('')
const status = ref<BikePathStatus>('GOOD')
const published = ref(false)
const loading = ref(false)

interface ObstacleForm {
  address: string
  type: ObstacleType
  severity: ObstacleSeverity
}

const obstacles = ref<ObstacleForm[]>([])

async function selectSuggestion(
    suggestion: AutocompleteSuggestion,
    type: 'route' | 'obstacle',
    index: number
) {
  const address = suggestion.full_address || ''

  if (type === 'route') {
    addresses.value[index] = address
  } else if (obstacles.value[index]) {
    obstacles.value[index].address = address
  }

  showSuggestions.value = false

  const lng = suggestion.coordinates?.longitude
  const lat = suggestion.coordinates?.latitude

  if (typeof lng === 'number' && typeof lat === 'number') {
    setMarker(type, index, lng, lat)
  } else if (address) {
    await geocodeAndSetMarker(address, type, index)
  }
}

async function geocodeAndSetMarker(address: string, type: 'route' | 'obstacle', index: number) {
  try {
    const result = await forwardGeocode({ address })
    setMarker(type, index, result.longitude, result.latitude)
  } catch (e) {
    catchApiError(e, 'BikePathCreateManual.geocodeAndSetMarker')
  }
}

async function updateRoute() {
  if (!map.value) return

  const coordinates = routeMarkers.value
      .filter((m): m is import('mapbox-gl').Marker => m !== null)
      .map(m => m.getLngLat())

  if (coordinates.length < 2) {
    const source = map.value.getSource('route') as import('mapbox-gl').GeoJSONSource | undefined
    if (source) source.setData({ type: 'FeatureCollection', features: [] })
    return
  }

  try {
    const waypoints: Coordinate[] = coordinates.map(c => ({
      latitude: c.lat,
      longitude: c.lng
    }))

    const routeResult = await calculateCyclingRoute({ waypoints })

    const routeCoordinates = routeResult.points
        .sort((a, b) => a.sequentialPosition - b.sequentialPosition)
        .map(point => [point.longitude, point.latitude])

    const source = map.value.getSource('route') as import('mapbox-gl').GeoJSONSource | undefined
    if (source) {
      source.setData({
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: routeCoordinates
        }
      })
    }
  } catch (e) {
    catchApiError(e, 'BikePathCreateManual.updateRoute')
  }
}

function getMarkerConfig(type: 'route' | 'obstacle', index: number): MarkerConfig {
  if (type === 'obstacle' && obstacles.value[index]) {
    const severity = obstacles.value[index].severity
    return {
      color: OBSTACLE_SEVERITY_COLORS[severity] || DEFAULT_OBSTACLE_COLOR,
      label: '!',
      draggable: true,
      onDragEnd: async (lng, lat) => {
        if (obstacles.value[index]) {
          obstacles.value[index].address = await getAddressFromCoordinates(lng, lat, 'BikePathCreateManual')
        }
      }
    }
  }

  const { color, label } = getRouteMarkerConfig(index, addresses.value.length)

  return {
    color,
    label,
    draggable: true,
    onDragEnd: async (lng, lat) => {
      addresses.value[index] = await getAddressFromCoordinates(lng, lat, 'BikePathCreateManual')
      await updateRoute()
    }
  }
}

function setMarker(type: 'route' | 'obstacle', index: number, lng: number, lat: number) {
  const config = getMarkerConfig(type, index)

  if (type === 'route') {
    createRouteMarker(index, lng, lat, config)
    void updateRoute()
  } else {
    createObstacleMarker(index, lng, lat, config)
  }
}

function handleMapClick(e: import('mapbox-gl').MapMouseEvent) {
  const { lng, lat } = e.lngLat

  handleMapClickBase(lng, lat, {
    getCurrentAddresses: () => addresses.value,
    onRouteClick: async (index, lng, lat) => {
      addresses.value[index] = await getAddressFromCoordinates(lng, lat, 'BikePathCreateManual')

      // Sincronizza routeMarkers: inserisci slot se necessario
      while (routeMarkers.value.length <= index) {
        routeMarkers.value.push(null)
      }

      // Se l'indice Ã¨ nel mezzo (waypoint inserito), aggiungi slot alla posizione corretta
      if (routeMarkers.value.length === addresses.value.length - 1 && index < routeMarkers.value.length) {
        routeMarkers.value.splice(index, 0, null)
      }

      setMarker('route', index, lng, lat)
      redrawRouteMarkers()
    },
    onObstacleClick: async (index, lng, lat) => {
      if (obstacles.value[index]) {
        obstacles.value[index].address = await getAddressFromCoordinates(lng, lat, 'BikePathCreateManual')
        setMarker('obstacle', index, lng, lat)
      }
    },
    onAddWaypoint: async (beforeIndex, lng, lat) => {
      addresses.value.splice(beforeIndex, 0, await getAddressFromCoordinates(lng, lat, 'BikePathCreateManual'))
      routeMarkers.value.splice(beforeIndex, 0, null)
      setMarker('route', beforeIndex, lng, lat)
      redrawRouteMarkers()
    }
  })
}

watch(map, newMap => {
  if (!newMap) return

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
          'line-join': ROUTE_LINE_JOIN,
          'line-cap': ROUTE_LINE_CAP
        },
        paint: {
          'line-color': ROUTE_LINE_COLOR,
          'line-width': ROUTE_LINE_WIDTH
        }
      })
    }
  })

  newMap.on('click', handleMapClick)
  newMap.getCanvas().style.cursor = MAP_CURSOR_CROSSHAIR
})

async function addAddress() {
  // Inserisci il nuovo waypoint prima della destinazione
  const insertIndex = addresses.value.length - 1
  addresses.value.splice(insertIndex, 0, '')

  // Inserisci anche uno slot marker alla stessa posizione
  routeMarkers.value.splice(insertIndex, 0, null)

  setActiveField('route', insertIndex)

  await nextTick()

  // Focus sull'input del nuovo waypoint
  const inputs = document.querySelectorAll('input[type="text"]')
  const input = inputs[insertIndex] as HTMLInputElement
  input?.focus()

  redrawRouteMarkers()
}

function removeAddress(index: number) {
  if (addresses.value.length > 2) {
    removeRouteMarker(index)
    addresses.value.splice(index, 1)
    routeMarkers.value.splice(index, 1)

    if (activeField.value?.type === 'route' && activeField.value.index === index) {
      activeField.value = null
    }

    redrawRouteMarkers()
    void updateRoute()
  }
}

function reorderAddresses(fromIndex: number, toIndex: number) {
  const [movedAddress = ''] = addresses.value.splice(fromIndex, 1)
  addresses.value.splice(toIndex, 0, movedAddress)

  const [movedMarker = null] = routeMarkers.value.splice(fromIndex, 1)
  routeMarkers.value.splice(toIndex, 0, movedMarker)

  redrawRouteMarkers()
  void updateRoute()
}

function redrawRouteMarkers() {
  routeMarkers.value.forEach((marker, index) => {
    if (marker) {
      const { lng, lat } = marker.getLngLat()
      setMarker('route', index, lng, lat)
    }
  })
}

async function addObstacle() {
  obstacles.value.push({ address: '', type: 'POTHOLE', severity: 'LOW' })
  addObstacleSlot()

  const newIndex = obstacles.value.length - 1
  setActiveField('obstacle', newIndex)

  await nextTick()

  // Focus sull'input del nuovo ostacolo
  const inputs = document.querySelectorAll('input[type="text"]')
  const obstacleInputIndex = addresses.value.length + newIndex
  const input = inputs[obstacleInputIndex] as HTMLInputElement
  input?.focus()
}

function removeObstacle(index: number) {
  removeObstacleMarker(index)
  obstacles.value.splice(index, 1)

  if (activeField.value?.type === 'obstacle' && activeField.value.index === index) {
    activeField.value = null
  }
}

function selectStatus(value: BikePathStatus) {
  status.value = value
  ;(document.activeElement as HTMLElement | null)?.blur?.()
}

function selectObstacleType(index: number, value: ObstacleType) {
  if (obstacles.value[index]) {
    obstacles.value[index].type = value
  }
  ;(document.activeElement as HTMLElement | null)?.blur?.()
}

function selectObstacleSeverity(index: number, value: ObstacleSeverity) {
  if (obstacles.value[index]) {
    obstacles.value[index].severity = value

    const marker = obstacleMarkers.value[index]
    if (marker) {
      const { lng, lat } = marker.getLngLat()
      setMarker('obstacle', index, lng, lat)
    }
  }
  ;(document.activeElement as HTMLElement | null)?.blur?.()
}

async function handleSubmit() {
  // Frontend validation
  if (!validateAndShow(validateAddresses(addresses.value), 'addresses', setError, show)) return
  if (!validateAndShow(validateOptionalDescription(description.value, DESCRIPTION_MAX_LENGTH), 'description', setError, show)) return

  // Validate obstacle addresses
  for (let i = 0; i < obstacles.value.length; i++) {
    if (!validateAndShow(
        validateRequired(obstacles.value[i]?.address || '', `Obstacle ${i + 1} address`),
        `obstacles[${i}].address`,
        setError,
        show
    )) return
  }

  const validAddresses = addresses.value.filter(addr => addr.trim() !== '')

  loading.value = true

  try {
    await createBikePathManually({
      addresses: validAddresses,
      description: description.value.trim() || undefined,
      status: status.value,
      published: published.value,
      obstacles: obstacles.value.map(o => ({
        address: o.address,
        type: o.type,
        severity: o.severity
      }))
    })

    show('Bike path created successfully', 'success')
    await router.push('/bike-paths/')
  } catch (error: any) {
    catchApiError(error, 'BikePathCreateManual.handleSubmit', setError)
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
          <h2 class="card-title mb-2">Route Addresses</h2>

          <div class="collapse collapse-arrow bg-base-200 mb-4">
            <input type="checkbox" />
            <div class="collapse-title text-sm font-medium">How does it work?</div>
            <div class="collapse-content text-sm">
              <p class="mb-2">
                Start by entering only the Origin and Destination and check the route preview. If it doesn't match what you want, add one or more intermediate waypoints to guide the route.
              </p>
              <p class="mb-2">
                You can add waypoints in order by clicking on the map or typing the address.
              </p>
              <p class="mb-2">
                You can drag markers to fine-tune their position and reorder waypoints via drag and drop.
              </p>
              <p>
                Note: the route preview is an estimate and may differ slightly from the saved route due to address-to-coordinate conversion. If you need higher accuracy, we recommend using the automatic recording mode.
              </p>
            </div>
          </div>

          <div class="space-y-4">
            <div
                v-for="(_address, index) in addresses"
                :key="index"
                class="flex items-start gap-2 transition-all"
                :class="{
                'opacity-50': draggedIndex === index,
                'border-t-2 border-primary':
                  dragOverIndex === index && draggedIndex !== null && draggedIndex !== index
              }"
                @dragover="(e) => onDragOver(e, index)"
                @dragleave="onDragLeave"
                @drop="onDrop(index, reorderAddresses)"
            >
              <div
                  class="cursor-move mt-9 text-gray-400 hover:text-gray-600"
                  draggable="true"
                  @dragstart="onDragStart(index)"
                  @dragend="onDragEnd"
              >
                <GripVertical :size="20" />
              </div>

              <div class="flex-1 relative">
                <label class="label">
                  <span class="label-text">
                    {{
                      index === 0
                          ? 'Origin'
                          : index === addresses.length - 1
                              ? 'Destination'
                              : `Waypoint ${index}`
                    }}
                  </span>
                </label>

                <input
                    v-model="addresses[index]"
                    type="text"
                    placeholder="Type address or click on map"
                    class="input input-bordered w-full transition-colors"
                    :class="{
                    'input-primary border-2':
                      activeField?.type === 'route' && activeField?.index === index
                  }"
                    @focus="setActiveField('route', index)"
                    @input="onAutocompleteInput(($event.target as HTMLInputElement).value)"
                    @blur="onAutocompleteBlur"
                    required
                />

                <div
                    v-if="
                    showSuggestions &&
                    activeField?.type === 'route' &&
                    activeField?.index === index &&
                    suggestions.length > 0
                  "
                    class="absolute z-50 w-full mt-1 bg-base-100 border border-base-300 rounded-lg shadow-lg max-h-60 overflow-y-auto"
                >
                  <div
                      v-for="(suggestion, i) in suggestions"
                      :key="i"
                      @click="selectSuggestion(suggestion, 'route', index)"
                      class="px-4 py-2 hover:bg-base-200 cursor-pointer border-b border-base-200 last:border-0"
                  >
                    <div class="font-medium">{{ suggestion.name }}</div>
                    <div class="text-sm text-gray-500">{{ suggestion.full_address }}</div>
                  </div>
                </div>
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
                :class="{'input-error': hasError('description')}"
                rows="3"
                :maxlength="DESCRIPTION_MAX_LENGTH"
            ></textarea>
            <label class="label"><span class="label-text-alt">{{ description.length }}/{{ DESCRIPTION_MAX_LENGTH }}</span></label>
          </div>

          <div>
            <label class="label"><span class="label-text">Status</span></label>
            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
                  :class="{'input-error': hasError('status')}"
              >
                {{ BIKE_PATH_STATUS_OPTIONS.find(o => o.value === status)?.label || 'Select status' }}
                <ChevronDown :size="16" />
              </div>
              <ul
                  tabindex="0"
                  class="dropdown-content menu bg-base-100 rounded-box z-10 w-full p-2 shadow max-h-60 overflow-y-auto"
              >
                <li v-for="option in BIKE_PATH_STATUS_OPTIONS" :key="option.value">
                  <a @click="selectStatus(option.value)">{{ option.label }}</a>
                </li>
              </ul>
            </div>
          </div>

          <div class="form-control">
            <label class="label cursor-pointer justify-start gap-4">
              <input
                  v-model="published"
                  type="checkbox"
                  class="checkbox"
                  :class="{'input-error': hasError('published')}"
              />
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

          <div v-if="obstacles.length > 0" class="collapse collapse-arrow bg-base-200 mb-4">
            <input type="checkbox" />
            <div class="collapse-title text-sm font-medium">How does it work?</div>
            <div class="collapse-content text-sm">
              <p>
                Click on the map to position the obstacle or type the address. You can drag the marker to adjust position.
              </p>
            </div>
          </div>

          <div v-if="obstacles.length === 0" class="text-center text-gray-500 py-4 text-sm">No obstacles added</div>

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
                <div class="relative">
                  <input
                      v-model="obstacle.address"
                      type="text"
                      placeholder="Type address or click on map"
                      class="input input-bordered w-full transition-colors"
                      :class="{
                      'input-primary border-2':
                        activeField?.type === 'obstacle' && activeField?.index === index,
                      'input-error': hasError(`obstacles[${index}].address`)
                    }"
                      @focus="setActiveField('obstacle', index)"
                      @input="onAutocompleteInput(($event.target as HTMLInputElement).value)"
                      @blur="onAutocompleteBlur"
                      required
                  />

                  <div
                      v-if="
                      showSuggestions &&
                      activeField?.type === 'obstacle' &&
                      activeField?.index === index &&
                      suggestions.length > 0
                    "
                      class="absolute z-50 w-full mt-1 bg-base-100 border border-base-300 rounded-lg shadow-lg max-h-60 overflow-y-auto"
                  >
                    <div
                        v-for="(suggestion, i) in suggestions"
                        :key="i"
                        @click="selectSuggestion(suggestion, 'obstacle', index)"
                        class="px-4 py-2 hover:bg-base-200 cursor-pointer border-b border-base-200 last:border-0"
                    >
                      <div class="font-medium">{{ suggestion.name }}</div>
                      <div class="text-sm text-gray-500">{{ suggestion.full_address }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label class="label"><span class="label-text">Type</span></label>
                  <div class="dropdown w-full">
                    <div
                        tabindex="0"
                        role="button"
                        class="btn btn-bordered w-full justify-between font-normal"
                        :class="{'input-error': hasError(`obstacles[${index}].type`)}"
                    >
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
                    <div
                        tabindex="0"
                        role="button"
                        class="btn btn-bordered w-full justify-between font-normal"
                        :class="{'input-error': hasError(`obstacles[${index}].severity`)}"
                    >
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