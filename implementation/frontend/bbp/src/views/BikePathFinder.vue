<script setup lang="ts">
/**
 * Bike Path Finder page (map + sidebar search).
 * - Shows a full-screen Mapbox map with a slide-in sidebar for search inputs.
 * - Uses Mapbox autocomplete for origin/destination addresses.
 * - Calls the bike path finder API with pagination and shows selectable results.
 * - When a result is selected, draws its route and overlays obstacles on the map.
 * - Persists/restores search state via Pinia store when navigating to details and back.
 *
 * Fragility notes:
 * - Map event listeners must be registered after the map instance exists and removed on unmount
 *   using the exact same handler reference. [web:185]
 * - Autocomplete suggestion clicks can be swallowed by input blur; use mousedown to select first.
 */
import { ref, onMounted, onUnmounted, watch, toRaw } from 'vue'
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

type ActiveField = 'origin' | 'destination' | null

const router = useRouter()
const { show } = useToast()
const { hasError, setError } = useFieldError()
const store = useBikePathFinderStore()

/**
 * Disables page scroll while this view is active (map full-screen UX).
 * Keep in mind: this composable may already manage body styles; we still restore on unmount below.
 */
useNoScroll()

/**
 * Pagination composable holds:
 * - searchResults: current result list
 * - hasMore: server says there are more pages
 * - showSpinner / loadingMore: loading states
 */
const {
  items: searchResults,
  hasMore,
  isLoading: showSpinner,
  isLoadingMore: loadingMore,
  loadInitial,
  loadMore: loadMorePagination,
  reset
} = usePagination<BikePathResponse>(BIKE_PATH_FINDER_PAGE_SIZE)

/** Map container reference used to mount Mapbox map. */
const mapContainer = ref<HTMLDivElement | null>(null)

/** Cache Mapbox token once (avoid repeated calls during render). */
const mapboxToken = getMapboxApiKey()

/**
 * Map initialization.
 * - interactive: true enables pan/zoom
 * - enableGeolocation: true enables the geolocation control/features (per your composable)
 */
const { map, isReady, initMap } = useMap({
  container: mapContainer,
  accessToken: mapboxToken,
  interactive: true,
  enableGeolocation: true
})

/** Route/marker helpers bound to the current map instance. */
const { drawRoute, addMarkers, clearRoute } = useMapRoute(map)

/** Obstacle helpers bound to the current map instance. */
const { addObstacles, clearObstacles } = useMapObstacles(map)

/** Autocomplete state shared by origin/destination inputs. */
const { suggestions, showSuggestions, onInput: onAutocompleteInput, onBlur: onAutocompleteBlur } =
    useMapboxAutocomplete()

/** Tracks which field currently owns the shared suggestions list. */
const activeField = ref<ActiveField>(null)

/** Sidebar UI + search params. */
const isSidebarOpen = ref(false)
const originAddress = ref('')
const originRadius = ref(DEFAULT_RADIUS_KM)
const destinationAddress = ref('')
const destinationRadius = ref(DEFAULT_RADIUS_KM)
const selectedBikePathId = ref<number | null>(null)

/**
 * Dropdown select helpers.
 * Blurring the focused dropdown trigger ensures it closes after selection (DaisyUI dropdown behavior). [web:190]
 */
function selectOriginRadius(value: number): void {
  originRadius.value = value
  ;(document.activeElement as HTMLElement | null)?.blur()
}

function selectDestinationRadius(value: number): void {
  destinationRadius.value = value
  ;(document.activeElement as HTMLElement | null)?.blur()
}

/**
 * Sets which input is active so suggestions render under the correct field.
 * @param field - 'origin' or 'destination'
 */
function setActiveField(field: Exclude<ActiveField, null>): void {
  activeField.value = field
}

/**
 * Applies an autocomplete suggestion to the selected field.
 * @param suggestion - Mapbox suggestion object (shape depends on your composable)
 * @param field - 'origin' or 'destination'
 */
function selectSuggestion(suggestion: any, field: Exclude<ActiveField, null>): void {
  const address = suggestion.full_address || ''
  if (field === 'origin') originAddress.value = address
  else destinationAddress.value = address
  showSuggestions.value = false
  activeField.value = null
}

/**
 * Executes the search and loads the first page of results.
 * Validates origin/destination presence before calling the API.
 */
