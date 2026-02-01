<script setup lang="ts">
/**
 * Bike path detail page.
 * - Loads a single bike path (from router history state cache when available, otherwise via API).
 * - Initializes an interactive Mapbox map and draws route + markers + obstacles.
 * - Supports editing bike path metadata (description/status/published) and editing individual obstacles.
 * - Allows deleting the bike path with a confirmation modal.
 */
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
import { useAsyncState } from '@/composables/useAsyncState'
import { useFieldError } from '@/composables/useFieldError'
import { validateOptionalDescription, validateAndShow } from '@/utils/validation'
import { formatDistance, formatScore } from '@/utils/format'
import { formatDateTime } from '@/utils/date'
import { OBSTACLE_TYPE_OPTIONS, OBSTACLE_SEVERITY_OPTIONS } from '@/constants/obstacle'
import { BIKE_PATH_STATUS_OPTIONS } from '@/constants/bikePath'
import { OBSTACLE_SEVERITY_COLORS } from '@/constants/map'
import { DESCRIPTION_MAX_LENGTH } from '@/constants/validation'
import { SPINNER_DELAY_MS } from '@/constants/ui'
import type { BikePathResponse, BikePathStatus, BikePathUpdateRequest } from '@/types/bikePath'
import type { ObstacleType, ObstacleSeverity } from '@/types/obstacle'

type ActiveObstacleEditData = {
  type: ObstacleType
  severity: ObstacleSeverity
  active: boolean
}

const route = useRoute()
const router = useRouter()

/** Toast helper for success/error feedback. */
const { show } = useToast()

/** Field-level error helpers used by validation utilities. */
const { hasError, setError } = useFieldError()

/** Async state for loading / updating the bike path. */
const { data: bikePath, execute } = useAsyncState<BikePathResponse>()

/** Async state for delete operation (kept separate for cleaner contexts/logging). */
const { execute: executeDelete } = useAsyncState<void>()

/** UI state flags for disabling actions while requests are in flight. */
const saving = ref(false)
const deleting = ref(false)

/** Map container DOM reference required for Mapbox initialization. */
const mapContainer = ref<HTMLElement | null>(null)

/** Editing mode for bike path metadata. */
const isEditing = ref(false)
const editDescription = ref('')
const editStatus = ref<BikePathStatus>('GOOD')
const editPublished = ref(false)

/**
 * Used for optimistic concurrency control:
 * we send `version` in update requests and replace it after a successful save.
 */
const originalVersion = ref<number>(0)

/** Obstacle editing: holds the obstacle currently being edited and its draft values. */
const editingObstacleId = ref<number | null>(null)
const obstacleEditData = ref<ActiveObstacleEditData>({
  type: 'POTHOLE',
  severity: 'LOW',
  active: true
})

/**
 * Delete confirmation modal driven by native <dialog>.
 * We open/close it via showModal()/close() for consistent behavior (Esc / backdrop). [page:1]
 */
const deleteDialog = ref<HTMLDialogElement | null>(null)
const isDeleteBikePathModalOpen = ref(false)

/**
 * Spinner state with delayed activation to prevent "flash" on fast responses.
 */
const showSpinner = ref(false)
let spinnerTimeout: ReturnType<typeof window.setTimeout> | null = null

/** Cache token so we don't call getMapboxApiKey() multiple times. */
const mapboxToken = getMapboxApiKey()

/**
 * Map composable:
 * - interactive: true enables pan/zoom.
 * - enableGeolocation: false disables user-location features for this view.
 */
const { map, isReady, initMap } = useMap({
  container: mapContainer,
  accessToken: mapboxToken,
  interactive: true,
  enableGeolocation: false
})

/** Route/marker drawing utilities bound to the map instance. */
const { drawRoute, addMarkers } = useMapRoute(map)

/** Obstacles drawing utilities bound to the map instance. */
const { addObstacles, clearObstacles } = useMapObstacles(map)

/**
 * Loads bike path details.
 * Optimization: if router history state includes a matching bikePath object, use it and skip the network call.
 * @returns Promise resolved when state is set and (if needed) the spinner is cleared.
 */
async function loadBikePath(): Promise<void> {
  const bikePathId = Number(route.params.id)

  // Router "state" cache (set by list/finder pages when navigating to detail).
  const stateData = history.state?.bikePath as BikePathResponse | undefined

  if (stateData && stateData.id === bikePathId) {
    bikePath.value = stateData
    originalVersion.value = stateData.version
    // Ensure DOM is ready before initializing map (container must exist).
    await nextTick()
    return
  }

  // Show spinner only if the request isn't quick.
  spinnerTimeout = window.setTimeout(() => {
    showSpinner.value = true
  }, SPINNER_DELAY_MS)

  const result = await execute(() => getBikePathById(bikePathId), 'BikePathDetail.loadBikePath')
  if (result) originalVersion.value = result.version

  if (spinnerTimeout) {
    clearTimeout(spinnerTimeout)
    spinnerTimeout = null
  }
  showSpinner.value = false
}

/**
 * Watch both map readiness and bike path payload.
 * When both are available, draw the route, add origin/destination markers, and add obstacles. [page:0]
 */
