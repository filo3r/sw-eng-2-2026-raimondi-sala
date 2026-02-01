<script setup lang="ts">
/**
 * Trip detail page.
 * - Loads a single trip (from router state cache when available, otherwise via API).
 * - Initializes an interactive Mapbox map and draws the route + origin/destination markers.
 * - Allows deleting the trip with a confirmation modal.
 */
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Trash2,
  MapPin,
  Calendar,
  Bike,
  Clock,
  Thermometer,
  Droplets,
  Wind,
  CloudSunRain
} from 'lucide-vue-next'
import { getTripById, deleteTrip } from '@/services/trip'
import { getMapboxApiKey } from '@/config/mapbox'
import { useMap } from '@/composables/useMap'
import { useMapRoute } from '@/composables/useMapRoute'
import { useToast } from '@/composables/useToast'
import { useAsyncState } from '@/composables/useAsyncState'
import {
  formatDistance,
  formatDuration,
  formatSpeed,
  formatTemperature,
  formatHumidity,
  formatWindSpeed
} from '@/utils/format'
import { formatDateTime } from '@/utils/date'
import { SPINNER_DELAY_MS } from '@/constants/ui'
import type { TripResponse } from '@/types/trip'

/** Router utilities for reading route params and navigating back. */
const route = useRoute()
const router = useRouter()

/** Global toast helper for success/error feedback. */
const { show } = useToast()

/** Async state for loading the trip entity. */
const { data: trip, execute } = useAsyncState<TripResponse>()

/** Async state for delete operation (kept separate for clearer logs/contexts). */
const { execute: executeDelete } = useAsyncState<void>()

/** UI state: disables destructive actions while delete request is in flight. */
const deleting = ref(false)

/** Map container DOM reference (required to initialize Mapbox map instance). */
const mapContainer = ref<HTMLElement | null>(null)

/** Delete confirmation modal driven by native <dialog>. */
const deleteDialog = ref<HTMLDialogElement | null>(null)
const isDeleteModalOpen = ref(false)

/**
 * Spinner state with delayed activation to prevent "flash" on fast responses.
 * We show the spinner only if the request takes longer than SPINNER_DELAY_MS.
 */
const showSpinner = ref(false)
let spinnerTimeout: ReturnType<typeof window.setTimeout> | null = null

/**
 * Mapbox access token cached once.
 * Avoids repeated reads of env/config while rendering.
 */
const mapboxToken = getMapboxApiKey()

/**
 * Map composable:
 * - interactive: true enables pan/zoom.
 * - enableGeolocation: false disables user-location features for this view.
 */
const { map, isReady, initMap } = useMap({
  container: mapContainer,
  accessToken: mapboxToken,
  interactive: true,
  enableGeolocation: false
})

/** Route drawing utilities bound to the map instance. */
const { drawRoute, addMarkers } = useMapRoute(map)

/**
 * Loads the trip details.
 * Optimization: if router history state includes a matching trip object, use it and skip the network call.
 * @returns Promise that resolves when trip state is ready (or determined missing).
 */
async function loadTrip(): Promise<void> {
  const tripId = Number(route.params.id)

  // Router "state" cache (set by the list page when navigating to detail).
  const stateData = history.state?.trip as TripResponse | undefined
  if (stateData && stateData.id === tripId) {
    trip.value = stateData
    return
  }

  // Show spinner only if the request isn't quick.
  spinnerTimeout = window.setTimeout(() => {
    showSpinner.value = true
  }, SPINNER_DELAY_MS)

  await execute(() => getTripById(tripId), 'TripDetail.loadTrip')

  if (spinnerTimeout) {
    clearTimeout(spinnerTimeout)
    spinnerTimeout = null
  }
  showSpinner.value = false
}

/**
 * Watches both the map readiness and the trip payload.
 * When both are available, draw the route polyline and add origin/destination markers.
 */
watch([isReady, trip], ([ready, tripData]) => {
  if (!ready || !tripData) return

  drawRoute(tripData.tripPoints)
  addMarkers(
      {
        address: tripData.origin,
        latitude: tripData.originLatitude,
        longitude: tripData.originLongitude
      },
      {
        address: tripData.destination,
        latitude: tripData.destinationLatitude,
        longitude: tripData.destinationLongitude
      }
  )
})

/**
 * Navigates back to the trips list.
 */
function goBack(): void {
  router.push('/trips')
}

/**
 * Opens the delete confirmation modal via native dialog API.
 */
function openDeleteModal(): void {
  if (isDeleteModalOpen.value) return
  deleteDialog.value?.showModal()
  isDeleteModalOpen.value = true
}

/**
 * Closes the delete confirmation modal and resets related UI state.
 */
function closeDeleteModal(): void {
  if (!isDeleteModalOpen.value) return
  deleteDialog.value?.close()
  isDeleteModalOpen.value = false
}

/**
 * Deletes the current trip.
 * - Closes the modal immediately for responsive UX.
 * - Performs API delete; on success, shows a toast and navigates back to list.
 */
