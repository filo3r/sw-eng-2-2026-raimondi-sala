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
const loading = ref(false)

onMounted(() => {
  document.body.style.overflow = 'hidden'
})

onUnmounted(() => {
  document.body.style.overflow = ''
})

async function handleRegister() {
  loading.value = true

  try {
    const response = await api.post('/api/auth/register', {
      name: name.value,
      surname: surname.value,
      username: username.value,
      email: email.value,
      password: password.value
    })

    authStore.setAuth(response.data.token, response.data.userId)
    show('Registration successful!', 'success')
    await router.push('/')
  } catch (error: any) {
    const message = error.response?.data?.message || 'Registration failed'
    show(message, 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex h-screen items-center justify-center overflow-hidden">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
      <div class="card-body items-center">
        <h2 class="card-title text-2xl">Register</h2>

        <form @submit.prevent="handleRegister" class="space-y-4 w-full self-stretch">
          <label class="input input-bordered flex items-center gap-2 w-full">
            <User :size="16" />
            <input type="text" class="grow" placeholder="Name" v-model.trim="name" required maxlength="50" />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <User :size="16" />
            <input type="text" class="grow" placeholder="Surname" v-model.trim="surname" required maxlength="50" />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <UserCircle :size="16" />
            <input type="text" class="grow" placeholder="Username" v-model.trim="username" required maxlength="50" />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <Mail :size="16" />
            <input type="email" class="grow" placeholder="Email" v-model.trim="email" required maxlength="150" />
          </label>

          <label class="input input-bordered flex items-center gap-2 w-full">
            <Lock :size="16" />
            <input type="password" class="grow" placeholder="Password" v-model.trim="password" required minlength="8" />
          </label>

          <button type="submit" class="btn btn-neutral w-full" :disabled="loading">
            {{ loading ? 'Registering...' : 'Register' }}
          </button>
        </form>

        <div class="divider">OR</div>

        <RouterLink to="/login" class="btn btn-ghost w-full">
          Already have an account? Login
        </RouterLink>
      </div>
    </div>
  </div>
</template>