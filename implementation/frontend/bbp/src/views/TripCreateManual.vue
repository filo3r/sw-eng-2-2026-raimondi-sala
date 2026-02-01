<script setup lang="ts">
/**
 * Trip manual creation page.
 * - Lets the user create a trip by specifying origin/destination and optional waypoints.
 * - Supports address autocomplete, map clicks, draggable markers, and waypoint reorder via drag & drop.
 * - Draws a route line on the map and keeps it in sync with the markers array.
 *
 * Fragility notes:
 * - The route depends on multiple composables (markers + route drawing + click handler + address manager).
 * - Keep `addresses` and `routeMarkers` aligned by index; most of the logic assumes that invariant.
 */
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Plus, Trash2, GripVertical } from 'lucide-vue-next'
import { createTripManually } from '@/services/trip'
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
import { normalizeTime } from '@/utils/date'
import {
  validateAddresses,
  validateOptionalDescription,
  validateEndAfterStart,
  validateTripDuration,
  validateOptionalMaxSpeed,
  validateAndShow
} from '@/utils/validation'
import { DESCRIPTION_MAX_LENGTH } from '@/constants/validation'
import {
  MAP_CURSOR_CROSSHAIR,
  ROUTE_UPDATE_DEBOUNCE_MS,
  TRIP_ROUTE_SOURCE_ID,
  TRIP_ROUTE_LAYER_ID
} from '@/constants/map'
import type { MarkerConfig } from '@/composables/useInteractiveMarkers'

/** Router instance used to navigate after successful creation or when going back. */
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
 * Marker manager for route points (origin, waypoints, destination).
 * `routeMarkers` must stay aligned with `addresses` indexes.
 */
const {
  createMarker: createRouteMarker,
  removeMarker: removeRouteMarker,
  markers: routeMarkers
} = useInteractiveMarkers(map)

/**
 * Route polyline drawing composable.
 * `updateRoute` recalculates route and redraws the line based on the markers list.
 */
const { updateRoute, attachMapClickHandler } = useRouteDrawing(map, {
  sourceId: TRIP_ROUTE_SOURCE_ID,
  layerId: TRIP_ROUTE_LAYER_ID
})

/** Shared Mapbox autocomplete state for inputs. */
const { suggestions, showSuggestions, onInput: onAutocompleteInput, onBlur: onAutocompleteBlur } =
    useMapboxAutocomplete()

/** Drag & drop state for reordering address inputs. */
const { draggedIndex, dragOverIndex, onDragStart, onDragOver, onDragLeave, onDrop, onDragEnd } =
    useDraggableList()

/**
 * Tracks which "field" is active for map clicks (e.g., route index).
 * `handleMapClickBase` encapsulates the logic of deciding what to do on click.
 */
const { activeField, setActiveField, handleMapClick: handleMapClickBase } = useMapClickHandler()

/**
 * Route addresses in order:
 * - index 0: origin
 * - last index: destination
 * - intermediate: waypoints
 */
const addresses = ref<string[]>(['', ''])

/**
 * Address manager keeps address list and marker list synchronized.
 * Centralizes add/remove/reorder and click-to-set-address logic.
 */
const {
  addAddress,
  removeAddress,
  reorderAddresses,
  handleRoutePointClick,
  handleWaypointInsert
} = useAddressManager({
  addresses,
  routeMarkers,
  activeField,
  setActiveField: (type: string, index: number) => setActiveField(type as 'route' | 'obstacle', index),
  removeRouteMarker,
  setMarker,
  updateRoute,
  debounceMs: ROUTE_UPDATE_DEBOUNCE_MS,
  context: 'TripCreateManual'
})

/** Optional trip description. */
const description = ref('')

/** Native date/time inputs (kept separate) */
const startDateStr = ref('') // YYYY-MM-DD
const startTimeStr = ref('') // HH:mm:ss (step=1)
const endDateStr = ref('')
const endTimeStr = ref('')

/** Optional max speed field; kept flexible for input binding. */
const maxSpeed = ref<number | ''>('')

