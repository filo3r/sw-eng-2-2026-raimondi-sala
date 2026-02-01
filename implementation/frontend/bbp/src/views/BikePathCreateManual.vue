<script setup lang="ts">
/**
 * BikePathCreateManual page.
 * - Creates a bike path by entering origin/destination and optional waypoints.
 * - Allows adding optional obstacles with type/severity and an obstacle marker on the map.
 * - Uses Mapbox autocomplete + forward geocoding as fallback.
 * - Route preview is derived from route markers; obstacles are separate marker list.
 *
 * Fragility notes:
 * - Route addresses (`addresses`) and route markers (`routeMarkers`) must stay aligned by index.
 * - Obstacles (`obstacles`) and obstacle markers (`obstacleMarkers`) are also index-aligned; removing
 *   an obstacle shifts indices and can desync marker slots depending on your composable.
 * - Autocomplete list can disappear on input blur before click; using @mousedown prevents that.
 */

import { ref, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Plus, Trash2, ChevronDown, GripVertical } from 'lucide-vue-next'
import { createBikePathManually } from '@/services/bikePath'
import { forwardGeocode } from '@/services/mapbox'
import { useToast } from '@/composables/useToast'
import { useFieldError } from '@/composables/useFieldError'
import { useMap } from '@/composables/useMap'
import { useInteractiveMarkers } from '@/composables/useInteractiveMarkers'
import { useRouteDrawing } from '@/composables/useRouteDrawing'
import { useAddressManager } from '@/composables/useAddressManager'
import { useMapboxAutocomplete, type AutocompleteSuggestion } from '@/composables/useMapboxAutocomplete'
import { useDraggableList } from '@/composables/useDraggableList'
import { useMapClickHandler } from '@/composables/useMapClickHandler'
import { getRouteMarkerConfig } from '@/composables/useRouteMarkerConfig'
import { getMapboxApiKey } from '@/config/mapbox'
import { catchApiError } from '@/utils/error'
import { getAddressFromCoordinates } from '@/utils/geocoding'
import { validateAddresses, validateOptionalDescription, validateRequired, validateAndShow } from '@/utils/validation'
import { BIKE_PATH_STATUS_OPTIONS } from '@/constants/bikePath'
import { OBSTACLE_TYPE_OPTIONS, OBSTACLE_SEVERITY_OPTIONS } from '@/constants/obstacle'
import { DESCRIPTION_MAX_LENGTH } from '@/constants/validation'
import {
  MAP_CURSOR_CROSSHAIR,
  OBSTACLE_SEVERITY_COLORS,
  DEFAULT_OBSTACLE_COLOR,
  ROUTE_UPDATE_DEBOUNCE_MS,
  ROUTE_SOURCE_ID,
  ROUTE_LAYER_ID
} from '@/constants/map'
import type { BikePathStatus } from '@/types/bikePath'
import type { ObstacleType, ObstacleSeverity } from '@/types/obstacle'
import type { MarkerConfig } from '@/composables/useInteractiveMarkers'

type MarkerKind = 'route' | 'obstacle'

/** Router instance used for navigation (back and redirect after creation). */
const router = useRouter()

/** Toast helper for user feedback. */
const { show } = useToast()

/** Field-level error helpers used by validation and API error mapping. */
const { hasError, setError } = useFieldError()

/** Map container element reference used to mount Mapbox map. */
const mapContainer = ref<HTMLElement | null>(null)

/**
 * Map initialization via composable.
 * - interactive: true enables pan/zoom
 * - enableGeolocation: true enables geolocation control (per your composable)
 */
const { initMap, map } = useMap({
  container: mapContainer,
  accessToken: getMapboxApiKey(),
  interactive: true,
  enableGeolocation: true
})

/**
 * Route markers (origin/waypoints/destination).
 * The index in `routeMarkers` must correspond to the index in `addresses`.
 */
const {
  createMarker: createRouteMarker,
  removeMarker: removeRouteMarker,
  markers: routeMarkers
} = useInteractiveMarkers(map)

/**
 * Obstacle markers (separate list).
 * The index in `obstacleMarkers` must correspond to the index in `obstacles`.
 */
const {
  createMarker: createObstacleMarker,
  removeMarker: removeObstacleMarker,
  addSlot: addObstacleSlot,
  markers: obstacleMarkers
} = useInteractiveMarkers(map)

/**
 * Route drawing composable.
 * `updateRoute` recalculates and redraws the polyline from the route markers.
 */
const { updateRoute, attachMapClickHandler } = useRouteDrawing(map, {
  sourceId: ROUTE_SOURCE_ID,
  layerId: ROUTE_LAYER_ID
})

/** Shared Mapbox autocomplete state for all address inputs. */
const { suggestions, showSuggestions, onInput: onAutocompleteInput, onBlur: onAutocompleteBlur } =
    useMapboxAutocomplete()

