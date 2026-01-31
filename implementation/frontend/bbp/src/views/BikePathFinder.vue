<script setup lang="ts">
import { ref, onMounted, watch, toRaw } from 'vue'
import { useRouter } from 'vue-router'
import { getMapboxApiKey } from '@/config/mapbox'
import { Search, X, Eraser, Star, Bike, ArrowRight, ChevronDown } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'
import { useFieldError } from '@/composables/useFieldError'
import { useNoScroll } from '@/composables/useNoScroll.ts'
import { useMap } from '@/composables/useMap'
import { useMapRoute } from '@/composables/useMapRoute'
import { useMapObstacles } from '@/composables/useMapObstacles'
import { useMapboxAutocomplete } from '@/composables/useMapboxAutocomplete'
import { usePagination } from '@/composables/usePagination'
import { useBikePathFinderStore } from '@/stores/bikePathFinder'
import { findBikePaths } from '@/services/bikePathFinder'
import { validateRequired, validateAndShow } from '@/utils/validation'
import { formatDistance, formatScore } from '@/utils/format'
import { RADIUS_OPTIONS, DEFAULT_RADIUS_KM } from '@/constants/bikePath'
import { BIKE_PATH_FINDER_PAGE_SIZE } from '@/constants/pagination'
import { ADDRESS_MAX_LENGTH } from '@/constants/validation'
import type { BikePathResponse } from '@/types/bikePath'

const router = useRouter()
const { show } = useToast()
const { hasError, setError } = useFieldError()
const store = useBikePathFinderStore()
useNoScroll()

const {
  items: searchResults,
  hasMore,
  isLoading: showSpinner,
  isLoadingMore: loadingMore,
  loadInitial,
  loadMore: loadMorePagination,
  reset
} = usePagination<BikePathResponse>(BIKE_PATH_FINDER_PAGE_SIZE)

const mapContainer = ref<HTMLDivElement | null>(null)

const { map, isReady, initMap } = useMap({
  container: mapContainer,
  accessToken: getMapboxApiKey(),
  interactive: true,
  enableGeolocation: true
})
const { drawRoute, addMarkers, clearRoute } = useMapRoute(map)
const { addObstacles, clearObstacles } = useMapObstacles(map)

const { suggestions, showSuggestions, onInput: onAutocompleteInput, onBlur: onAutocompleteBlur } =
    useMapboxAutocomplete()
const activeField = ref<'origin' | 'destination' | null>(null)

const isSidebarOpen = ref(false)
const originAddress = ref('')
const originRadius = ref(DEFAULT_RADIUS_KM)
const destinationAddress = ref('')
const destinationRadius = ref(DEFAULT_RADIUS_KM)
const selectedBikePathId = ref<number | null>(null)

