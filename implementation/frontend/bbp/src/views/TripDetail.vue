<script setup lang="ts">
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
import { formatDistance, formatDuration, formatSpeed, formatTemperature, formatHumidity, formatWindSpeed } from '@/utils/format'
import { formatDateTime } from '@/utils/date'
import type { TripResponse } from '@/types/trip'

const route = useRoute()
const router = useRouter()
const { show } = useToast()

const trip = ref<TripResponse | null>(null)
const loading = ref(false)
const mapContainer = ref<HTMLElement | null>(null)

const showDeleteModal = ref(false)

const { map, isReady, initMap } = useMap({
  container: mapContainer,
  accessToken: getMapboxApiKey(),
  interactive: true,
  enableGeolocation: false
})

const { drawRoute, addMarkers } = useMapRoute(map)

async function loadTrip() {
  const tripId = Number(route.params.id)

  const stateData = history.state?.trip as TripResponse | undefined

  if (stateData && stateData.id === tripId) {
    trip.value = stateData
    return
  }

  loading.value = true
  try {
    trip.value = await getTripById(tripId)
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to load trip'
    show(message, 'error')
  } finally {
    loading.value = false
  }
}

watch([isReady, trip], ([ready, tripData]) => {
  if (ready && tripData) {
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
  }
})

function goBack() {
  const from = history.state?.from

  if (from === 'Trips') {
    router.push({ name: 'Trips' })
  } else {
    router.back()
  }
}

async function handleDelete() {
  if (!trip.value) return

  try {
    await deleteTrip(trip.value.id)
    show('Trip deleted successfully', 'success')
    router.push('/trips')
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to delete trip'
    show(message, 'error')
  } finally {
    showDeleteModal.value = false
  }
}

onMounted(async () => {
  await loadTrip()
  initMap()
})
</script>

<template>
  <div class="p-6 overflow-x-hidden">
    <div v-if="loading" class="flex justify-center items-center py-12">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <div v-else-if="!trip" class="text-center py-12">
      <p class="text-gray-500">Trip not found</p>
      <button @click="goBack" class="btn btn-neutral mt-4">Go Back</button>
    </div>

    <div v-else class="space-y-6">
      <div class="space-y-4">
        <div class="flex items-start gap-4">
          <button @click="goBack" class="btn btn-ghost btn-circle shrink-0">
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
                @click="showDeleteModal = true"
                class="btn btn-sm btn-ghost text-error"
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

    <dialog :class="['modal', showDeleteModal && 'modal-open']">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">Delete Trip</h3>
        <p class="mb-6">Are you sure you want to delete this trip? This action cannot be undone.</p>
        <div class="flex gap-2 justify-end">
          <button @click="showDeleteModal = false" class="btn btn-ghost">Cancel</button>
          <button @click="handleDelete" class="btn btn-error">Delete</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="showDeleteModal = false">close</button>
      </form>
    </dialog>
  </div>
</template>
