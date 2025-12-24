<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Mail, Lock, UserCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import api from '@/api/axios'

const router = useRouter()
const authStore = useAuthStore()
const { show } = useToast()

const name = ref('')
const surname = ref('')
const username = ref('')
const email = ref('')
const password = ref('')

const originalData = ref({
  name: '',
  surname: '',
  username: '',
  email: ''
})

const isEditing = ref(false)
const loading = ref(false)
const showDeleteModal = ref(false)

onMounted(async () => {
  document.body.style.overflow = 'hidden'
  await fetchUserData()
})

onUnmounted(() => {
  document.body.style.overflow = ''
})

async function fetchUserData() {
  try {
    const response = await api.get('/api/users/me')
    name.value = response.data.name
    surname.value = response.data.surname
    username.value = response.data.username
    email.value = response.data.email

    originalData.value = {
      name: response.data.name,
      surname: response.data.surname,
      username: response.data.username,
      email: response.data.email
    }
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to load profile'
    show(message, 'error')
  }
}

function toggleEdit() {
  if (isEditing.value) {
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
    const updateData: any = {}

    if (name.value !== originalData.value.name) updateData.name = name.value
    if (surname.value !== originalData.value.surname) updateData.surname = surname.value
    if (username.value !== originalData.value.username) updateData.username = username.value
    if (email.value !== originalData.value.email) updateData.email = email.value
    if (password.value) updateData.password = password.value

    await api.patch('/api/users/me', updateData)

    await fetchUserData()
    password.value = ''
    isEditing.value = false
    show('Profile updated successfully!', 'success')
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to update profile'
    show(message, 'error')
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
    await api.delete('/api/users/me')
    authStore.clearAuth()
    show('Account deleted successfully', 'success')
    await router.push('/login')
  } catch (error: any) {
    const message = error.response?.data?.message || 'Failed to delete account'
    show(message, 'error')
  }
}
</script>

<template>
  <div class="flex h-screen items-center justify-center overflow-hidden">
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
                maxlength="50"
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
                maxlength="50"
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
                maxlength="50"
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
                maxlength="150"
            />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <Lock :size="16" />
            <input
                type="password"
                class="grow"
                placeholder="New Password (optional)"
                v-model.trim="password"
                minlength="8"
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