<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMapboxApiKey } from '@/config/mapbox'
import { Search, X, Eraser, Star, Bike, ArrowRight, ChevronDown } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'
import { useMap } from '@/composables/useMap'
import { useMapRoute } from '@/composables/useMapRoute'
import { useMapObstacles } from '@/composables/useMapObstacles'
import { findBikePaths } from '@/services/bikePathFinder'
import { RADIUS_OPTIONS } from '@/constants/bikePath'
import { BIKE_PATH_FINDER_PAGE_SIZE } from '@/constants/pagination'
import { formatDistance, formatScore } from '@/utils/format'
import type { BikePathResponse } from '@/types/bikePath'

const router = useRouter()
const { show } = useToast()

const mapContainer = ref<HTMLDivElement | null>(null)

// Map composables
const { map, isReady, initMap } = useMap({
  container: mapContainer,
  accessToken: getMapboxApiKey(),
  interactive: true,
  enableGeolocation: true
})
const { drawRoute, addMarkers, clearRoute } = useMapRoute(map)
const { addObstacles, clearObstacles } = useMapObstacles(map)

// Search state
const isSidebarOpen = ref(false)
const originAddress = ref('')
const originRadius = ref(0.1)
const destinationAddress = ref('')
const destinationRadius = ref(0.1)
const loading = ref(false)
const searchResults = ref<BikePathResponse[]>([])
const selectedBikePathId = ref<number | null>(null)
const currentPage = ref(0)
const hasMore = ref(false)

