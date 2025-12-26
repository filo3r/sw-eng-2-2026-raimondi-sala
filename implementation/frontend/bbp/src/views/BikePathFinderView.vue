<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import mapboxgl from 'mapbox-gl'
import { getMapboxApiKey } from '@/config/mapbox'
import { Search, X, Eraser, Star, Bike, ArrowRight, ChevronDown } from 'lucide-vue-next'
import api from '@/api/axios'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const mapContainer = ref<HTMLDivElement | null>(null)
let map: mapboxgl.Map | null = null

const { show } = useToast()

const isSidebarOpen = ref(false)
const originAddress = ref('')
const originRadius = ref(0.1)
const destinationAddress = ref('')
const destinationRadius = ref(0.1)
const loading = ref(false)
const searchResults = ref<any[]>([])
const selectedBikePathId = ref<number | null>(null)
const currentPage = ref(0)
const hasMore = ref(false)
const pageSize = 5

const radiusOptions = [
  { value: 0.1, label: '100m' },
  { value: 0.25, label: '250m' },
  { value: 0.5, label: '500m' },
  { value: 1, label: '1 km' },
  { value: 2, label: '2 km' },
  { value: 5, label: '5 km' },
  { value: 10, label: '10 km' }
]

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
    const response = await api.post('/api/finder/bike-paths', {
      originAddress: originAddress.value,
      destinationAddress: destinationAddress.value,
      originRadiusKm: originRadius.value,
      destinationRadiusKm: destinationRadius.value
    }, {
      params: { page: 0, size: pageSize }
    })

    searchResults.value = response.data.content
    hasMore.value = response.data.hasNext
    show(`Found ${response.data.totalElements} bike paths`, 'success')
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
    const response = await api.post('/api/finder/bike-paths', {
      originAddress: originAddress.value,
      destinationAddress: destinationAddress.value,
      originRadiusKm: originRadius.value,
      destinationRadiusKm: destinationRadius.value
    }, {
      params: { page: currentPage.value, size: pageSize }
    })

    searchResults.value.push(...response.data.content)
    hasMore.value = response.data.hasNext
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
}

function selectBikePath(bikePathId: number) {
  selectedBikePathId.value = bikePathId
  // TODO: mostrare info sulla mappa (implementazione successiva)
}

function viewDetails(bikePathId: number) {
  router.push({ name: 'BikePathDetail', params: { id: bikePathId } })
}

onMounted(() => {
  document.body.style.overflow = 'hidden'

  if (!mapContainer.value) return

  mapboxgl.accessToken = getMapboxApiKey()

  map = new mapboxgl.Map({
    container: mapContainer.value,
    style: 'mapbox://styles/mapbox/outdoors-v12',
    center: [9.19, 45.46],
    zoom: 12,
    pitch: 0,
    bearing: 0,
    collectResourceTiming: false,
    attributionControl: false
  })

  map.addControl(new mapboxgl.AttributionControl({ compact: true }), 'bottom-right')
  map.addControl(new mapboxgl.NavigationControl(), 'top-right')

  const geolocateControl = new mapboxgl.GeolocateControl({
    positionOptions: {
      enableHighAccuracy: true
    },
    trackUserLocation: true,
    showUserHeading: true
  })

  map.addControl(geolocateControl, 'top-right')

  map.on('load', () => {
    const compassButton = document.querySelector('.mapboxgl-ctrl-compass')
    compassButton?.addEventListener('click', () => {
      map?.easeTo({
        pitch: 0,
        bearing: 0,
        duration: 500
      })
    })

    navigator.geolocation.getCurrentPosition(
        (_position) => {
          geolocateControl.trigger()
        },
        (_error) => {
          console.log('Geolocation not available, staying centered on Milan')
        },
        {
          enableHighAccuracy: true,
          timeout: 5000
        }
    )
  })

  setTimeout(() => {
    map?.resize()
  }, 300)
})

onUnmounted(() => {
  document.body.style.overflow = ''
  map?.remove()
})
</script>

<template>
  <div class="h-full w-full overflow-hidden relative">
    <div ref="mapContainer" class="h-full w-full"></div>

    <!-- Search Button -->
    <button
        @click="isSidebarOpen = !isSidebarOpen"
        class="btn btn-circle btn-neutral shadow-xl absolute top-4 left-4 z-30"
    >
      <Search v-if="!isSidebarOpen" :size="20" />
      <X v-else :size="20" />
    </button>

    <!-- Sidebar -->
    <div
        :class="[
        'absolute top-0 left-0 h-full bg-base-100 shadow-2xl z-20 transition-transform duration-300',
        'w-96 overflow-y-auto',
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
      ]"
    >
      <div class="p-6 pt-20">
        <h2 class="text-2xl font-bold mb-6">Search Bike Paths</h2>

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
                {{ radiusOptions.find(o => o.value === originRadius)?.label }}
                <ChevronDown :size="16" />
              </div>
              <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-[1] w-full p-2 shadow">
                <li v-for="option in radiusOptions" :key="option.value">
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
                {{ radiusOptions.find(o => o.value === destinationRadius)?.label }}
                <ChevronDown :size="16" />
              </div>
              <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-[1] w-full p-2 shadow">
                <li v-for="option in radiusOptions" :key="option.value">
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
                <span>{{ bikePath.score }}</span>
              </div>
              <div class="flex items-center gap-1">
                <Bike :size="16" />
                <span>{{ bikePath.totalDistance }} km</span>
              </div>
              <span class="text-gray-600">{{ bikePath.statusDescription }}</span>
            </div>

            <!-- Description (se presente) -->
            <p v-if="bikePath.description" class="text-sm text-gray-600 line-clamp-2 mb-3">
              {{ bikePath.description }}
            </p>

            <!-- View Details Button -->
            <button
                @click.stop="viewDetails(bikePath.id)"
                class="btn btn-sm btn-neutral w-full"
            >
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