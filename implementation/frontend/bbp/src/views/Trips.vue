<script setup lang="ts">
import { ref, onMounted, toRaw } from 'vue'
import { useRouter } from 'vue-router'
import { Filter, Plus, X, Search, Eraser, MapPin, Clock } from 'lucide-vue-next'
import { getUserTrips, searchTrips } from '@/services/trip'
import { getMapboxApiKey } from '@/config/mapbox'
import { generateStaticMapUrl } from '@/utils/mapStatic'
import { useToast } from '@/composables/useToast'
import { formatDistance, formatDuration } from '@/utils/format'
import { formatDateRange } from '@/utils/date'
import type { TripResponse } from '@/types/trip'

const router = useRouter()
const { show } = useToast()

const trips = ref<TripResponse[]>([])
const loading = ref(false)
const currentPage = ref(0)
const hasMore = ref(false)

const isFilterModalOpen = ref(false)
const originFilter = ref('')
const destinationFilter = ref('')
const startTimeFrom = ref('')
const startTimeTo = ref('')
const hasActiveFilters = ref(false)

async function loadTrips() {
  loading.value = true
  try {
    const response = await getUserTrips(currentPage.value, 6, 'startTime', 'DESC')
    trips.value = response.content
    hasMore.value = response.hasNext
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to load trips'
    show(message, 'error')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loading.value = true
  currentPage.value++

  try {
    const response = hasActiveFilters.value
        ? await searchTrips(
            {
              origin: originFilter.value || undefined,
              destination: destinationFilter.value || undefined,
              startTimeFrom: startTimeFrom.value ? new Date(startTimeFrom.value).toISOString() : undefined,
              startTimeTo: startTimeTo.value ? new Date(startTimeTo.value).toISOString() : undefined
            },
            currentPage.value,
            6,
            'startTime',
            'DESC'
        )
        : await getUserTrips(currentPage.value, 6, 'startTime', 'DESC')

    trips.value.push(...response.content)
    hasMore.value = response.hasNext
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to load more'
    show(message, 'error')
  } finally {
    loading.value = false
  }
}

function openFilterModal() {
  isFilterModalOpen.value = true
}

function closeFilterModal() {
  isFilterModalOpen.value = false
}

function clearFilters() {
  originFilter.value = ''
  destinationFilter.value = ''
  startTimeFrom.value = ''
  startTimeTo.value = ''
}

async function applyFilters() {
  loading.value = true
  currentPage.value = 0
  hasActiveFilters.value = !!(originFilter.value || destinationFilter.value || startTimeFrom.value || startTimeTo.value)

  try {
    const response = await searchTrips(
        {
          origin: originFilter.value || undefined,
          destination: destinationFilter.value || undefined,
          startTimeFrom: startTimeFrom.value ? new Date(startTimeFrom.value).toISOString() : undefined,
          startTimeTo: startTimeTo.value ? new Date(startTimeTo.value).toISOString() : undefined
        },
        0,
        6,
        'startTime',
        'DESC'
    )

    trips.value = response.content
    hasMore.value = response.hasNext
    show(`Found ${response.totalElements} trips`, 'success')
  } catch (error: any) {
    const message = error.response?.data?.message || 'Search failed'
    show(message, 'error')
    trips.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }

  closeFilterModal()
}

function goToCreateTrip() {
  router.push('/trips/create/manual')
}

function viewTripDetail(id: number) {
  const selectedTrip = trips.value.find(t => t.id === id)

  router.push({
    name: 'TripDetail',
    params: { id },
    state: {
      trip: selectedTrip ? toRaw(selectedTrip) : undefined,
      from: 'Trips'
    }
  })
}

onMounted(() => {
  loadTrips()
})
</script>

<template>
  <div class="p-6 overflow-x-hidden">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-3xl font-bold">Trips</h1>
      <div class="flex gap-2">
        <button @click="openFilterModal" class="btn btn-neutral">
          <Filter :size="16" />
          Filters
        </button>
        <button @click="goToCreateTrip" class="btn btn-neutral">
          <Plus :size="16" />
          Record Trip
        </button>
      </div>
    </div>

    <dialog :class="['modal', isFilterModalOpen && 'modal-open']">
      <div class="modal-box">
        <div class="flex justify-between items-center mb-4">
          <h3 class="font-bold text-lg">Filter Trips</h3>
          <button @click="closeFilterModal" class="btn btn-sm btn-circle btn-ghost">
            <X :size="16" />
          </button>
        </div>

        <form @submit.prevent="applyFilters" class="space-y-4">
          <div>
            <label class="label">
              <span class="label-text">Origin</span>
            </label>
            <input
                type="text"
                v-model="originFilter"
                placeholder="Search by origin location"
                class="input input-bordered w-full"
                maxlength="256"
            />
          </div>

          <div>
            <label class="label">
              <span class="label-text">Destination</span>
            </label>
            <input
                type="text"
                v-model="destinationFilter"
                placeholder="Search by destination location"
                class="input input-bordered w-full"
                maxlength="256"
            />
          </div>

          <div>
            <label class="label">
              <span class="label-text">Start Time From</span>
            </label>
            <input
                type="datetime-local"
                v-model="startTimeFrom"
                class="input input-bordered w-full"
            />
          </div>

          <div>
            <label class="label">
              <span class="label-text">Start Time To</span>
            </label>
            <input
                type="datetime-local"
                v-model="startTimeTo"
                class="input input-bordered w-full"
            />
          </div>

          <div class="flex gap-2 mt-6">
            <button type="button" @click="clearFilters" class="btn btn-ghost flex-1">
              <Eraser :size="16" />
              Clear
            </button>
            <button type="submit" class="btn btn-neutral flex-1">
              <Search :size="16" />
              Apply Filters
            </button>
          </div>
        </form>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="closeFilterModal">close</button>
      </form>
    </dialog>

    <div v-if="loading && trips.length === 0" class="flex justify-center items-center py-12">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <div v-else-if="trips.length === 0" class="text-center py-12">
      <p class="text-gray-500">No trips found. Record your first one!</p>
    </div>

    <div v-else>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
            v-for="trip in trips"
            :key="trip.id"
            @click="viewTripDetail(trip.id)"
            class="card bg-base-100 shadow-xl cursor-pointer hover:shadow-2xl transition-shadow"
        >
          <figure class="h-48 bg-gray-200">
            <img
                :src="generateStaticMapUrl(trip.tripPoints, {
                accessToken: getMapboxApiKey(),
                width: 400,
                height: 300,
                addMarkers: true
              })"
                :alt="`Route from ${trip.origin} to ${trip.destination}`"
                class="w-full h-full object-cover"
            />
          </figure>

          <div class="card-body p-4">
            <div class="space-y-1 mb-2">
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500">From:</span>
                <p class="truncate flex-1 font-medium text-sm">{{ trip.origin }}</p>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500">To:</span>
                <p class="truncate flex-1 font-medium text-sm">{{ trip.destination }}</p>
              </div>
            </div>

            <div class="flex items-center gap-3 text-sm mb-2">
              <div class="flex items-center gap-1">
                <MapPin :size="14" />
                <span>{{ formatDistance(trip.totalDistance) }}</span>
              </div>
              <div class="flex items-center gap-1">
                <Clock :size="14" />
                <span>{{ formatDuration(trip.totalDuration) }}</span>
              </div>
            </div>

            <div class="text-xs text-gray-500 mt-2 pt-2 border-t">
              {{ formatDateRange(trip.startTime, trip.endTime) }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="flex justify-center mt-8">
        <button
            @click="loadMore"
            class="btn btn-neutral"
            :disabled="loading"
        >
          {{ loading ? 'Loading...' : 'Load More' }}
        </button>
      </div>
    </div>
  </div>
</template>