watch([isReady, bikePath], ([ready, path]) => {
  if (!ready || !path) return

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

  // Ensure obstacles are in sync with the latest loaded data.
  clearObstacles()
  addObstacles(path.obstacles)
})

/**
 * Navigates back to the appropriate page.
 * If user came from BikePathFinder, returns there; otherwise returns to the bike paths list.
 */
function goBack(): void {
  const from = history.state?.from
  if (from === 'BikePathFinder') router.push({ name: 'BikePathFinder' })
  else router.push('/bike-paths')
}

/**
 * Returns a UI color for a given obstacle severity.
 * @param severity - Obstacle severity level
 * @returns CSS color string
 */
function getSeverityColor(severity: ObstacleSeverity): string {
  return OBSTACLE_SEVERITY_COLORS[severity]
}

/**
 * Select helpers for dropdowns.
 * They blur the active element after selection so the dropdown closes consistently.
 */
function selectStatus(value: BikePathStatus): void {
  editStatus.value = value
  ;(document.activeElement as HTMLElement | null)?.blur()
}

function selectObstacleType(value: ObstacleType): void {
  obstacleEditData.value.type = value
  ;(document.activeElement as HTMLElement | null)?.blur()
}

function selectObstacleSeverity(value: ObstacleSeverity): void {
  obstacleEditData.value.severity = value
  ;(document.activeElement as HTMLElement | null)?.blur()
}

function selectObstacleActive(value: boolean): void {
  obstacleEditData.value.active = value
  ;(document.activeElement as HTMLElement | null)?.blur()
}

/**
 * Toggles edit mode for bike path metadata.
 * When entering edit mode, initializes draft fields from the current bike path.
 * When leaving edit mode, cancels any obstacle edit in progress.
 */
function toggleEdit(): void {
  if (!bikePath.value) return

  if (isEditing.value) {
    isEditing.value = false
    editingObstacleId.value = null
    return
  }

  editDescription.value = bikePath.value.description || ''
  editStatus.value = bikePath.value.status
  editPublished.value = bikePath.value.published
  isEditing.value = true
}

/**
 * Saves the edited bike path metadata.
 * Uses `originalVersion` to support optimistic concurrency; updates local state on success.
 */
async function handleSave(): Promise<void> {
  if (!bikePath.value) return

  // Frontend validation (optional description with max length).
  if (
      !validateAndShow(
          validateOptionalDescription(editDescription.value, DESCRIPTION_MAX_LENGTH),
          'description',
          setError,
          show
      )
  ) {
    return
  }

  saving.value = true

  try {
    await execute(
        async () => {
          const updateRequest: BikePathUpdateRequest = {
            version: originalVersion.value,
            description: editDescription.value || undefined,
            // Send status/published only when they actually changed.
            status: editStatus.value !== bikePath.value!.status ? editStatus.value : undefined,
            published: editPublished.value !== bikePath.value!.published ? editPublished.value : undefined
          }
          return await updateBikePath(bikePath.value!.id, updateRequest)
        },
        'BikePathDetail.handleSave',
        (updated) => {
          bikePath.value = updated
          originalVersion.value = updated.version
          isEditing.value = false
          editingObstacleId.value = null

          // Refresh obstacles layer after update.
          clearObstacles()
          addObstacles(updated.obstacles)

          show('Bike path updated successfully', 'success')
        },
        undefined,
        setError
    )
  } finally {
    saving.value = false
  }
}

/**
 * Opens the delete confirmation modal via native dialog API. [page:1]
 */
function openDeleteBikePathModal(): void {
  if (isDeleteBikePathModalOpen.value) return
  deleteDialog.value?.showModal()
  isDeleteBikePathModalOpen.value = true
}

/**
 * Closes the delete confirmation modal and resets related UI state. [page:1]
 */
function closeDeleteBikePathModal(): void {
  if (!isDeleteBikePathModalOpen.value) return
  deleteDialog.value?.close()
  isDeleteBikePathModalOpen.value = false
}

/**
 * Deletes the current bike path.
 * Closes the modal immediately for responsive UX, then calls API and navigates back on success.
 */
async function handleDelete(): Promise<void> {
  if (!bikePath.value) return

  closeDeleteBikePathModal()
  deleting.value = true

  try {
    await executeDelete(
        () => deleteBikePath(bikePath.value!.id),
        'BikePathDetail.handleDelete',
        async () => {
          show('Bike path deleted successfully', 'success')
          await router.push('/bike-paths')
        }
    )
  } finally {
    deleting.value = false
  }
}

/**
 * Starts editing an obstacle by copying its current values into `obstacleEditData`.
 * @param obstacleId - Obstacle ID to edit
 */
