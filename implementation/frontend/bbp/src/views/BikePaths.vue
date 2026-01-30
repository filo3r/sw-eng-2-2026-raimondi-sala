<script setup lang="ts">
import { ref, onMounted, toRaw} from 'vue'
import { useRouter } from 'vue-router'
import { Filter, Plus, X, Search, Eraser, Star, Bike, UsersRound, User } from 'lucide-vue-next'
import { getUserBikePaths, searchBikePaths } from '@/services/bikePath'
import { getMapboxApiKey } from '@/config/mapbox'
import { generateStaticMapUrl } from '@/utils/mapStatic'
import { useToast } from '@/composables/useToast'
import { useAsyncState } from '@/composables/useAsyncState'
import { useFieldError } from '@/composables/useFieldError'
import { useMapboxAutocomplete } from '@/composables/useMapboxAutocomplete'
import { formatDistance, formatScore } from '@/utils/format'
import { formatDate } from '@/utils/date'
import { catchApiError } from '@/utils/error'
import { ADDRESS_MAX_LENGTH } from '@/constants/validation'
import { BIKE_PATH_PAGE_SIZE, SORT_DESC } from '@/constants/pagination'
import { SPINNER_DELAY_MS } from '@/constants/ui'
import type { BikePathResponse, PagedBikePathResponse } from '@/types/bikePath'

const router = useRouter()
const { show } = useToast()
const { hasError, setError } = useFieldError()

const { isLoading, execute } = useAsyncState<PagedBikePathResponse>()
const bikePaths = ref<BikePathResponse[]>([])
const loadingMore = ref(false)
const currentPage = ref(0)
const hasMore = ref(false)

const { suggestions, showSuggestions, onInput: onAutocompleteInput, onBlur: onAutocompleteBlur } =
    useMapboxAutocomplete()
const activeField = ref<'origin' | 'destination' | null>(null)

const isFilterModalOpen = ref(false)
const originFilter = ref('')
const destinationFilter = ref('')
const createdDateFromStr = ref('')
const createdTimeFromStr = ref('')
const createdDateToStr = ref('')
const createdTimeToStr = ref('')
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