async function handleSearch(): Promise<void> {
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

/**
 * Loads the next page of results with the same current search params.
 */
async function handleLoadMore(): Promise<void> {
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

/**
 * Clears search inputs, results, map overlays, and stored state.
 */
function clearSearch(): void {
  originAddress.value = ''
  originRadius.value = DEFAULT_RADIUS_KM
  destinationAddress.value = ''
  destinationRadius.value = DEFAULT_RADIUS_KM
  selectedBikePathId.value = null

  showSuggestions.value = false
  activeField.value = null

  reset()
  clearRoute()
  clearObstacles()
  store.clearSearchState()
}

/**
 * Selects a bike path result and draws it on the map.
 * @param bikePathId - Selected bike path id
 */
function selectBikePath(bikePathId: number): void {
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

  if (selectedPath.obstacles?.length) addObstacles(selectedPath.obstacles)
}

/**
 * Navigates to BikePathDetail, saving finder state so it can be restored when navigating back.
 * @param bikePathId - Selected bike path id
 */
function viewDetails(bikePathId: number): void {
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

/**
 * Restores sidebar/search state from the store (if present).
 */
function restoreSearchState(): void {
  if (!store.hasSearchState) return

  originAddress.value = store.originAddress
  destinationAddress.value = store.destinationAddress
  originRadius.value = store.originRadius
  destinationRadius.value = store.destinationRadius

  searchResults.value = store.searchResults
  hasMore.value = store.hasMore

  selectedBikePathId.value = store.selectedBikePathId
  isSidebarOpen.value = store.isSidebarOpen
}

/**
 * Restores map overlays (route/markers/obstacles) from the stored selected bike path.
 * Requires map readiness and a selected id.
 */
function restoreMapState(): void {
  if (!isReady.value || !store.selectedBikePathId) return

  const selectedPath = searchResults.value.find(bp => bp.id === store.selectedBikePathId)
  if (!selectedPath) return

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

  if (selectedPath.obstacles?.length) addObstacles(selectedPath.obstacles)
}

/**
 * When the map becomes ready, restore overlays if we have saved search state.
 * Watching a ref is the standard pattern for reacting to readiness flags. [web:91]
 */
watch(isReady, (ready) => {
  if (ready && store.hasSearchState) restoreMapState()
})

/**
 * Map click handler used to close the sidebar when user clicks on the map.
 * Important: keep this as a stable function reference so it can be removed with map.off(...). [web:185]
 */
function handleMapClick(): void {
  if (isSidebarOpen.value) isSidebarOpen.value = false
}

/** Store and restore body styles to avoid leaking "no scroll" styles to other routes. */
const originalBodyOverflow = document.body.style.overflow

onMounted(() => {
  // Keep the current behavior: enforce no scroll (map full-screen).
  document.body.style.overflow = 'hidden'

  restoreSearchState()
  initMap()
})

/**
 * Attach/detach map click listener safely:
 * - Attach only when map is ready and map instance exists
 * - Detach on unmount (prevents duplicated handlers if the component is re-mounted)
 */
watch(isReady, (ready) => {
  if (!ready || !map.value) return
  map.value.on('click', handleMapClick)
})

onUnmounted(() => {
  // Cleanup map listener (requires same handler reference). [web:185]
  if (map.value) {
    map.value.off('click', handleMapClick)
  }

  // Restore body style so other pages can scroll normally.
  document.body.style.overflow = originalBodyOverflow
})
</script>

<template>
  <div class="h-full w-full overflow-hidden relative">
    <div ref="mapContainer" class="h-full w-full"></div>

    <button
        v-if="!isSidebarOpen"
        @click="isSidebarOpen = true"
        class="btn btn-circle btn-neutral shadow-xl absolute top-4 left-4 z-30"
        aria-label="Open search sidebar"
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
          <button @click="isSidebarOpen = false" class="btn btn-circle btn-ghost" aria-label="Close sidebar">
            <X :size="20" />
          </button>
        </div>

        <form @submit.prevent="handleSearch" class="space-y-4">
          <div>
            <label class="label"><span class="label-text">Origin</span></label>

            <div class="relative">
              <input
                  v-model.trim="originAddress"
                  type="text"
                  placeholder="Enter origin address"
                  class="input input-bordered w-full"
                  :class="{ 'input-error': hasError('originAddress') }"
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
                    @mousedown.prevent="selectSuggestion(suggestion, 'origin')"
                    class="px-4 py-2 hover:bg-base-200 cursor-pointer border-b border-base-200 last:border-0"
                >
                  <div class="font-medium">{{ suggestion.name }}</div>
                  <div class="text-sm text-gray-500">{{ suggestion.full_address }}</div>
                </div>
              </div>
            </div>

            <label class="label"><span class="label-text">Radius</span></label>

            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
                  :class="{ 'input-error': hasError('originRadiusKm') }"
              >
                {{ RADIUS_OPTIONS.find(o => o.value === originRadius)?.label }}
                <ChevronDown :size="16" />
              </div>

              <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow">
                <li v-for="option in RADIUS_OPTIONS" :key="option.value">
                  <a @click="selectOriginRadius(option.value)">{{ option.label }}</a>
                </li>
              </ul>
            </div>
          </div>

          <div>
            <label class="label"><span class="label-text">Destination</span></label>

            <div class="relative">
              <input
                  v-model.trim="destinationAddress"
                  type="text"
                  placeholder="Enter destination address"
                  class="input input-bordered w-full"
                  :class="{ 'input-error': hasError('destinationAddress') }"
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
                    @mousedown.prevent="selectSuggestion(suggestion, 'destination')"
                    class="px-4 py-2 hover:bg-base-200 cursor-pointer border-b border-base-200 last:border-0"
                >
                  <div class="font-medium">{{ suggestion.name }}</div>
                  <div class="text-sm text-gray-500">{{ suggestion.full_address }}</div>
                </div>
              </div>
            </div>

            <label class="label"><span class="label-text">Radius</span></label>

            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
                  :class="{ 'input-error': hasError('destinationRadiusKm') }"
              >
                {{ RADIUS_OPTIONS.find(o => o.value === destinationRadius)?.label }}
                <ChevronDown :size="16" />
              </div>

              <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow">
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
            <button type="submit" class="btn btn-neutral flex-1" :disabled="showSpinner" :aria-busy="showSpinner">
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

          <button v-if="hasMore" @click="handleLoadMore" class="btn btn-neutral w-full" :disabled="loadingMore">
            {{ loadingMore ? 'Loading...' : 'Load More' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>