/** Submit state to disable the form while creating the trip. */
const loading = ref(false)

/**
 * Builds a Date from native date + time strings.
 * @param dateStr - Date string in YYYY-MM-DD
 * @param timeStr - Time string in HH:mm:ss (or HH:mm normalized)
 * @returns Parsed Date, or null if inputs are missing/invalid.
 */
function toDate(dateStr: string, timeStr: string): Date | null {
  if (!dateStr || !timeStr) return null
  const d = new Date(`${dateStr}T${normalizeTime(timeStr)}`)
  return Number.isNaN(d.getTime()) ? null : d
}

/** Computed start/end Date objects (null when incomplete/invalid). */
const startTime = computed<Date | null>(() => toDate(startDateStr.value, startTimeStr.value))
const endTime = computed<Date | null>(() => toDate(endDateStr.value, endTimeStr.value))

/**
 * Native "soft" constraints for end date/time inputs.
 * - minEndDate: cannot be before start date
 * - minEndTime: if same day, cannot be before start time
 */
const minEndDate = computed(() => (startDateStr.value ? startDateStr.value : undefined))
const minEndTime = computed(() => {
  if (!startDateStr.value || !endDateStr.value) return undefined
  if (startDateStr.value !== endDateStr.value) return undefined
  const t = normalizeTime(startTimeStr.value)
  return t || undefined
})

/**
 * Applies an autocomplete suggestion to the address input at `index`.
 * If coordinates are available, set marker immediately; otherwise forward-geocode.
 * @param suggestion - Autocomplete suggestion from Mapbox composable
 * @param index - Address index to update (origin/waypoint/destination)
 */
async function selectSuggestion(suggestion: AutocompleteSuggestion, index: number): Promise<void> {
  const address = suggestion.full_address || ''
  addresses.value[index] = address
  showSuggestions.value = false

  const lng = suggestion.coordinates?.longitude
  const lat = suggestion.coordinates?.latitude

  if (typeof lng === 'number' && typeof lat === 'number') {
    setMarker(index, lng, lat)
  } else if (address) {
    await geocodeAndSetMarker(address, index)
  }
}

/**
 * Forward-geocodes an address to coordinates and places/updates the marker.
 * @param address - Human-readable address string
 * @param index - Marker/address index
 */
async function geocodeAndSetMarker(address: string, index: number): Promise<void> {
  try {
    const result = await forwardGeocode({ address })
    setMarker(index, result.longitude, result.latitude)
  } catch (e: unknown) {
    catchApiError(e, 'TripCreateManual.geocodeAndSetMarker')
  }
}

/**
 * Builds marker configuration based on index and total route points.
 * Marker behavior:
 * - draggable: true
 * - onDragEnd: reverse-geocode and update route
 * @param index - Marker index
 * @returns MarkerConfig used by useInteractiveMarkers
 */
function getMarkerConfig(index: number): MarkerConfig {
  const { color, label } = getRouteMarkerConfig(index, addresses.value.length)

  return {
    color,
    label,
    draggable: true,
    onDragEnd: async (lng, lat) => {
      addresses.value[index] = await getAddressFromCoordinates(lng, lat, 'TripCreateManual')
      await updateRoute(routeMarkers.value, ROUTE_UPDATE_DEBOUNCE_MS, 'TripCreateManual')
    }
  }
}

/**
 * Creates or updates the route marker at a given index and triggers a route redraw.
 * @param index - Marker index
 * @param lng - Longitude
 * @param lat - Latitude
 */
function setMarker(index: number, lng: number, lat: number): void {
  const config = getMarkerConfig(index)
  createRouteMarker(index, lng, lat, config)
  void updateRoute(routeMarkers.value, ROUTE_UPDATE_DEBOUNCE_MS, 'TripCreateManual')
}

/**
 * Map click handler:
 * - Delegates to useMapClickHandler which decides whether this click updates an existing point
 *   or inserts a new waypoint, based on the active field/context.
 */