async function handleDelete(): Promise<void> {
  if (!trip.value) return

  closeDeleteModal()
  deleting.value = true

  try {
    await executeDelete(
        () => deleteTrip(trip.value!.id),
        'TripDetail.handleDelete',
        async () => {
          show('Trip deleted successfully', 'success')
          await router.push('/trips')
        }
    )
  } finally {
    deleting.value = false
  }
}

/**
 * Lifecycle initialization:
 * - Loads trip data (may come from history cache).
 * - Initializes the map once the container element exists.
 */
onMounted(async () => {
  await loadTrip()
  initMap()
})
</script>

<template>
  <div v-if="showSpinner" class="flex h-screen items-center justify-center">
    <span class="loading loading-spinner loading-lg"></span>
  </div>

  <div v-else-if="!trip" class="p-6 text-center py-12">
    <p class="text-gray-500">Trip not found</p>
    <button @click="goBack" class="btn btn-neutral mt-4">Go Back</button>
  </div>

  <div v-else class="p-6 overflow-x-hidden">
    <div class="space-y-6">
      <div class="space-y-4">
        <div class="flex items-start gap-4">
          <button @click="goBack" class="btn btn-ghost btn-circle shrink-0" aria-label="Back to trips">
            <ArrowLeft :size="20" />
          </button>
          <h1 class="text-3xl font-bold wrap-break-word flex-1">
            {{ trip.origin }} → {{ trip.destination }}
          </h1>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body p-0">
          <div ref="mapContainer" class="w-full h-96 rounded-lg"></div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title">Trip Information</h2>
            <button
                @click="openDeleteModal"
                class="btn btn-sm btn-ghost text-error"
                aria-label="Delete trip"
            >
              <Trash2 :size="16" />
            </button>
          </div>

          <div class="flex flex-wrap gap-4 mb-6">
            <div class="flex items-start gap-2 flex-1 min-w-[250px]">
              <MapPin :size="18" class="text-success shrink-0 mt-0.5" />
              <div class="flex-1">
                <span class="font-medium text-sm text-gray-500">From:</span>
                <p class="text-gray-700">{{ trip.origin }}</p>
              </div>
            </div>
            <div class="flex items-start gap-2 flex-1 min-w-[250px]">
              <MapPin :size="18" class="text-purple-500 shrink-0 mt-0.5" />
              <div class="flex-1">
                <span class="font-medium text-sm text-gray-500">To:</span>
                <p class="text-gray-700">{{ trip.destination }}</p>
              </div>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="flex items-center gap-2">
              <Calendar :size="18" />
              <span class="font-medium">Started:</span>
              <span>{{ formatDateTime(trip.startTime) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Calendar :size="18" />
              <span class="font-medium">Ended:</span>
              <span>{{ formatDateTime(trip.endTime) }}</span>
            </div>

            <div class="flex items-center gap-2">
              <Bike :size="18" />
              <span class="font-medium">Distance:</span>
              <span>{{ formatDistance(trip.totalDistance) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Clock :size="18" />
              <span class="font-medium">Duration:</span>
              <span>{{ formatDuration(trip.totalDuration) }}</span>
            </div>

            <div class="flex items-center gap-2">
              <Bike :size="18" />
              <span class="font-medium">Avg Speed:</span>
              <span>{{ formatSpeed(trip.averageSpeed) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Bike :size="18" />
              <span class="font-medium">Max Speed:</span>
              <span>{{ trip.maxSpeed ? formatSpeed(trip.maxSpeed) : 'Not available' }}</span>
            </div>
          </div>

          <div class="mt-6">
            <label class="block font-medium mb-2">Description</label>
            <p v-if="trip.description" class="text-gray-700">{{ trip.description }}</p>
            <p v-else class="text-gray-500 italic">No description</p>
          </div>
        </div>
      </div>

      <div v-if="trip.meteorologicalData" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">Weather Conditions</h2>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="space-y-4">
              <div class="flex items-center gap-2">
                <Thermometer :size="18" />
                <span class="font-medium">Temperature:</span>
                <span>{{ formatTemperature(trip.meteorologicalData.temperature) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Droplets :size="18" />
                <span class="font-medium">Humidity:</span>
                <span>{{ formatHumidity(trip.meteorologicalData.humidity) }}</span>
              </div>
            </div>

            <div class="space-y-4">
              <div class="flex items-center gap-2">
                <Wind :size="18" />
                <span class="font-medium">Wind Speed:</span>
                <span>{{ formatWindSpeed(trip.meteorologicalData.windSpeed) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <CloudSunRain :size="18" />
                <span class="font-medium">Condition:</span>
                <span>{{ trip.meteorologicalData.weatherDescription }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <dialog ref="deleteDialog" class="modal" @close="isDeleteModalOpen = false">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">Delete Trip</h3>
        <p class="mb-6">Are you sure you want to delete this trip? This action cannot be undone.</p>
        <div class="flex gap-2 justify-end">
          <button @click="closeDeleteModal" class="btn btn-ghost">Cancel</button>
          <button @click="handleDelete" class="btn btn-error" :disabled="deleting">
            {{ deleting ? 'Deleting...' : 'Delete' }}
          </button>
        </div>
      </div>

      <form method="dialog" class="modal-backdrop">
        <button @click="closeDeleteModal">close</button>
      </form>
    </dialog>
  </div>
</template>