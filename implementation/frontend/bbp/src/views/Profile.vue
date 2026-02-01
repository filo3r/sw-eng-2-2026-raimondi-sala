<script setup lang="ts">
/**
 * Profile page.
 * - Loads current user data on mount and displays it in read-only mode by default.
 * - Allows editing user fields (name, surname, username, email) and optionally changing password.
 * - Supports logout and account deletion with confirmation modal.
 *
 * Notes:
 * - Uses a delayed spinner (SPINNER_DELAY_MS) to avoid UI flicker on fast requests.
 * - Keeps a copy of the originally loaded user data to support "Cancel" restoring fields
 *   and to compute a minimal update payload (send only changed fields).
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Mail, Lock, UserCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useAsyncState } from '@/composables/useAsyncState'
import { useFieldError } from '@/composables/useFieldError'
import { getCurrentUser, updateCurrentUser, deleteCurrentUser } from '@/services/user'
import {
  validateName,
  validateSurname,
  validateUsername,
  validateEmail,
  validateOptionalPassword,
  validateAndShow
} from '@/utils/validation'
import type { UserUpdateRequest, UserResponse } from '@/types/user'
import {
  USER_NAME_MAX_LENGTH,
  USER_SURNAME_MAX_LENGTH,
  USERNAME_MAX_LENGTH,
  EMAIL_MAX_LENGTH,
  PASSWORD_MIN_LENGTH
} from '@/constants/validation'
import { SPINNER_DELAY_MS } from '@/constants/ui'

/** Router instance used for SPA navigation (e.g., redirect to /login on logout/delete). */
const router = useRouter()

/** Auth store for clearing auth on logout/delete. */
const authStore = useAuthStore()

/** Toast helper for user feedback. */
const { show } = useToast()

/** Field-level error helpers used by validation and API error mapping. */
const { hasError, setError } = useFieldError()

/** Async state executor for load/update operations. */
const { execute } = useAsyncState<UserResponse>()

/** Async state executor for delete operation (kept separate for clearer context names). */
const { execute: executeDelete } = useAsyncState<void>()

/** Form fields bound to the UI. */
const name = ref('')
const surname = ref('')
const username = ref('')
const email = ref('')
const password = ref('')

/**
 * Snapshot of the last successfully loaded user profile.
 * Used to:
 * - restore values on Cancel
 * - build a minimal update payload on Save
 */
const originalData = ref<UserResponse | null>(null)

/** UI state. */
const isEditing = ref(false)
const saving = ref(false)
const deleting = ref(false)
const showDeleteModal = ref(false)

/** Spinner state with delayed activation to avoid flicker. */
const showSpinner = ref(false)
const initialLoadComplete = ref(false)
let spinnerTimeout: ReturnType<typeof window.setTimeout> | null = null

/**
 * Fetches the current user's profile and populates the form fields.
 * Uses a delayed spinner so fast requests don't flash a loader.
 */
async function fetchUserData(): Promise<void> {
  spinnerTimeout = window.setTimeout(() => {
    showSpinner.value = true
  }, SPINNER_DELAY_MS)

  await execute(
      () => getCurrentUser(),
      'Profile.fetchUserData',
      (userData) => {
        name.value = userData.name
        surname.value = userData.surname
        username.value = userData.username
        email.value = userData.email
        originalData.value = userData
        initialLoadComplete.value = true
      }
  )

  if (spinnerTimeout) {
    clearTimeout(spinnerTimeout)
    spinnerTimeout = null
  }
  showSpinner.value = false
}

/**
 * Toggles edit mode.
 * If leaving edit mode (Cancel), restores values from `originalData` and clears password input.
 */
function toggleEdit(): void {
  if (isEditing.value && originalData.value) {
    name.value = originalData.value.name
    surname.value = originalData.value.surname
    username.value = originalData.value.username
    email.value = originalData.value.email
    password.value = ''
  }
  isEditing.value = !isEditing.value
}

/**
 * Saves profile changes.
 * - Validates fields client-side.
 * - Builds a minimal update payload containing only changed fields.
 * - Refreshes user data after successful update to keep `originalData` in sync.
 */
async function handleSave(): Promise<void> {
  // Frontend validation
  if (!validateAndShow(validateName(name.value), 'name', setError, show)) return
  if (!validateAndShow(validateSurname(surname.value), 'surname', setError, show)) return
  if (!validateAndShow(validateUsername(username.value), 'username', setError, show)) return
  if (!validateAndShow(validateEmail(email.value), 'email', setError, show)) return
  if (!validateAndShow(validateOptionalPassword(password.value), 'password', setError, show)) return

  saving.value = true

  const updateData: UserUpdateRequest = {}

  // Send only fields that changed; password is optional and only sent when provided.
  if (originalData.value) {
    if (name.value !== originalData.value.name) updateData.name = name.value
    if (surname.value !== originalData.value.surname) updateData.surname = surname.value
    if (username.value !== originalData.value.username) updateData.username = username.value
    if (email.value !== originalData.value.email) updateData.email = email.value
    if (password.value) updateData.password = password.value
  }

  try {
    await execute(
        () => updateCurrentUser(updateData),
        'Profile.handleSave',
        async () => {
          await fetchUserData()
          password.value = ''
          isEditing.value = false
          show('Profile updated successfully!', 'success')
        },
        undefined,
        setError
    )
  } finally {
    saving.value = false
  }
}