function handleMapClick(e: import('mapbox-gl').MapMouseEvent): void {
  const { lng, lat } = e.lngLat

  handleMapClickBase(lng, lat, {
    getCurrentAddresses: () => addresses.value,
    onRouteClick: (index, lng, lat) => handleRoutePointClick(index, lng, lat),
    onObstacleClick: () => {},
    onAddWaypoint: (beforeIndex, lng, lat) => handleWaypointInsert(beforeIndex, lng, lat)
  })
}

/**
 * Submits the trip creation request.
 * Runs frontend validations first, then calls createTripManually().
 */
async function handleSubmit(): Promise<void> {
  // Frontend validation
  if (!validateAndShow(validateAddresses(addresses.value), 'addresses', setError, show)) return
  if (!validateAndShow(validateOptionalDescription(description.value, DESCRIPTION_MAX_LENGTH), 'description', setError, show)) return
  if (!validateAndShow(validateEndAfterStart(startTime.value, endTime.value), 'endTime', setError, show)) return
  if (!validateAndShow(validateTripDuration(startTime.value, endTime.value), 'endTime', setError, show)) return
  if (!validateAndShow(validateOptionalMaxSpeed(maxSpeed.value), 'maxSpeed', setError, show)) return

  const validAddresses = addresses.value.filter(addr => addr.trim() !== '')

  loading.value = true

  try {
    await createTripManually({
      addresses: validAddresses,
      description: description.value.trim() || undefined,
      startTime: startTime.value!.toISOString(),
      endTime: endTime.value!.toISOString(),
      maxSpeed: maxSpeed.value !== '' ? Number(maxSpeed.value) : undefined
    })

    show('Trip recorded successfully', 'success')
    await router.push('/trips/')
  } catch (error: unknown) {
    catchApiError(error, 'TripCreateManual.handleSubmit', setError)
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
              <p>
                You can drag markers to fine-tune their position and reorder waypoints via drag and drop.
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
                      @mousedown.prevent="selectSuggestion(suggestion, index)"
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
          <h2 class="card-title mb-4">Trip Details</h2>

          <div>
            <label class="label"><span class="label-text">Description (optional)</span></label>
            <textarea
                v-model="description"
                placeholder="Add notes or description"
                class="textarea textarea-bordered w-full"
                :class="{ 'input-error': hasError('description') }"
                rows="3"
                :maxlength="DESCRIPTION_MAX_LENGTH"
            ></textarea>
            <label class="label">
              <span class="label-text-alt">{{ description.length }}/{{ DESCRIPTION_MAX_LENGTH }}</span>
            </label>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="label"><span class="label-text">Start</span></label>
              <input
                  v-model="startDateStr"
                  type="date"
                  class="input input-bordered w-full"
                  :class="{ 'input-error': hasError('startTime') }"
                  required
              />
              <input
                  v-model="startTimeStr"
                  type="time"
                  step="1"
                  class="input input-bordered w-full"
                  :class="{ 'input-error': hasError('startTime') }"
                  required
              />
            </div>

            <div class="space-y-2">
              <label class="label"><span class="label-text">End</span></label>
              <input
                  v-model="endDateStr"
                  type="date"
                  class="input input-bordered w-full"
                  :class="{ 'input-error': hasError('endTime') }"
                  :min="minEndDate"
                  required
              />
              <input
                  v-model="endTimeStr"
                  type="time"
                  step="1"
                  class="input input-bordered w-full"
                  :class="{ 'input-error': hasError('endTime') }"
                  :min="minEndTime"
                  required
              />
            </div>
          </div>

          <div>
            <label class="label"><span class="label-text">Max Speed (optional)</span></label>
            <input
                v-model="maxSpeed"
                type="number"
                placeholder="Enter maximum speed in km/h"
                class="input input-bordered w-full"
                :class="{ 'input-error': hasError('maxSpeed') }"
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
        <button type="button" @click="goBack" class="btn btn-ghost">Cancel</button>
        <button type="submit" class="btn btn-neutral" :disabled="loading" :aria-busy="loading">
          {{ loading ? 'Recording...' : 'Record' }}
        </button>
      </div>
    </form>
  </div>
</template>