/** Drag & drop state for reordering route address inputs. */
const { draggedIndex, dragOverIndex, onDragStart, onDragOver, onDragLeave, onDrop, onDragEnd } =
    useDraggableList()

/**
 * Tracks which field is active for map clicks and autocomplete.
 * activeField is expected to contain { type: 'route' | 'obstacle', index }.
 */
const { activeField, setActiveField, handleMapClick: handleMapClickBase } = useMapClickHandler()

/**
 * Route addresses in order:
 * - index 0: origin
 * - last index: destination
 * - intermediate indices: waypoints
 */
const addresses = ref<string[]>(['', ''])

/**
 * Address manager keeps route addresses and route markers synchronized.
 * It delegates marker placement to our `setMarker(...)` (wrapped here to fix type + signature).
 */
const { addAddress, removeAddress, reorderAddresses, handleRoutePointClick, handleWaypointInsert } =
    useAddressManager({
      addresses,
      routeMarkers,
      activeField,
      setActiveField: (type: string, index: number) => setActiveField(type as MarkerKind, index),
      removeRouteMarker,
      setMarker: (index, lng, lat) => setMarker('route', index, lng, lat),
      updateRoute,
      debounceMs: ROUTE_UPDATE_DEBOUNCE_MS,
      context: 'BikePathCreateManual'
    })

/** Bike path metadata fields. */
const description = ref('')
const status = ref<BikePathStatus>('GOOD')
const published = ref(false)

/** Submit state to disable the form while creating the bike path. */
const loading = ref(false)

/** Obstacle form model. */
interface ObstacleForm {
  address: string
  type: ObstacleType
  severity: ObstacleSeverity
}

/** List of obstacle forms. Index must align with obstacleMarkers. */
const obstacles = ref<ObstacleForm[]>([])

/**
 * Applies an autocomplete suggestion to either a route address or an obstacle address.
 * If coordinates are present, place/update marker immediately; otherwise forward-geocode.
 *
 * @param suggestion - Autocomplete suggestion from Mapbox composable
 * @param type - Whether we are editing a route point or an obstacle
 * @param index - Index in the corresponding array (addresses or obstacles)
 */
async function selectSuggestion(
    suggestion: AutocompleteSuggestion,
    type: MarkerKind,
    index: number
): Promise<void> {
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

/**
 * Forward-geocodes an address and sets/updates a marker.
 *
 * @param address - Address string to geocode
 * @param type - Marker kind (route/obstacle)
 * @param index - Marker index
 */
async function geocodeAndSetMarker(address: string, type: MarkerKind, index: number): Promise<void> {
  try {
    const result = await forwardGeocode({ address })
    setMarker(type, index, result.longitude, result.latitude)
  } catch (e: unknown) {
    catchApiError(e, 'BikePathCreateManual.geocodeAndSetMarker')
  }
}

/**
 * Builds marker configuration based on marker kind and index.
 * - Route markers: colored/labeled by position and update route on drag end.
 * - Obstacle markers: color depends on severity and only reverse-geocodes address on drag end.
 */
function getMarkerConfig(type: MarkerKind, index: number): MarkerConfig {
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
      await updateRoute(routeMarkers.value, ROUTE_UPDATE_DEBOUNCE_MS, 'BikePathCreateManual')
    }
  }
}

/**
 * Creates/updates a marker.
 * - For route markers, we also trigger a (debounced) route update.
 * - For obstacle markers, we only place/update the marker.
 */
function setMarker(type: MarkerKind, index: number, lng: number, lat: number): void {
  const config = getMarkerConfig(type, index)

  if (type === 'route') {
    createRouteMarker(index, lng, lat, config)
    void updateRoute(routeMarkers.value, ROUTE_UPDATE_DEBOUNCE_MS, 'BikePathCreateManual')
  } else {
    createObstacleMarker(index, lng, lat, config)
  }
}

/**
 * Map click handler:
 * Delegates to useMapClickHandler to decide whether the click is meant to:
 * - update an existing route point
 * - update an obstacle
 * - insert a waypoint before a given index
 */
function handleMapClick(e: import('mapbox-gl').MapMouseEvent): void {
  const { lng, lat } = e.lngLat

  handleMapClickBase(lng, lat, {
    getCurrentAddresses: () => addresses.value,
    onRouteClick: (index, lng, lat) => handleRoutePointClick(index, lng, lat),
    onObstacleClick: async (index, lng, lat) => {
      if (obstacles.value[index]) {
        obstacles.value[index].address = await getAddressFromCoordinates(lng, lat, 'BikePathCreateManual')
        setMarker('obstacle', index, lng, lat)
      }
    },
    onAddWaypoint: (beforeIndex, lng, lat) => handleWaypointInsert(beforeIndex, lng, lat)
  })
}