function selectOriginRadius(value: number) {
  originRadius.value = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function selectDestinationRadius(value: number) {
  destinationRadius.value = value
  ;(document.activeElement as HTMLElement)?.blur()
}

async function handleSearch() {
  loading.value = true
  currentPage.value = 0
  selectedBikePathId.value = null

  try {
    const response = await findBikePaths(
        {
          originAddress: originAddress.value,
          destinationAddress: destinationAddress.value,
          originRadiusKm: originRadius.value,
          destinationRadiusKm: destinationRadius.value
        },
        0,
        BIKE_PATH_FINDER_PAGE_SIZE
    )

    searchResults.value = response.content
    hasMore.value = response.hasNext
    show(`Found ${response.totalElements} bike paths`, 'success')
  } catch (error: any) {
    const message = error.response?.data?.message || 'Search failed'
    show(message, 'error')
    searchResults.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loading.value = true
  currentPage.value++

  try {
    const response = await findBikePaths(
        {
          originAddress: originAddress.value,
          destinationAddress: destinationAddress.value,
          originRadiusKm: originRadius.value,
          destinationRadiusKm: destinationRadius.value
        },
        currentPage.value,
        BIKE_PATH_FINDER_PAGE_SIZE
    )

    searchResults.value.push(...response.content)
    hasMore.value = response.hasNext
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to load more'
    show(message, 'error')
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  originAddress.value = ''
  originRadius.value = 0.1
  destinationAddress.value = ''
  destinationRadius.value = 0.1
  searchResults.value = []
  selectedBikePathId.value = null
  currentPage.value = 0
  hasMore.value = false

  clearRoute()
  clearObstacles()
}

function selectBikePath(bikePathId: number) {
  selectedBikePathId.value = bikePathId

  const selectedPath = searchResults.value.find(bp => bp.id === bikePathId)
  if (!selectedPath || !isReady.value) return

  // Clear previous route and obstacles
  clearRoute()
  clearObstacles()

  // Draw route
  drawRoute(selectedPath.bikePathPoints)

  // Add origin and destination markers
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

  // Add obstacle markers
  if (selectedPath.obstacles && selectedPath.obstacles.length > 0) {
    addObstacles(selectedPath.obstacles)
  }
}

function viewDetails(bikePathId: number) {
  const selectedBikePath = searchResults.value.find(bp => bp.id === bikePathId)

  router.push({
    name: 'BikePathDetail',
    params: { id: bikePathId },
    state: {
      bikePath: selectedBikePath,
      from: 'BikePathFinder'
    }
  })
}

onMounted(() => {
  document.body.style.overflow = 'hidden'

  initMap()

  // Close sidebar when clicking on map
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

    <!-- Search Button (only when sidebar is closed) -->
    <button
        v-if="!isSidebarOpen"
        @click="isSidebarOpen = true"
        class="btn btn-circle btn-neutral shadow-xl absolute top-4 left-4 z-30"
    >
      <Search :size="20" />
    </button>

    <!-- Sidebar -->
    <div
        :class="[
        'absolute top-0 left-0 h-full bg-base-100 shadow-2xl z-20 transition-transform duration-300',
        'w-96 overflow-y-auto',
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
      ]"
    >
      <div class="p-6">
        <!-- Header with title and X -->
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-2xl font-bold">Search Bike Paths</h2>
          <button @click="isSidebarOpen = false" class="btn btn-circle btn-ghost">
            <X :size="20" />
          </button>
        </div>

        <!-- Search Form -->
        <form @submit.prevent="handleSearch" class="space-y-4">
          <!-- Origin -->
          <div>
            <label class="label">
              <span class="label-text">Origin</span>
            </label>
            <input
                type="text"
                v-model.trim="originAddress"
                placeholder="Enter origin address"
                class="input input-bordered w-full"
                required
            />
            <label class="label">
              <span class="label-text">Radius</span>
            </label>
            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
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

          <!-- Destination -->
          <div>
            <label class="label">
              <span class="label-text">Destination</span>
            </label>
            <input
                type="text"
                v-model.trim="destinationAddress"
                placeholder="Enter destination address"
                class="input input-bordered w-full"
                required
            />
            <label class="label">
              <span class="label-text">Radius</span>
            </label>
            <div class="dropdown w-full">
              <div
                  tabindex="0"
                  role="button"
                  class="btn btn-bordered w-full justify-between font-normal"
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

          <!-- Action Buttons -->
          <div class="flex gap-2">
            <button type="button" @click="clearSearch" class="btn btn-ghost flex-1">
              <Eraser :size="16" />
              Clear
            </button>
            <button type="submit" class="btn btn-neutral flex-1" :disabled="loading">
              <Search :size="16" />
              {{ loading ? 'Searching...' : 'Search' }}
            </button>
          </div>
        </form>

        <div class="divider"></div>

        <!-- Results Section -->
        <div v-if="loading && searchResults.length === 0" class="text-center text-gray-500">
          <span class="loading loading-spinner loading-md"></span>
          <p class="mt-2">Searching...</p>
        </div>

        <div v-else-if="searchResults.length === 0" class="text-center text-gray-500">
          <p>No results found. Try searching!</p>
        </div>

        <div v-else class="space-y-3">
          <!-- Result Cards -->
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
            <!-- Origin -->
            <div class="flex items-center gap-2 mb-1">
              <span class="text-xs text-gray-500">From:</span>
              <p class="truncate flex-1 font-medium">{{ bikePath.origin }}</p>
            </div>

            <!-- Destination -->
            <div class="flex items-center gap-2 mb-3">
              <span class="text-xs text-gray-500">To:</span>
              <p class="truncate flex-1 font-medium">{{ bikePath.destination }}</p>
            </div>

            <!-- Score, Distance, Status -->
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

            <!-- Description (if present) -->
            <p v-if="bikePath.description" class="text-sm text-gray-600 line-clamp-2 mb-3">
              {{ bikePath.description }}
            </p>

            <!-- View Details Button -->
            <button @click.stop="viewDetails(bikePath.id)" class="btn btn-sm btn-neutral w-full">
              View Details
              <ArrowRight :size="16" />
            </button>
          </div>

          <!-- Load More Button -->
          <button
              v-if="hasMore"
              @click="loadMore"
              class="btn btn-neutral w-full"
              :disabled="loading"
          >
            {{ loading ? 'Loading...' : 'Load More' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>