<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Edit,
  Trash2,
  Save,
  X,
  Star,
  Bike,
  MapPin,
  User,
  Calendar,
  ChevronDown,
  Globe,
  Shield
} from 'lucide-vue-next'
import { getBikePathById, updateBikePath, deleteBikePath } from '@/services/bikePath'
import { getMapboxApiKey } from '@/config/mapbox'
import { useMap } from '@/composables/useMap'
import { useMapRoute } from '@/composables/useMapRoute'
import { useMapObstacles } from '@/composables/useMapObstacles'
import { useToast } from '@/composables/useToast'
import { formatDistance, formatScore } from '@/utils/format'
import { formatDateTime } from '@/utils/date'
import { parseApiError } from '@/utils/error'
import { logError } from '@/utils/logger'
import { OBSTACLE_TYPE_OPTIONS, OBSTACLE_SEVERITY_OPTIONS } from '@/constants/obstacle'
import { BIKE_PATH_STATUS_OPTIONS } from '@/constants/bikePath'
import { OBSTACLE_SEVERITY_COLORS } from '@/constants/map'
import { DESCRIPTION_MAX_LENGTH } from '@/constants/validation'
import { SPINNER_DELAY_MS } from '@/constants/ui'
import type { BikePathResponse, BikePathStatus, BikePathUpdateRequest } from '@/types/bikePath'
import type { ObstacleType, ObstacleSeverity } from '@/types/obstacle'

const route = useRoute()
const router = useRouter()
const { show } = useToast()

const bikePath = ref<BikePathResponse | null>(null)
const loading = ref(false)
const saving = ref(false)
const mapContainer = ref<HTMLElement | null>(null)

const isEditing = ref(false)
const editDescription = ref('')
const editStatus = ref<BikePathStatus>('GOOD')
const editPublished = ref(false)
const originalVersion = ref<number>(0)

const editingObstacleId = ref<number | null>(null)
const obstacleEditData = ref<{
  type: ObstacleType
  severity: ObstacleSeverity
  active: boolean
}>({ type: 'POTHOLE', severity: 'LOW', active: true })

const showDeleteBikePathModal = ref(false)

const initialLoading = ref(true)
const showSpinner = ref(false)
let spinnerTimeout: number | null = null

const { map, isReady, initMap } = useMap({
  container: mapContainer,
  accessToken: getMapboxApiKey(),
  interactive: true,
  enableGeolocation: false
})

const { drawRoute, addMarkers } = useMapRoute(map)
const { addObstacles, clearObstacles } = useMapObstacles(map)

async function loadBikePath() {
  const bikePathId = Number(route.params.id)

  const stateData = history.state?.bikePath as BikePathResponse | undefined

  if (stateData && stateData.id === bikePathId) {
    bikePath.value = stateData
    originalVersion.value = stateData.version
    initialLoading.value = false
    await nextTick()
    return
  }

  spinnerTimeout = window.setTimeout(() => {
    showSpinner.value = true
  }, SPINNER_DELAY_MS)

  try {
    bikePath.value = await getBikePathById(bikePathId)
    originalVersion.value = bikePath.value.version
  } catch (error: any) {
    logError(error, 'BikePathDetail.loadBikePath')
    show(parseApiError(error), 'error')
  } finally {
    if (spinnerTimeout) clearTimeout(spinnerTimeout)
    showSpinner.value = false
    initialLoading.value = false
  }
}

watch([isReady, bikePath], ([ready, path]) => {
  if (ready && path) {
    drawRoute(path.bikePathPoints)
    addMarkers(
        {
          address: path.origin,
          latitude: path.originLatitude,
          longitude: path.originLongitude
        },
        {
          address: path.destination,
          latitude: path.destinationLatitude,
          longitude: path.destinationLongitude
        }
    )
    addObstacles(path.obstacles)
  }
})

function goBack() {
  const from = history.state?.from

  if (from === 'BikePathFinder') {
    router.push({ name: 'BikePathFinder' })
  } else {
    router.push('/bike-paths')
  }
}

function getSeverityColor(severity: ObstacleSeverity): string {
  return OBSTACLE_SEVERITY_COLORS[severity]
}

