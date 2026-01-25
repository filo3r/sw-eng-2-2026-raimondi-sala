<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Mail, Lock, UserCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { getCurrentUser, updateCurrentUser, deleteCurrentUser } from '@/services/user'
import type { UserUpdateRequest, UserResponse } from '@/types/user'
import { parseApiError } from '@/utils/error'
import { logError } from '@/utils/logger'
import {
  USER_NAME_MAX_LENGTH,
  USER_SURNAME_MAX_LENGTH,
  USERNAME_MAX_LENGTH,
  EMAIL_MAX_LENGTH,
  PASSWORD_MIN_LENGTH
} from '@/constants/validation'
import { SPINNER_DELAY_MS } from '@/constants/ui'

const router = useRouter()
const authStore = useAuthStore()
const { show } = useToast()

const name = ref('')
const surname = ref('')
const username = ref('')
const email = ref('')
const password = ref('')

const originalData = ref<UserResponse | null>(null)

const isEditing = ref(false)
const loading = ref(false)
const showDeleteModal = ref(false)

const initialLoading = ref(true)
const showSpinner = ref(false)
let spinnerTimeout: number | null = null

onMounted(async () => {
  await fetchUserData()
})

async function fetchUserData() {
  spinnerTimeout = window.setTimeout(() => {
    showSpinner.value = true
  }, SPINNER_DELAY_MS)

  try {
    const userData = await getCurrentUser()
    name.value = userData.name
    surname.value = userData.surname
    username.value = userData.username
    email.value = userData.email
    originalData.value = userData
  } catch (error: any) {
    logError(error, 'Profile.fetchUserData')
    show(parseApiError(error), 'error')
  } finally {
    if (spinnerTimeout) clearTimeout(spinnerTimeout)
    showSpinner.value = false
    initialLoading.value = false
  }
}

function toggleEdit() {
  if (isEditing.value && originalData.value) {
    name.value = originalData.value.name
    surname.value = originalData.value.surname
    username.value = originalData.value.username
    email.value = originalData.value.email
    password.value = ''
  }
  isEditing.value = !isEditing.value
}

async function handleSave() {
  loading.value = true

  try {
    const updateData: UserUpdateRequest = {}

    if (originalData.value) {
      if (name.value !== originalData.value.name) updateData.name = name.value
      if (surname.value !== originalData.value.surname) updateData.surname = surname.value
      if (username.value !== originalData.value.username) updateData.username = username.value
      if (email.value !== originalData.value.email) updateData.email = email.value
      if (password.value) updateData.password = password.value
    }

    await updateCurrentUser(updateData)
    await fetchUserData()
    password.value = ''
    isEditing.value = false
    show('Profile updated successfully!', 'success')
  } catch (error: any) {
    logError(error, 'Profile.handleSave')
    show(parseApiError(error), 'error')
  } finally {
    loading.value = false
  }
}

async function handleLogout() {
  authStore.clearAuth()
  show('Logged out successfully', 'success')
  await router.push('/login')
}

function openDeleteModal() {
  showDeleteModal.value = true
}

async function confirmDelete() {
  showDeleteModal.value = false

  try {
    await deleteCurrentUser()
    authStore.clearAuth()
    show('Account deleted successfully', 'success')
    await router.push('/login')
  } catch (error: any) {
    logError(error, 'Profile.confirmDelete')
    show(parseApiError(error), 'error')
  }
}
</script>

<template>
  <div v-if="showSpinner" class="flex h-screen items-center justify-center">
    <span class="loading loading-spinner loading-lg"></span>
  </div>

  <div v-else-if="!initialLoading" class="flex h-screen items-center justify-center overflow-hidden">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
      <div class="card-body items-center">
        <h2 class="card-title text-2xl">Profile</h2>

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

        <form v-else @submit.prevent="handleSave" class="space-y-4 w-full self-stretch">
          <label class="input input-bordered flex items-center gap-2 w-full">
            <User :size="16" />
            <input
                type="text"
                class="grow"
                placeholder="Name"
                v-model.trim="name"
                required
                :maxlength="USER_NAME_MAX_LENGTH"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <User :size="16" />
            <input
                type="text"
                class="grow"
                placeholder="Surname"
                v-model.trim="surname"
                required
                :maxlength="USER_SURNAME_MAX_LENGTH"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <UserCircle :size="16" />
            <input
                type="text"
                class="grow"
                placeholder="Username"
                v-model.trim="username"
                required
                :maxlength="USERNAME_MAX_LENGTH"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <Mail :size="16" />
            <input
                type="email"
                class="grow"
                placeholder="Email"
                v-model.trim="email"
                required
                :maxlength="EMAIL_MAX_LENGTH"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <Lock :size="16" />
            <input
                type="password"
                class="grow"
                placeholder="New Password (optional)"
                v-model.trim="password"
                :minlength="PASSWORD_MIN_LENGTH"
            />
          </label>

          <div class="flex gap-2">
            <button type="button" @click="toggleEdit" class="btn btn-ghost flex-1">
              Cancel
            </button>
            <button type="submit" class="btn btn-neutral flex-1" :disabled="loading">
              {{ loading ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </form>

        <div class="divider"></div>

        <button @click="openDeleteModal" class="btn btn-error btn-outline w-full">
          Delete Account
        </button>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <input type="checkbox" class="modal-toggle" v-model="showDeleteModal" />
    <div class="modal" role="dialog">
      <div class="modal-box">
        <h3 class="text-lg font-bold">Delete Account</h3>
        <p class="py-4">Are you sure you want to delete your account? This action cannot be undone.</p>
        <div class="modal-action">
          <button @click="showDeleteModal = false" class="btn">Cancel</button>
          <button @click="confirmDelete" class="btn btn-error">Delete</button>
        </div>
      </div>
    </div>
  </div>
</template>