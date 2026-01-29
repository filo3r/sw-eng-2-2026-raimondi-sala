<script setup lang="ts">
import { ref, onMounted, toRaw } from 'vue'
import { useRouter } from 'vue-router'
import { Filter, Plus, X, Search, Eraser, MapPin, Clock } from 'lucide-vue-next'
import { getUserTrips, searchTrips } from '@/services/trip'
import { getMapboxApiKey } from '@/config/mapbox'
import { generateStaticMapUrl } from '@/utils/mapStatic'
import { useToast } from '@/composables/useToast'
import { useAsyncState } from '@/composables/useAsyncState'
import { useMapboxAutocomplete } from '@/composables/useMapboxAutocomplete'
import { formatDistance, formatDuration } from '@/utils/format'
import { formatDateRange } from '@/utils/date'
import { catchApiError } from '@/utils/error'
import { ADDRESS_MAX_LENGTH } from '@/constants/validation'
import { TRIP_PAGE_SIZE, SORT_DESC } from '@/constants/pagination'
import { SPINNER_DELAY_MS } from '@/constants/ui'
import type { TripResponse, PagedTripResponse } from '@/types/trip'

const router = useRouter()
const { show } = useToast()

const { isLoading, execute } = useAsyncState<PagedTripResponse>()
const trips = ref<TripResponse[]>([])
const loadingMore = ref(false)
const currentPage = ref(0)
const hasMore = ref(false)

const { suggestions, showSuggestions, onInput: onAutocompleteInput, onBlur: onAutocompleteBlur } =
    useMapboxAutocomplete()
const activeField = ref<'origin' | 'destination' | null>(null)

const isFilterModalOpen = ref(false)
const originFilter = ref('')
const destinationFilter = ref('')
const startDateFromStr = ref('')
const startTimeFromStr = ref('')
const startDateToStr = ref('')
const startTimeToStr = ref('')
const hasActiveFilters = ref(false)

const showSpinner = ref(false)
const initialLoadComplete = ref(false)
let spinnerTimeout: number | null = null

function normalizeTime(t: string): string {
  if (!t) return ''
  return t.length === 5 ? `${t}:00` : t
}

function toDate(dateStr: string, timeStr: string): Date | null {
  if (!dateStr || !timeStr) return null
  const d = new Date(`${dateStr}T${normalizeTime(timeStr)}`)
  return Number.isNaN(d.getTime()) ? null : d
}

function setActiveField(field: 'origin' | 'destination') {
  activeField.value = field
}

function selectSuggestion(suggestion: any, field: 'origin' | 'destination') {
  const address = suggestion.full_address || ''
  if (field === 'origin') {
    originFilter.value = address
  } else {
    destinationFilter.value = address
  }
  showSuggestions.value = false
  activeField.value = null
}

async function loadTrips() {
  spinnerTimeout = window.setTimeout(() => {
    showSpinner.value = true
  }, SPINNER_DELAY_MS)

  await execute(
      () => getUserTrips(currentPage.value, TRIP_PAGE_SIZE, 'startTime', SORT_DESC),
      'Trips.loadTrips',
      (response) => {
        trips.value = response.content
        hasMore.value = response.hasNext
        initialLoadComplete.value = true
      }
  )

  if (spinnerTimeout) clearTimeout(spinnerTimeout)
  showSpinner.value = false
}