function selectOriginRadius(value: number) {
  originRadius.value = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function selectDestinationRadius(value: number) {
  destinationRadius.value = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function setActiveField(field: 'origin' | 'destination') {
  activeField.value = field
}

function selectSuggestion(suggestion: any, field: 'origin' | 'destination') {
  const address = suggestion.full_address || ''

  if (field === 'origin') {
    originAddress.value = address
  } else {
    destinationAddress.value = address
  }

  showSuggestions.value = false
  activeField.value = null
}

async function handleSearch() {
  // Frontend validation
  if (!validateAndShow(validateRequired(originAddress.value, 'Origin'), 'originAddress', setError, show)) return
  if (!validateAndShow(validateRequired(destinationAddress.value, 'Destination'), 'destinationAddress', setError, show)) return

  selectedBikePathId.value = null

  await loadInitial((page, size) =>
      findBikePaths(
          {
            originAddress: originAddress.value,
            destinationAddress: destinationAddress.value,
            originRadiusKm: originRadius.value,
            destinationRadiusKm: destinationRadius.value
          },
          page,
          size
      )
  )

  if (searchResults.value.length > 0) {
    show(`Found ${searchResults.value.length} bike paths`, 'success')
  }
}

async function handleLoadMore() {
  await loadMorePagination((page, size) =>
      findBikePaths(
          {
            originAddress: originAddress.value,
            destinationAddress: destinationAddress.value,
            originRadiusKm: originRadius.value,
            destinationRadiusKm: destinationRadius.value
          },
          page,
          size
      )
  )
}

function clearSearch() {
  originAddress.value = ''
  originRadius.value = DEFAULT_RADIUS_KM
  destinationAddress.value = ''
  destinationRadius.value = DEFAULT_RADIUS_KM
  selectedBikePathId.value = null

  reset()
  clearRoute()
  clearObstacles()
  store.clearSearchState()
}

function selectBikePath(bikePathId: number) {
  selectedBikePathId.value = bikePathId

  const selectedPath = searchResults.value.find(bp => bp.id === bikePathId)
  if (!selectedPath || !isReady.value) return

  clearRoute()
  clearObstacles()

  drawRoute(selectedPath.bikePathPoints)

  addMarkers(
      {
        address: selectedPath.origin,
        latitude: selectedPath.originLatitude,
        longitude: selectedPath.originLongitude
      },
      {
        address: selectedPath.destination,
        latitude: selectedPath.destinationLatitude,
        longitude: selectedPath.destinationLongitude
      }
  )

  if (selectedPath.obstacles && selectedPath.obstacles.length > 0) {
    addObstacles(selectedPath.obstacles)
  }
}

function viewDetails(bikePathId: number) {
  const selectedBikePath = searchResults.value.find(bp => bp.id === bikePathId)

  store.saveSearchState({
    originAddress: originAddress.value,
    destinationAddress: destinationAddress.value,
    originRadius: originRadius.value,
    destinationRadius: destinationRadius.value,
    searchResults: toRaw(searchResults.value),
    currentPage: 0, // currentPage is now internal to composable
    hasMore: hasMore.value,
    selectedBikePathId: selectedBikePathId.value,
    isSidebarOpen: isSidebarOpen.value
  })

  router.push({
    name: 'BikePathDetail',
    params: { id: bikePathId },
    state: {
      bikePath: selectedBikePath ? toRaw(selectedBikePath) : undefined,
      from: 'BikePathFinder'
    } as Record<string, any>
  })
}

function restoreSearchState() {
  if (!store.hasSearchState) {
    console.log('⊘ No saved search state to restore')
    return
  }

  console.log('↻ Restoring search state from store...')

  originAddress.value = store.originAddress
  destinationAddress.value = store.destinationAddress
  originRadius.value = store.originRadius
  destinationRadius.value = store.destinationRadius

  searchResults.value = store.searchResults
  hasMore.value = store.hasMore

  selectedBikePathId.value = store.selectedBikePathId
  isSidebarOpen.value = store.isSidebarOpen

  console.log(`✓ Restored ${searchResults.value.length} search results`)
}

function restoreMapState() {
  if (!isReady.value || !store.selectedBikePathId) return

  const selectedPath = searchResults.value.find(bp => bp.id === store.selectedBikePathId)
  if (!selectedPath) return

  console.log('↻ Restoring map state...')

  drawRoute(selectedPath.bikePathPoints)

  addMarkers(
      {
        address: selectedPath.origin,
        latitude: selectedPath.originLatitude,
        longitude: selectedPath.originLongitude
      },
      {
        address: selectedPath.destination,
        latitude: selectedPath.destinationLatitude,
        longitude: selectedPath.destinationLongitude
      }
  )

  if (selectedPath.obstacles && selectedPath.obstacles.length > 0) {
    addObstacles(selectedPath.obstacles)
  }

  console.log('✓ Map state restored')
}

watch(isReady, (ready) => {
  if (ready && store.hasSearchState) {
    restoreMapState()
  }
})

onMounted(() => {
  document.body.style.overflow = 'hidden'

  restoreSearchState()
  initMap()

  if (map.value) {
    (map.value as any).on('click', () => {
      if (isSidebarOpen.value) {
        isSidebarOpen.value = false
      }
    })
  }
})
</script>

<template>
  <div class="h-full w-full overflow-hidden relative">
    <div ref="mapContainer" class="h-full w-full"></div>

    <button
        v-if="!isSidebarOpen"
        @click="isSidebarOpen = true"
        class="btn btn-circle btn-neutral shadow-xl absolute top-4 left-4 z-30"
    >
      <Search :size="20" />
    </button>

    <div
        :class="[
        'absolute top-0 left-0 h-full bg-base-100 shadow-2xl z-20 transition-transform duration-300',
        'w-96 overflow-y-auto',
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
      ]"
    >
      <div class="p-6">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-2xl font-bold">Search Bike Paths</h2>
          <button @click="isSidebarOpen = false" class="btn btn-circle btn-ghost">
            <X :size="20" />
          </button>
        </div>

        <form @submit.prevent="handleSearch" class="space-y-4">
          <div>
            <label class="label">
              <span class="label-text">Origin</span>
            </label>
            <div class="relative">
              <input
                  type="text"
                  v-model.trim="originAddress"
                  placeholder="Enter origin address"
                  class="input input-bordered w-full"
                  :class="{'input-error': hasError('originAddress')}"
                  :maxlength="ADDRESS_MAX_LENGTH"
                  @focus="setActiveField('origin')"
                  @input="onAutocompleteInput(($event.target as HTMLInputElement).value)"
                  @blur="onAutocompleteBlur"
                  required
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
            <label class="label">
              <span class="label-text">Radius</span>
            </label>
            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
                  :class="{'input-error': hasError('originRadiusKm')}"
              >
                {{ RADIUS_OPTIONS.find(o => o.value === originRadius)?.label }}
                <ChevronDown :size="16" />
              </div>
              <ul
                  tabindex="0"
                  class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow"
              >
                <li v-for="option in RADIUS_OPTIONS" :key="option.value">
                  <a @click="selectOriginRadius(option.value)">{{ option.label }}</a>
                </li>
              </ul>
            </div>
          </div>

          <div>
            <label class="label">
              <span class="label-text">Destination</span>
            </label>
            <div class="relative">
              <input
                  type="text"
                  v-model.trim="destinationAddress"
                  placeholder="Enter destination address"
                  class="input input-bordered w-full"
                  :class="{'input-error': hasError('destinationAddress')}"
                  :maxlength="ADDRESS_MAX_LENGTH"
                  @focus="setActiveField('destination')"
                  @input="onAutocompleteInput(($event.target as HTMLInputElement).value)"
                  @blur="onAutocompleteBlur"
                  required
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
            <label class="label">
              <span class="label-text">Radius</span>
            </label>
            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
                  :class="{'input-error': hasError('destinationRadiusKm')}"
              >
                {{ RADIUS_OPTIONS.find(o => o.value === destinationRadius)?.label }}
                <ChevronDown :size="16" />
              </div>
              <ul
                  tabindex="0"
                  class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow"
              >
                <li v-for="option in RADIUS_OPTIONS" :key="option.value">
                  <a @click="selectDestinationRadius(option.value)">{{ option.label }}</a>
                </li>
              </ul>
            </div>
          </div>

          <div class="flex gap-2">
            <button type="button" @click="clearSearch" class="btn btn-ghost flex-1">
              <Eraser :size="16" />
              Clear
            </button>
            <button type="submit" class="btn btn-neutral flex-1" :disabled="showSpinner">
              <Search :size="16" />
              {{ showSpinner ? 'Searching...' : 'Search' }}
            </button>
          </div>
        </form>

        <div class="divider"></div>

        <div v-if="showSpinner && searchResults.length === 0" class="text-center text-gray-500">
          <span class="loading loading-spinner loading-md"></span>
          <p class="mt-2">Searching...</p>
        </div>

        <div v-else-if="searchResults.length === 0" class="text-center text-gray-500">
          <p>No results found. Try searching!</p>
        </div>

        <div v-else class="space-y-3">
          <div
              v-for="bikePath in searchResults"
              :key="bikePath.id"
              @click="selectBikePath(bikePath.id)"
              :class="[
              'p-4 border rounded-lg cursor-pointer transition-colors',
              selectedBikePathId === bikePath.id
                ? 'border-primary bg-primary/10'
                : 'border-base-300 hover:bg-base-200'
            ]"
          >
            <div class="flex items-center gap-2 mb-1">
              <span class="text-xs text-gray-500">From:</span>
              <p class="truncate flex-1 font-medium">{{ bikePath.origin }}</p>
            </div>

            <div class="flex items-center gap-2 mb-3">
              <span class="text-xs text-gray-500">To:</span>
              <p class="truncate flex-1 font-medium">{{ bikePath.destination }}</p>
            </div>

            <div class="flex items-center gap-4 text-sm mb-3">
              <div class="flex items-center gap-1">
                <Star :size="16" class="text-warning fill-warning" />
                <span>{{ formatScore(bikePath.score) }}</span>
              </div>
              <div class="flex items-center gap-1">
                <Bike :size="16" />
                <span>{{ formatDistance(bikePath.totalDistance) }}</span>
              </div>
              <span class="text-gray-600">{{ bikePath.statusDescription }}</span>
            </div>

            <p v-if="bikePath.description" class="text-sm text-gray-600 line-clamp-2 mb-3">
              {{ bikePath.description }}
            </p>

            <button @click.stop="viewDetails(bikePath.id)" class="btn btn-sm btn-neutral w-full">
              View Details
              <ArrowRight :size="16" />
            </button>
          </div>

          <button
              v-if="hasMore"
              @click="handleLoadMore"
              class="btn btn-neutral w-full"
              :disabled="loadingMore"
          >
            {{ loadingMore ? 'Loading...' : 'Load More' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>