/**
 * Adds a new obstacle form and reserves a corresponding obstacle marker slot.
 * Also focuses the new obstacle address input.
 *
 * Fragility: focusing by querySelectorAll index is brittle if the DOM structure changes.
 */
async function addObstacle(): Promise<void> {
  obstacles.value.push({ address: '', type: 'POTHOLE', severity: 'LOW' })
  addObstacleSlot()

  const newIndex = obstacles.value.length - 1
  setActiveField('obstacle', newIndex)

  await nextTick()

  // Focus the newly added obstacle input (fragile DOM query).
  const inputs = document.querySelectorAll('input[type="text"]')
  const obstacleInputIndex = addresses.value.length + newIndex
  const input = inputs[obstacleInputIndex] as HTMLInputElement | undefined
  input?.focus()
}

/**
 * Removes an obstacle form and its marker.
 * Fragility: after splice, obstacle indices shift; ensure your marker composable remains aligned.
 */
function removeObstacle(index: number): void {
  removeObstacleMarker(index)
  obstacles.value.splice(index, 1)

  if (activeField.value?.type === 'obstacle' && activeField.value.index === index) {
    activeField.value = null
  }
}

/**
 * Sets bike path status and closes the dropdown by blurring the focused trigger (DaisyUI dropdown behavior). [web:190]
 */
function selectStatus(value: BikePathStatus): void {
  status.value = value
  ;(document.activeElement as HTMLElement | null)?.blur?.()
}

/**
 * Updates obstacle type and closes dropdown (blur).
 */
function selectObstacleType(index: number, value: ObstacleType): void {
  if (obstacles.value[index]) obstacles.value[index].type = value
  ;(document.activeElement as HTMLElement | null)?.blur?.()
}

/**
 * Updates obstacle severity.
 * Also re-renders the marker to update its color (by recreating/updating it at same coords).
 */
function selectObstacleSeverity(index: number, value: ObstacleSeverity): void {
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

/**
 * Submits the bike path creation request.
 * - Validates route addresses + optional description.
 * - Validates each obstacle address.
 * - Sends payload to createBikePathManually.
 */
async function handleSubmit(): Promise<void> {
  if (!validateAndShow(validateAddresses(addresses.value), 'addresses', setError, show)) return
  if (!validateAndShow(
      validateOptionalDescription(description.value, DESCRIPTION_MAX_LENGTH),
      'description',
      setError,
      show
  )) return

  // Validate obstacle addresses (each obstacle must have an address if present).
  for (let i = 0; i < obstacles.value.length; i++) {
    if (
        !validateAndShow(
            validateRequired(obstacles.value[i]?.address || '', `Obstacle ${i + 1} address`),
            `obstacles[${i}].address`,
            setError,
            show
        )
    ) return
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
  } catch (error: unknown) {
    catchApiError(error, 'BikePathCreateManual.handleSubmit', setError)
  } finally {
    loading.value = false
  }
}

/** Navigates back using browser history. */
function goBack(): void {
  router.back()
}

/**
 * Lifecycle initialization:
 * - initMap() creates the map instance.
 * - watch(map, ..., { immediate: true }) attaches the click handler as soon as map is available. [web:28]
 */
onMounted(() => {
  initMap()

  watch(
      map,
      (newMap) => {
        if (newMap) {
          attachMapClickHandler(handleMapClick)
          newMap.getCanvas().style.cursor = MAP_CURSOR_CROSSHAIR
        }
      },
      { immediate: true }
  )
})
</script>

<template>
  <div class="p-6 overflow-x-hidden">
    <div class="flex items-center gap-4 mb-6">
      <button @click="goBack" class="btn btn-ghost btn-circle shrink-0" aria-label="Go back">
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
                  aria-label="Drag to reorder"
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
                      @mousedown.prevent="selectSuggestion(suggestion, 'route', index)"
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
                  aria-label="Remove waypoint"
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
                :class="{ 'input-error': hasError('description') }"
                rows="3"
                :maxlength="DESCRIPTION_MAX_LENGTH"
            ></textarea>
            <label class="label">
              <span class="label-text-alt">{{ description.length }}/{{ DESCRIPTION_MAX_LENGTH }}</span>
            </label>
          </div>

          <div>
            <label class="label"><span class="label-text">Status</span></label>
            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
                  :class="{ 'input-error': hasError('status') }"
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
                  :class="{ 'input-error': hasError('published') }"
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
                        @mousedown.prevent="selectSuggestion(suggestion, 'obstacle', index)"
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
                        :class="{ 'input-error': hasError(`obstacles[${index}].type`) }"
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
                        :class="{ 'input-error': hasError(`obstacles[${index}].severity`) }"
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
        <button type="submit" class="btn btn-neutral" :disabled="loading" :aria-busy="loading">
          {{ loading ? 'Creating...' : 'Create' }}
        </button>
      </div>
    </form>
  </div>
</template>