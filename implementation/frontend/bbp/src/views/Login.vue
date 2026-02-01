<script setup lang="ts">
/**
 * Login page.
 * - Collects user credentials (email + password).
 * - Performs frontend validation before calling the auth API.
 * - On success, stores JWT + userId in the auth store and redirects to home.
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Mail, Lock } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useFieldError } from '@/composables/useFieldError'
import { login } from '@/services/auth'
import { validateEmail, validatePassword, validateAndShow } from '@/utils/validation'
import { catchApiError } from '@/utils/error'
import { EMAIL_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '@/constants/validation'
import type { UserLoginRequest } from '@/types/user'

/** Router instance used for SPA navigation after successful login. */
const router = useRouter()

/** Auth store used to persist token/userId and mark user as logged-in. */
const authStore = useAuthStore()

/** Toast helper for user feedback. */
const { show } = useToast()

/** Field-level error helpers used by validation and API error mapping. */
const { hasError, setError } = useFieldError()

/** Form fields bound to inputs. */
const email = ref('')
const password = ref('')

/** UI loading state to disable submit and show progress text. */
const loading = ref(false)

/**
 * Submits the login form.
 * - Validates fields client-side first.
 * - Calls the login API.
 * - Saves auth token in store and redirects on success.
 * - Maps API errors to field errors on failure.
 */
async function handleLogin(): Promise<void> {
  // Frontend validation
  if (!validateAndShow(validateEmail(email.value), 'email', setError, show)) return
  if (!validateAndShow(validatePassword(password.value), 'password', setError, show)) return

  loading.value = true

  try {
    const request: UserLoginRequest = {
      email: email.value,
      password: password.value
    }

    const response = await login(request)

    // Persist auth state (token + userId) for subsequent authenticated API calls.
    authStore.setAuth(response.token, response.userId)

    show('Login successful!', 'success')
    await router.push('/')
  } catch (error: unknown) {
    // Centralized API error handling (e.g., map 400/401 to field errors, show toast, etc.).
    catchApiError(error, 'Login.handleLogin', setError)
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

        <!-- @submit.prevent avoids native page reload and runs SPA login flow -->
        <form @submit.prevent="handleLogin" class="space-y-4 w-full self-stretch">
          <label
              class="input input-bordered flex items-center gap-2 w-full"
              :class="{ 'input-error': hasError('email') }"
          >
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

          <label
              class="input input-bordered flex items-center gap-2 w-full"
              :class="{ 'input-error': hasError('password') }"
          >
            <Lock :size="16" />
            <input
                v-model.trim="password"
                type="password"
                class="grow"
                placeholder="Password"
                required
                :minlength="PASSWORD_MIN_LENGTH"
                autocomplete="current-password"
            />
          </label>

          <button type="submit" class="btn btn-neutral w-full" :disabled="loading" :aria-busy="loading">
            {{ loading ? 'Logging in...' : 'Login' }}
          </button>
        </form>

        <div class="divider">OR</div>

        <!-- RouterLink is globally available in templates (Vue Router). [web:27] -->
        <RouterLink to="/register" class="btn btn-ghost w-full">
          Don't have an account? Register
        </RouterLink>
      </div>
    </div>
  </div>
</template>