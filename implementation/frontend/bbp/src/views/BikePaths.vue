<script setup lang="ts">
import { ref, onMounted, toRaw} from 'vue'
import { useRouter } from 'vue-router'
import { Filter, Plus, X, Search, Eraser, Star, Bike, UsersRound, User } from 'lucide-vue-next'
import { getUserBikePaths, searchBikePaths } from '@/services/bikePath'
import { getMapboxApiKey } from '@/config/mapbox'
import { generateStaticMapUrl } from '@/utils/mapStatic'
import { useToast } from '@/composables/useToast'
import { formatDistance, formatScore } from '@/utils/format'
import { formatDate } from '@/utils/date'
import type { BikePathResponse } from '@/types/bikePath'

const router = useRouter()
const { show } = useToast()

const bikePaths = ref<BikePathResponse[]>([])
const loading = ref(false)
const currentPage = ref(0)
const hasMore = ref(false)

const isFilterModalOpen = ref(false)
const originFilter = ref('')
const destinationFilter = ref('')
const createdAtFrom = ref('')
const createdAtTo = ref('')
const hasActiveFilters = ref(false)

async function loadBikePaths() {
  loading.value = true
  try {
    const response = await getUserBikePaths(currentPage.value, 6, 'createdAt', 'DESC')
    bikePaths.value = response.content
    hasMore.value = response.hasNext
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to load bike paths'
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
        ? await searchBikePaths(
            {
              origin: originFilter.value || undefined,
              destination: destinationFilter.value || undefined,
              createdAtFrom: createdAtFrom.value ? new Date(createdAtFrom.value).toISOString() : undefined,
              createdAtTo: createdAtTo.value ? new Date(createdAtTo.value).toISOString() : undefined
            },
            currentPage.value,
            6,
            'createdAt',
            'DESC'
        )
        : await getUserBikePaths(currentPage.value, 6, 'createdAt', 'DESC')

    bikePaths.value.push(...response.content)
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
  createdAtFrom.value = ''
  createdAtTo.value = ''
}

async function applyFilters() {
  loading.value = true
  currentPage.value = 0
  hasActiveFilters.value = !!(originFilter.value || destinationFilter.value || createdAtFrom.value || createdAtTo.value)

  try {
    const response = await searchBikePaths(
        {
          origin: originFilter.value || undefined,
          destination: destinationFilter.value || undefined,
          createdAtFrom: createdAtFrom.value ? new Date(createdAtFrom.value).toISOString() : undefined,
          createdAtTo: createdAtTo.value ? new Date(createdAtTo.value).toISOString() : undefined
        },
        0,
        6,
        'createdAt',
        'DESC'
    )

    bikePaths.value = response.content
    hasMore.value = response.hasNext
    show(`Found ${response.totalElements} bike paths`, 'success')
  } catch (error: any) {
    const message = error.response?.data?.message || 'Search failed'
    show(message, 'error')
    bikePaths.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }

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
  <div class="p-6 overflow-x-hidden">
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
              <span class="label-text">Created From</span>
            </label>
            <input
                type="datetime-local"
                v-model="createdAtFrom"
                class="input input-bordered w-full"
            />
          </div>

          <div>
            <label class="label">
              <span class="label-text">Created To</span>
            </label>
            <input
                type="datetime-local"
                v-model="createdAtTo"
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

    <div v-if="loading && bikePaths.length === 0" class="flex justify-center items-center py-12">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <div v-else-if="bikePaths.length === 0" class="text-center py-12">
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