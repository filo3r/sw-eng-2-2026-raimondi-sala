<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Mail, Lock } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { login } from '@/services/auth'
import type { UserLoginRequest } from '@/types/user'
import { catchApiError } from '@/utils/error'
import { EMAIL_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '@/constants/validation'

const router = useRouter()
const authStore = useAuthStore()
const { show } = useToast()

const email = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  loading.value = true

  try {
    const request: UserLoginRequest = {
      email: email.value,
      password: password.value
    }

    const response = await login(request)
    authStore.setAuth(response.token, response.userId)
    show('Login successful!', 'success')
    await router.push('/')
  } catch (error: any) {
    catchApiError(error, 'Login.handleLogin')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex h-screen items-center justify-center overflow-hidden">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
      <div class="card-body items-center">
        <h2 class="card-title text-2xl">Login</h2>

        <form @submit.prevent="handleLogin" class="space-y-4 w-full self-stretch">
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
                placeholder="Password"
                v-model.trim="password"
                required
                :minlength="PASSWORD_MIN_LENGTH"
            />
          </label>

          <button type="submit" class="btn btn-neutral w-full" :disabled="loading">
            {{ loading ? 'Logging in...' : 'Login' }}
          </button>
        </form>

        <div class="divider">OR</div>

        <RouterLink to="/register" class="btn btn-ghost w-full">
          Don't have an account? Register
        </RouterLink>
      </div>
    </div>
  </div>
</template>