function startEditObstacle(obstacleId: number): void {
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

/**
 * Cancels obstacle editing mode (does not persist changes).
 */
function cancelEditObstacle(): void {
  editingObstacleId.value = null
}

/**
 * Saves a single obstacle update using the bike path update endpoint.
 * @param obstacleId - Obstacle ID being updated
 */
async function saveObstacle(obstacleId: number): Promise<void> {
  if (!bikePath.value) return

  saving.value = true

  try {
    await execute(
        async () => {
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
          return await updateBikePath(bikePath.value!.id, updateRequest)
        },
        'BikePathDetail.saveObstacle',
        (updated) => {
          bikePath.value = updated
          originalVersion.value = updated.version
          editingObstacleId.value = null

          // Refresh obstacles layer after update.
          clearObstacles()
          addObstacles(updated.obstacles)

          show('Obstacle updated successfully', 'success')
        },
        undefined,
        setError
    )
  } finally {
    saving.value = false
  }
}

/**
 * Store original body overflow/height so we can restore on unmount.
 * This protects other pages from style side-effects.
 */
const originalBodyOverflow = document.body.style.overflow
const originalBodyHeight = document.body.style.height

onMounted(async () => {
  // Ensure scrolling behaves as expected on this page (map + long content).
  document.body.style.overflow = 'auto'
  document.body.style.height = 'auto'

  await loadBikePath()
  initMap()
})

onUnmounted(() => {
  // Cleanup delayed spinner if component unmounts mid-request.
  if (spinnerTimeout) {
    clearTimeout(spinnerTimeout)
    spinnerTimeout = null
  }

  // Restore body styles to avoid leaking layout changes to other routes.
  document.body.style.overflow = originalBodyOverflow
  document.body.style.height = originalBodyHeight
})
</script>

<template>
  <div v-if="showSpinner" class="flex h-screen items-center justify-center">
    <span class="loading loading-spinner loading-lg"></span>
  </div>

  <div v-else-if="!bikePath" class="p-6 text-center py-12">
    <p class="text-gray-500">Bike path not found</p>
    <button @click="goBack" class="btn btn-neutral mt-4">Go Back</button>
  </div>

  <div v-else class="p-6 overflow-x-hidden overflow-y-auto min-h-screen">
    <div class="space-y-6">
      <div class="space-y-4">
        <div class="flex items-start gap-4">
          <button @click="goBack" class="btn btn-ghost btn-circle shrink-0" aria-label="Back">
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
                  aria-label="Cancel edit"
              >
                <X :size="16" />
              </button>

              <button
                  v-if="isEditing"
                  @click="handleSave"
                  class="btn btn-sm btn-primary"
                  :disabled="saving"
                  aria-label="Save changes"
              >
                <Save :size="16" />
                {{ saving ? 'Saving...' : 'Save' }}
              </button>

              <button
                  v-if="!isEditing"
                  @click="toggleEdit"
                  class="btn btn-sm btn-ghost"
                  aria-label="Edit bike path"
              >
                <Edit :size="16" />
              </button>

              <button
                  @click="openDeleteBikePathModal"
                  :class="['btn btn-sm btn-ghost', isEditing ? 'text-gray-400' : 'text-error']"
                  :disabled="isEditing"
                  aria-label="Delete bike path"
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
                      :class="{ 'input-error': hasError('status') }"
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
                        :class="{ 'input-error': hasError('published') }"
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
                :class="{ 'input-error': hasError('description') }"
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
                        aria-label="Edit obstacle"
                        :disabled="!isEditing"
                        :class="!isEditing ? 'opacity-50' : ''"
                        title="Enable edit mode to edit obstacles"
                    >
                      <Edit :size="14" />
                    </button>
                  </div>

                  <div class="flex items-center gap-2 mt-2">
                    <span
                        class="badge"
                        :style="{
                        backgroundColor: getSeverityColor(obstacle.severity),
                        color: obstacle.severity === 'CRITICAL' ? 'white' : 'black'
                      }"
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
                      <label class="label"><span class="label-text">Type</span></label>
                      <div class="dropdown w-full">
                        <div
                            tabindex="0"
                            role="button"
                            class="btn btn-bordered w-full justify-between font-normal"
                            :class="{ 'input-error': hasError('type') }"
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
                      <label class="label"><span class="label-text">Severity</span></label>
                      <div class="dropdown w-full">
                        <div
                            tabindex="0"
                            role="button"
                            class="btn btn-bordered w-full justify-between font-normal"
                            :class="{ 'input-error': hasError('severity') }"
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
                      <label class="label"><span class="label-text">Status</span></label>
                      <div class="dropdown w-full">
                        <div
                            tabindex="0"
                            role="button"
                            class="btn btn-bordered w-full justify-between font-normal"
                            :class="{ 'input-error': hasError('active') }"
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

    <dialog ref="deleteDialog" class="modal" @close="isDeleteBikePathModalOpen = false">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">Delete Bike Path</h3>
        <p class="mb-6">Are you sure you want to delete this bike path? This action cannot be undone.</p>
        <div class="flex gap-2 justify-end">
          <button @click="closeDeleteBikePathModal" class="btn btn-ghost">Cancel</button>
          <button @click="handleDelete" class="btn btn-error" :disabled="deleting">
            {{ deleting ? 'Deleting...' : 'Delete' }}
          </button>
        </div>
      </div>

      <form method="dialog" class="modal-backdrop">
        <button @click="closeDeleteBikePathModal">close</button>
      </form>
    </dialog>
  </div>
</template>