async function loadMore() {
  currentPage.value++

  let loadMoreSpinnerTimeout = window.setTimeout(() => {
    loadingMore.value = true
  }, SPINNER_DELAY_MS)

  try {
    const response = hasActiveFilters.value
        ? await searchTrips(
            {
              origin: originFilter.value || undefined,
              destination: destinationFilter.value || undefined,
              startTimeFrom: toDate(startDateFromStr.value, startTimeFromStr.value)?.toISOString(),
              startTimeTo: toDate(startDateToStr.value, startTimeToStr.value)?.toISOString()
            },
            currentPage.value,
            TRIP_PAGE_SIZE,
            'startTime',
            SORT_DESC
        )
        : await getUserTrips(currentPage.value, TRIP_PAGE_SIZE, 'startTime', SORT_DESC)

    trips.value.push(...response.content)
    hasMore.value = response.hasNext
  } catch (error) {
    catchApiError(error, 'Trips.loadMore')
  } finally {
    clearTimeout(loadMoreSpinnerTimeout)
    loadingMore.value = false
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
  startDateFromStr.value = ''
  startTimeFromStr.value = ''
  startDateToStr.value = ''
  startTimeToStr.value = ''
}

async function applyFilters() {
  currentPage.value = 0

  const startTimeFrom = toDate(startDateFromStr.value, startTimeFromStr.value)
  const startTimeTo = toDate(startDateToStr.value, startTimeToStr.value)

  hasActiveFilters.value = !!(originFilter.value || destinationFilter.value || startTimeFrom || startTimeTo)

  await execute(
      () => searchTrips(
          {
            origin: originFilter.value || undefined,
            destination: destinationFilter.value || undefined,
            startTimeFrom: startTimeFrom?.toISOString(),
            startTimeTo: startTimeTo?.toISOString()
          },
          0,
          TRIP_PAGE_SIZE,
          'startTime',
          SORT_DESC
      ),
      'Trips.applyFilters',
      (response) => {
        trips.value = response.content
        hasMore.value = response.hasNext
        show(`Found ${response.totalElements} trips`, 'success')
      },
      () => {
        trips.value = []
        hasMore.value = false
      }
  )

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
  <div v-if="showSpinner" class="flex h-screen items-center justify-center">
    <span class="loading loading-spinner loading-lg"></span>
  </div>

  <div v-else-if="initialLoadComplete" class="p-6 overflow-x-hidden">
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
            <div class="relative">
              <input
                  type="text"
                  v-model="originFilter"
                  placeholder="Search by origin location"
                  class="input input-bordered w-full"
                  :maxlength="ADDRESS_MAX_LENGTH"
                  @focus="setActiveField('origin')"
                  @input="onAutocompleteInput(($event.target as HTMLInputElement).value)"
                  @blur="onAutocompleteBlur"
              />

              <div
                  v-if="showSuggestions && activeField === 'origin' && suggestions.length > 0"
                  class="absolute z-50 w-full mt-1 bg-base-100 border border-base-300 rounded-lg shadow-lg max-h-60 overflow-y-auto"
              >
                <div
                    v-for="(suggestion, i) in suggestions"
                    :key="i"
                    @click="selectSuggestion(suggestion, 'origin')"
                    class="px-4 py-2 hover:bg-base-200 cursor-pointer border-b border-base-200 last:border-0"
                >
                  <div class="font-medium">{{ suggestion.name }}</div>
                  <div class="text-sm text-gray-500">{{ suggestion.full_address }}</div>
                </div>
              </div>
            </div>
          </div>

          <div>
            <label class="label">
              <span class="label-text">Destination</span>
            </label>
            <div class="relative">
              <input
                  type="text"
                  v-model="destinationFilter"
                  placeholder="Search by destination location"
                  class="input input-bordered w-full"
                  :maxlength="ADDRESS_MAX_LENGTH"
                  @focus="setActiveField('destination')"
                  @input="onAutocompleteInput(($event.target as HTMLInputElement).value)"
                  @blur="onAutocompleteBlur"
              />

              <div
                  v-if="showSuggestions && activeField === 'destination' && suggestions.length > 0"
                  class="absolute z-50 w-full mt-1 bg-base-100 border border-base-300 rounded-lg shadow-lg max-h-60 overflow-y-auto"
              >
                <div
                    v-for="(suggestion, i) in suggestions"
                    :key="i"
                    @click="selectSuggestion(suggestion, 'destination')"
                    class="px-4 py-2 hover:bg-base-200 cursor-pointer border-b border-base-200 last:border-0"
                >
                  <div class="font-medium">{{ suggestion.name }}</div>
                  <div class="text-sm text-gray-500">{{ suggestion.full_address }}</div>
                </div>
              </div>
            </div>
          </div>

          <div>
            <label class="label">
              <span class="label-text">Start Time From</span>
            </label>
            <div class="grid grid-cols-2 gap-2">
              <input v-model="startDateFromStr" type="date" class="input input-bordered w-full" />
              <input v-model="startTimeFromStr" type="time" step="1" class="input input-bordered w-full" />
            </div>
          </div>

          <div>
            <label class="label">
              <span class="label-text">Start Time To</span>
            </label>
            <div class="grid grid-cols-2 gap-2">
              <input v-model="startDateToStr" type="date" class="input input-bordered w-full" />
              <input v-model="startTimeToStr" type="time" step="1" class="input input-bordered w-full" />
            </div>
          </div>

          <div class="flex gap-2 mt-6">
            <button type="button" @click="clearFilters" class="btn btn-ghost flex-1">
              <Eraser :size="16" />
              Clear
            </button>
            <button type="submit" class="btn btn-neutral flex-1" :disabled="isLoading">
              <Search :size="16" />
              {{ isLoading ? 'Searching...' : 'Apply Filters' }}
            </button>
          </div>
        </form>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="closeFilterModal">close</button>
      </form>
    </dialog>

    <div v-if="trips.length === 0" class="text-center py-12">
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
        <button @click="loadMore" class="btn btn-neutral" :disabled="loadingMore">
          {{ loadingMore ? 'Loading...' : 'Load More' }}
        </button>
      </div>
    </div>
  </div>
</template>