async function loadBikePaths() {
  spinnerTimeout = window.setTimeout(() => {
    showSpinner.value = true
  }, SPINNER_DELAY_MS)

  await execute(
      () => getUserBikePaths(currentPage.value, BIKE_PATH_PAGE_SIZE, 'createdAt', SORT_DESC),
      'BikePaths.loadBikePaths',
      (response) => {
        bikePaths.value = response.content
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
        ? await searchBikePaths(
            {
              origin: originFilter.value || undefined,
              destination: destinationFilter.value || undefined,
              createdAtFrom: toDate(createdDateFromStr.value, createdTimeFromStr.value)?.toISOString(),
              createdAtTo: toDate(createdDateToStr.value, createdTimeToStr.value)?.toISOString()
            },
            currentPage.value,
            BIKE_PATH_PAGE_SIZE,
            'createdAt',
            SORT_DESC
        )
        : await getUserBikePaths(currentPage.value, BIKE_PATH_PAGE_SIZE, 'createdAt', SORT_DESC)

    bikePaths.value.push(...response.content)
    hasMore.value = response.hasNext
  } catch (error) {
    catchApiError(error, 'BikePaths.loadMore')
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
  createdDateFromStr.value = ''
  createdTimeFromStr.value = ''
  createdDateToStr.value = ''
  createdTimeToStr.value = ''
}

async function applyFilters() {
  currentPage.value = 0

  const createdAtFrom = toDate(createdDateFromStr.value, createdTimeFromStr.value)
  const createdAtTo = toDate(createdDateToStr.value, createdTimeToStr.value)

  hasActiveFilters.value = !!(originFilter.value || destinationFilter.value || createdAtFrom || createdAtTo)

  await execute(
      () => searchBikePaths(
          {
            origin: originFilter.value || undefined,
            destination: destinationFilter.value || undefined,
            createdAtFrom: createdAtFrom?.toISOString(),
            createdAtTo: createdAtTo?.toISOString()
          },
          0,
          BIKE_PATH_PAGE_SIZE,
          'createdAt',
          SORT_DESC
      ),
      'BikePaths.applyFilters',
      (response) => {
        bikePaths.value = response.content
        hasMore.value = response.hasNext
        show(`Found ${response.totalElements} bike paths`, 'success')
      },
      () => {
        bikePaths.value = []
        hasMore.value = false
      },
      setError
  )

  closeFilterModal()
}

function goToCreateBikePath() {
  router.push('/bike-paths/create/manual')
}

function viewBikePathDetail(id: number) {
  const selectedBikePath = bikePaths.value.find(bp => bp.id === id)
  router.push({
    name: 'BikePathDetail',
    params: { id },
    state: {
      bikePath: selectedBikePath ? toRaw(selectedBikePath) : undefined,
      from: 'BikePaths'
    }
  })
}

onMounted(() => {
  loadBikePaths()
})
</script>

<template>
  <div v-if="showSpinner" class="flex h-screen items-center justify-center">
    <span class="loading loading-spinner loading-lg"></span>
  </div>

  <div v-else-if="initialLoadComplete" class="p-6 overflow-x-hidden">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-3xl font-bold">Bike Paths</h1>
      <div class="flex gap-2">
        <button @click="openFilterModal" class="btn btn-neutral">
          <Filter :size="16" />
          Filters
        </button>
        <button @click="goToCreateBikePath" class="btn btn-neutral">
          <Plus :size="16" />
          Create Bike Path
        </button>
      </div>
    </div>

    <dialog :class="['modal', isFilterModalOpen && 'modal-open']">
      <div class="modal-box">
        <div class="flex justify-between items-center mb-4">
          <h3 class="font-bold text-lg">Filter Bike Paths</h3>
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
                  :class="{'input-error': hasError('origin')}"
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
                  :class="{'input-error': hasError('destination')}"
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
              <span class="label-text">Created From</span>
            </label>
            <div class="grid grid-cols-2 gap-2">
              <input
                  v-model="createdDateFromStr"
                  type="date"
                  class="input input-bordered w-full"
                  :class="{'input-error': hasError('createdAtFrom')}"
              />
              <input
                  v-model="createdTimeFromStr"
                  type="time"
                  step="1"
                  class="input input-bordered w-full"
                  :class="{'input-error': hasError('createdAtFrom')}"
              />
            </div>
          </div>

          <div>
            <label class="label">
              <span class="label-text">Created To</span>
            </label>
            <div class="grid grid-cols-2 gap-2">
              <input
                  v-model="createdDateToStr"
                  type="date"
                  class="input input-bordered w-full"
                  :class="{'input-error': hasError('createdAtTo')}"
              />
              <input
                  v-model="createdTimeToStr"
                  type="time"
                  step="1"
                  class="input input-bordered w-full"
                  :class="{'input-error': hasError('createdAtTo')}"
              />
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

    <div v-if="bikePaths.length === 0" class="text-center py-12">
      <p class="text-gray-500">No bike paths found. Create your first one!</p>
    </div>

    <div v-else>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
            v-for="bikePath in bikePaths"
            :key="bikePath.id"
            @click="viewBikePathDetail(bikePath.id)"
            class="card bg-base-100 shadow-xl cursor-pointer hover:shadow-2xl transition-shadow"
        >
          <figure class="h-48 bg-gray-200">
            <img
                :src="generateStaticMapUrl(bikePath.bikePathPoints, {
                accessToken: getMapboxApiKey(),
                width: 400,
                height: 300,
                addMarkers: true
              })"
                :alt="`Route from ${bikePath.origin} to ${bikePath.destination}`"
                class="w-full h-full object-cover"
            />
          </figure>

          <div class="card-body p-4">
            <div class="space-y-1 mb-2">
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500">From:</span>
                <p class="truncate flex-1 font-medium text-sm">{{ bikePath.origin }}</p>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-500">To:</span>
                <p class="truncate flex-1 font-medium text-sm">{{ bikePath.destination }}</p>
              </div>
            </div>

            <div class="flex items-center gap-3 text-sm mb-2">
              <div class="flex items-center gap-1">
                <Star :size="14" class="text-warning fill-warning" />
                <span>{{ formatScore(bikePath.score) }}</span>
              </div>
              <div class="flex items-center gap-1">
                <Bike :size="14" />
                <span>{{ formatDistance(bikePath.totalDistance) }}</span>
              </div>
              <span class="text-xs text-gray-600">{{ bikePath.statusDescription }}</span>
            </div>

            <div class="flex items-center justify-between text-xs text-gray-500 mt-2 pt-2 border-t">
              <span>{{ formatDate(bikePath.createdAt) }}</span>
              <div class="flex items-center gap-1">
                <UsersRound v-if="bikePath.published" :size="14" class="text-success" />
                <User v-else :size="14" class="text-gray-400" />
                <span>{{ bikePath.published ? 'Public' : 'Private' }}</span>
              </div>
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