/**
 * Logs out the current user.
 * Clears auth state and redirects to login.
 */
async function handleLogout(): Promise<void> {
  authStore.clearAuth()
  show('Logged out successfully', 'success')
  await router.push('/login')
}

/**
 * Opens the delete-account confirmation modal.
 * (This component currently uses DaisyUI "modal-toggle" checkbox pattern.) [web:92]
 */
function openDeleteModal(): void {
  showDeleteModal.value = true
}

/**
 * Confirms account deletion.
 * - Closes the modal immediately for responsive UX.
 * - Deletes the current user, clears auth, then redirects to login.
 */
async function confirmDelete(): Promise<void> {
  showDeleteModal.value = false
  deleting.value = true

  try {
    await executeDelete(
        () => deleteCurrentUser(),
        'Profile.confirmDelete',
        async () => {
          authStore.clearAuth()
          show('Account deleted successfully', 'success')
          await router.push('/login')
        }
    )
  } finally {
    deleting.value = false
  }
}

/** Initial page load: fetch user profile. */
onMounted(() => {
  void fetchUserData()
})
</script>

<template>
  <div v-if="showSpinner" class="flex h-screen items-center justify-center">
    <span class="loading loading-spinner loading-lg"></span>
  </div>

  <div v-else-if="initialLoadComplete" class="flex h-screen items-center justify-center overflow-hidden">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
      <div class="card-body items-center">
        <h2 class="card-title text-2xl">Profile</h2>

        <!-- Read-only view -->
        <div v-if="!isEditing" class="space-y-4 w-full self-stretch">
          <div class="flex items-center gap-2">
            <User :size="16" />
            <div>
              <p class="text-sm text-gray-500">Name</p>
              <p class="font-medium">{{ name }}</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <User :size="16" />
            <div>
              <p class="text-sm text-gray-500">Surname</p>
              <p class="font-medium">{{ surname }}</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <UserCircle :size="16" />
            <div>
              <p class="text-sm text-gray-500">Username</p>
              <p class="font-medium">{{ username }}</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <Mail :size="16" />
            <div>
              <p class="text-sm text-gray-500">Email</p>
              <p class="font-medium">{{ email }}</p>
            </div>
          </div>

          <div class="flex gap-2">
            <button type="button" @click="toggleEdit" class="btn btn-neutral flex-1">
              Edit
            </button>
            <button type="button" @click="handleLogout" class="btn btn-ghost flex-1">
              Logout
            </button>
          </div>
        </div>

        <!-- Edit form -->
        <form v-else @submit.prevent="handleSave" class="space-y-4 w-full self-stretch">
          <label class="input input-bordered flex items-center gap-2 w-full" :class="{ 'input-error': hasError('name') }">
            <User :size="16" />
            <input
                v-model.trim="name"
                type="text"
                class="grow"
                placeholder="Name"
                required
                :maxlength="USER_NAME_MAX_LENGTH"
                autocomplete="given-name"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full" :class="{ 'input-error': hasError('surname') }">
            <User :size="16" />
            <input
                v-model.trim="surname"
                type="text"
                class="grow"
                placeholder="Surname"
                required
                :maxlength="USER_SURNAME_MAX_LENGTH"
                autocomplete="family-name"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full" :class="{ 'input-error': hasError('username') }">
            <UserCircle :size="16" />
            <input
                v-model.trim="username"
                type="text"
                class="grow"
                placeholder="Username"
                required
                :maxlength="USERNAME_MAX_LENGTH"
                autocomplete="username"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full" :class="{ 'input-error': hasError('email') }">
            <Mail :size="16" />
            <input
                v-model.trim="email"
                type="email"
                class="grow"
                placeholder="Email"
                required
                :maxlength="EMAIL_MAX_LENGTH"
                autocomplete="email"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full" :class="{ 'input-error': hasError('password') }">
            <Lock :size="16" />
            <input
                v-model.trim="password"
                type="password"
                class="grow"
                placeholder="New Password (optional)"
                :minlength="PASSWORD_MIN_LENGTH"
                autocomplete="new-password"
            />
          </label>

          <div class="flex gap-2">
            <button type="button" @click="toggleEdit" class="btn btn-ghost flex-1">
              Cancel
            </button>
            <button type="submit" class="btn btn-neutral flex-1" :disabled="saving" :aria-busy="saving">
              {{ saving ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </form>

        <div class="divider"></div>

        <button @click="openDeleteModal" class="btn btn-error btn-outline w-full" :disabled="deleting">
          {{ deleting ? 'Deleting...' : 'Delete Account' }}
        </button>
      </div>
    </div>

    <!-- DaisyUI modal-toggle pattern (checkbox-driven modal). [web:92] -->
    <input type="checkbox" class="modal-toggle" v-model="showDeleteModal" />
    <div class="modal" role="dialog">
      <div class="modal-box">
        <h3 class="text-lg font-bold">Delete Account</h3>
        <p class="py-4">Are you sure you want to delete your account? This action cannot be undone.</p>
        <div class="modal-action">
          <button @click="showDeleteModal = false" class="btn">Cancel</button>
          <button @click="confirmDelete" class="btn btn-error" :disabled="deleting">
            {{ deleting ? 'Deleting...' : 'Delete' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>