function selectStatus(value: BikePathStatus) {
  editStatus.value = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function selectObstacleType(value: ObstacleType) {
  obstacleEditData.value.type = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function selectObstacleSeverity(value: ObstacleSeverity) {
  obstacleEditData.value.severity = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function selectObstacleActive(value: boolean) {
  obstacleEditData.value.active = value
  ;(document.activeElement as HTMLElement)?.blur()
}

function toggleEdit() {
  if (!bikePath.value) return

  if (isEditing.value) {
    isEditing.value = false
    editingObstacleId.value = null
  } else {
    editDescription.value = bikePath.value.description || ''
    editStatus.value = bikePath.value.status
    editPublished.value = bikePath.value.published
    isEditing.value = true
  }
}

async function handleSave() {
  if (!bikePath.value) return

  saving.value = true

  try {
    const updateRequest: BikePathUpdateRequest = {
      version: originalVersion.value,
      description: editDescription.value || undefined,
      status: editStatus.value !== bikePath.value.status ? editStatus.value : undefined,
      published: editPublished.value !== bikePath.value.published ? editPublished.value : undefined
    }

    const updated = await updateBikePath(bikePath.value.id, updateRequest)
    bikePath.value = updated
    originalVersion.value = updated.version
    isEditing.value = false
    editingObstacleId.value = null

    clearObstacles()
    addObstacles(updated.obstacles)

    show('Bike path updated successfully', 'success')
  } catch (error: any) {
    logError(error, 'BikePathDetail.handleSave')
    show(parseApiError(error), 'error')
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!bikePath.value) return

  loading.value = true

  try {
    await deleteBikePath(bikePath.value.id)
    show('Bike path deleted successfully', 'success')
    await router.push('/bike-paths')
  } catch (error: any) {
    logError(error, 'BikePathDetail.handleDelete')
    show(parseApiError(error), 'error')
  } finally {
    loading.value = false
    showDeleteBikePathModal.value = false
  }
}

function startEditObstacle(obstacleId: number) {
  if (!bikePath.value) return

  const obstacle = bikePath.value.obstacles.find(o => o.id === obstacleId)
  if (!obstacle) return

  editingObstacleId.value = obstacleId
  obstacleEditData.value = {
    type: obstacle.type,
    severity: obstacle.severity,
    active: obstacle.active
  }
}

function cancelEditObstacle() {
  editingObstacleId.value = null
}

async function saveObstacle(obstacleId: number) {
  if (!bikePath.value) return

  saving.value = true

  try {
    const updateRequest: BikePathUpdateRequest = {
      version: originalVersion.value,
      obstaclesToUpdate: [
        {
          id: obstacleId,
          type: obstacleEditData.value.type,
          severity: obstacleEditData.value.severity,
          active: obstacleEditData.value.active
        }
      ]
    }

    const updated = await updateBikePath(bikePath.value.id, updateRequest)
    bikePath.value = updated
    originalVersion.value = updated.version
    editingObstacleId.value = null

    clearObstacles()
    addObstacles(updated.obstacles)

    show('Obstacle updated successfully', 'success')
  } catch (error: any) {
    logError(error, 'BikePathDetail.saveObstacle')
    show(parseApiError(error), 'error')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  document.body.style.overflow = 'auto'
  document.body.style.height = 'auto'

  await loadBikePath()
  initMap()
})

onUnmounted(() => {
  document.body.style.overflow = 'auto'
  document.body.style.height = 'auto'
})
</script>

<template>
  <div v-if="showSpinner" class="flex h-screen items-center justify-center">
    <span class="loading loading-spinner loading-lg"></span>
  </div>

  <div v-else-if="!initialLoading" class="p-6 overflow-x-hidden overflow-y-auto min-h-screen">
    <div v-if="!bikePath" class="text-center py-12">
      <p class="text-gray-500">Bike path not found</p>
      <button @click="goBack" class="btn btn-neutral mt-4">Go Back</button>
    </div>

    <div v-else class="space-y-6">
      <div class="space-y-4">
        <div class="flex items-start gap-4">
          <button @click="goBack" class="btn btn-ghost btn-circle shrink-0">
            <ArrowLeft :size="20" />
          </button>
          <h1 class="text-3xl font-bold wrap-break-word flex-1">
            {{ bikePath.origin }} → {{ bikePath.destination }}
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
            <h2 class="card-title">Bike Path Information</h2>
            <div class="flex gap-2">
              <button
                  v-if="isEditing"
                  @click="toggleEdit"
                  class="btn btn-sm btn-ghost"
                  :disabled="saving"
              >
                <X :size="16" />
              </button>
              <button
                  v-if="isEditing"
                  @click="handleSave"
                  class="btn btn-sm btn-primary"
                  :disabled="saving"
              >
                <Save :size="16" />
                {{ saving ? 'Saving...' : 'Save' }}
              </button>
              <button
                  v-if="!isEditing"
                  @click="toggleEdit"
                  class="btn btn-sm btn-ghost"
              >
                <Edit :size="16" />
              </button>
              <button
                  @click="showDeleteBikePathModal = true"
                  :class="['btn btn-sm btn-ghost', isEditing ? 'text-gray-400' : 'text-error']"
                  :disabled="isEditing"
              >
                <Trash2 :size="16" />
              </button>
            </div>
          </div>

          <div class="flex flex-wrap gap-4 mb-6">
            <div class="flex items-start gap-2 flex-1 min-w-[250px]">
              <MapPin :size="18" class="text-success shrink-0 mt-0.5" />
              <div class="flex-1">
                <span class="font-medium text-sm text-gray-500">From:</span>
                <p class="text-gray-700">{{ bikePath.origin }}</p>
              </div>
            </div>
            <div class="flex items-start gap-2 flex-1 min-w-[250px]">
              <MapPin :size="18" class="text-purple-500 shrink-0 mt-0.5" />
              <div class="flex-1">
                <span class="font-medium text-sm text-gray-500">To:</span>
                <p class="text-gray-700">{{ bikePath.destination }}</p>
              </div>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="space-y-4">
              <div class="flex items-center gap-2">
                <Star :size="18" class="text-warning fill-warning" />
                <span class="font-medium">Score:</span>
                <span>{{ formatScore(bikePath.score) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Bike :size="18" />
                <span class="font-medium">Distance:</span>
                <span>{{ formatDistance(bikePath.totalDistance) }}</span>
              </div>
            </div>

            <div class="space-y-4">
              <div class="flex items-center gap-2">
                <Shield :size="18" />
                <span class="font-medium">Status:</span>
                <div v-if="isEditing" class="dropdown flex-1">
                  <div
                      tabindex="0"
                      role="button"
                      class="btn btn-sm btn-bordered w-full justify-between font-normal"
                  >
                    {{ BIKE_PATH_STATUS_OPTIONS.find(o => o.value === editStatus)?.label }}
                    <ChevronDown :size="16" />
                  </div>
                  <ul
                      tabindex="0"
                      class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow max-h-60 overflow-y-auto"
                  >
                    <li v-for="option in BIKE_PATH_STATUS_OPTIONS" :key="option.value">
                      <a @click="selectStatus(option.value)">{{ option.label }}</a>
                    </li>
                  </ul>
                </div>
                <span v-else>{{ bikePath.statusDescription }}</span>
              </div>

              <div class="flex items-center gap-2">
                <Globe :size="18" />
                <span class="font-medium">Visibility:</span>
                <div v-if="isEditing" class="form-control">
                  <label class="label cursor-pointer justify-start gap-2 py-0">
                    <input
                        type="checkbox"
                        v-model="editPublished"
                        class="checkbox checkbox-sm checkbox-primary"
                    />
                    <span class="label-text">Public</span>
                  </label>
                </div>
                <span v-else>{{ bikePath.published ? 'Public' : 'Private' }}</span>
              </div>
            </div>
          </div>

          <div class="mt-6">
            <label class="block font-medium mb-2">Description</label>
            <textarea
                v-if="isEditing"
                v-model="editDescription"
                class="textarea textarea-bordered w-full"
                rows="3"
                :maxlength="DESCRIPTION_MAX_LENGTH"
                placeholder="Add a description..."
            ></textarea>
            <p v-else-if="bikePath.description" class="text-gray-700">{{ bikePath.description }}</p>
            <p v-else class="text-gray-500 italic">No description</p>
          </div>

          <div class="mt-6 pt-6 border-t grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
            <div class="flex items-center gap-2">
              <User :size="14" />
              <span>Created by: {{ bikePath.createdByUsername }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Calendar :size="14" />
              <span>Created: {{ formatDateTime(bikePath.createdAt) }}</span>
            </div>
            <div v-if="bikePath.updatedByUsername" class="flex items-center gap-2">
              <User :size="14" />
              <span>Updated by: {{ bikePath.updatedByUsername }}</span>
            </div>
            <div v-if="bikePath.updatedAt" class="flex items-center gap-2">
              <Calendar :size="14" />
              <span>Updated: {{ formatDateTime(bikePath.updatedAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">
            Obstacles ({{ bikePath.obstacles.filter(o => o.active).length }})
          </h2>

          <div v-if="bikePath.obstacles.length === 0" class="text-center py-8 text-gray-500">
            No obstacles reported on this path
          </div>

          <div v-else class="space-y-4">
            <div
                v-for="obstacle in bikePath.obstacles"
                :key="obstacle.id"
                class="card bg-base-200 shadow"
            >
              <div class="card-body p-4">
                <div v-if="editingObstacleId !== obstacle.id">
                  <div class="flex items-start justify-between mb-2">
                    <div class="flex-1">
                      <h3 class="font-semibold text-lg">{{ obstacle.typeDescription }}</h3>
                      <p class="text-sm text-gray-600 mt-1">{{ obstacle.address }}</p>
                    </div>
                    <button
                        @click="startEditObstacle(obstacle.id)"
                        class="btn btn-sm btn-ghost"
                    >
                      <Edit :size="14" />
                    </button>
                  </div>
                  <div class="flex items-center gap-2 mt-2">
                    <span
                        class="badge"
                        :style="{ backgroundColor: getSeverityColor(obstacle.severity), color: obstacle.severity === 'CRITICAL' ? 'white' : 'black' }"
                    >
                      {{ obstacle.severityDescription }}
                    </span>
                    <span :class="['badge', obstacle.active ? 'badge-warning' : 'badge-neutral']">
                      {{ obstacle.active ? 'Active' : 'Inactive' }}
                    </span>
                  </div>
                </div>

                <div v-else class="space-y-4">
                  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div>
                      <label class="label">
                        <span class="label-text">Type</span>
                      </label>
                      <div class="dropdown w-full">
                        <div
                            tabindex="0"
                            role="button"
                            class="btn btn-bordered w-full justify-between font-normal"
                        >
                          {{ OBSTACLE_TYPE_OPTIONS.find(o => o.value === obstacleEditData.type)?.label }}
                          <ChevronDown :size="16" />
                        </div>
                        <ul
                            tabindex="0"
                            class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow max-h-60 overflow-y-auto"
                        >
                          <li v-for="type in OBSTACLE_TYPE_OPTIONS" :key="type.value">
                            <a @click="selectObstacleType(type.value)">{{ type.label }}</a>
                          </li>
                        </ul>
                      </div>
                    </div>
                    <div>
                      <label class="label">
                        <span class="label-text">Severity</span>
                      </label>
                      <div class="dropdown w-full">
                        <div
                            tabindex="0"
                            role="button"
                            class="btn btn-bordered w-full justify-between font-normal"
                        >
                          {{ OBSTACLE_SEVERITY_OPTIONS.find(o => o.value === obstacleEditData.severity)?.label }}
                          <ChevronDown :size="16" />
                        </div>
                        <ul
                            tabindex="0"
                            class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow"
                        >
                          <li v-for="severity in OBSTACLE_SEVERITY_OPTIONS" :key="severity.value">
                            <a @click="selectObstacleSeverity(severity.value)">{{ severity.label }}</a>
                          </li>
                        </ul>
                      </div>
                    </div>
                    <div>
                      <label class="label">
                        <span class="label-text">Status</span>
                      </label>
                      <div class="dropdown w-full">
                        <div
                            tabindex="0"
                            role="button"
                            class="btn btn-bordered w-full justify-between font-normal"
                        >
                          {{ obstacleEditData.active ? 'Active' : 'Inactive' }}
                          <ChevronDown :size="16" />
                        </div>
                        <ul
                            tabindex="0"
                            class="dropdown-content menu bg-base-100 rounded-box z-1 w-full p-2 shadow"
                        >
                          <li><a @click="selectObstacleActive(true)">Active</a></li>
                          <li><a @click="selectObstacleActive(false)">Inactive</a></li>
                        </ul>
                      </div>
                    </div>
                  </div>

                  <div class="flex justify-end gap-2">
                    <button
                        @click="cancelEditObstacle"
                        class="btn btn-sm btn-ghost"
                        :disabled="saving"
                    >
                      <X :size="14" />
                      Cancel
                    </button>
                    <button
                        @click="saveObstacle(obstacle.id)"
                        class="btn btn-sm btn-primary"
                        :disabled="saving"
                    >
                      <Save :size="14" />
                      {{ saving ? 'Saving...' : 'Save' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <dialog :class="['modal', showDeleteBikePathModal && 'modal-open']">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">Delete Bike Path</h3>
        <p class="mb-6">Are you sure you want to delete this bike path? This action cannot be undone.</p>
        <div class="flex gap-2 justify-end">
          <button @click="showDeleteBikePathModal = false" class="btn btn-ghost">Cancel</button>
          <button @click="handleDelete" class="btn btn-error" :disabled="loading">
            {{ loading ? 'Deleting...' : 'Delete' }}
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="showDeleteBikePathModal = false">close</button>
      </form>
    </dialog>
  </div>
</template>