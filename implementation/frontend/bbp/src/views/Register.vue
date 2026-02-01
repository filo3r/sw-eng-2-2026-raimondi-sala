<script setup lang="ts">
/**
 * Register page.
 * - Collects user details (name, surname, username, email, password).
 * - Performs frontend validation before calling the register API.
 * - On success, stores JWT + userId in the auth store and redirects to home.
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Mail, Lock, UserCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useFieldError } from '@/composables/useFieldError'
import { register } from '@/services/auth'
import {
  validateName,
  validateSurname,
  validateUsername,
  validateEmail,
  validatePassword,
  validateAndShow
} from '@/utils/validation'
import { catchApiError } from '@/utils/error'
import {
  USER_NAME_MAX_LENGTH,
  USER_SURNAME_MAX_LENGTH,
  USERNAME_MAX_LENGTH,
  EMAIL_MAX_LENGTH,
  PASSWORD_MIN_LENGTH
} from '@/constants/validation'
import type { UserRegisterRequest } from '@/types/user'

/** Router instance used for SPA navigation after successful registration. */
const router = useRouter()

/** Auth store used to persist token/userId and mark user as logged-in. */
const authStore = useAuthStore()

/** Toast helper for user feedback. */
const { show } = useToast()

/** Field-level error helpers used by validation and API error mapping. */
const { hasError, setError } = useFieldError()

/** Form fields bound to inputs. */
const name = ref('')
const surname = ref('')
const username = ref('')
const email = ref('')
const password = ref('')

/** UI loading state to disable submit button and show progress text. */
const loading = ref(false)

/**
 * Submits the registration form.
 * - Validates fields client-side first.
 * - Calls the register API.
 * - Saves auth token in store and redirects on success.
 * - Maps API errors to field errors on failure.
 */
async function handleRegister(): Promise<void> {
  // Frontend validation
  if (!validateAndShow(validateName(name.value), 'name', setError, show)) return
  if (!validateAndShow(validateSurname(surname.value), 'surname', setError, show)) return
  if (!validateAndShow(validateUsername(username.value), 'username', setError, show)) return
  if (!validateAndShow(validateEmail(email.value), 'email', setError, show)) return
  if (!validateAndShow(validatePassword(password.value), 'password', setError, show)) return

  loading.value = true

  try {
    const request: UserRegisterRequest = {
      name: name.value,
      surname: surname.value,
      username: username.value,
      email: email.value,
      password: password.value
    }

    const response = await register(request)

    // Persist auth state (token + userId) for subsequent authenticated API calls.
    authStore.setAuth(response.token, response.userId)

    show('Registration successful!', 'success')
    await router.push('/') /* SPA navigation via router.push(...) [web:117] */
  } catch (error: unknown) {
    // Centralized API error handling (e.g., map 400/409 to field errors).
    catchApiError(error, 'Register.handleRegister', setError)
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

        <!-- @submit.prevent avoids native page reload and runs SPA register flow -->
        <form @submit.prevent="handleRegister" class="space-y-4 w-full self-stretch">
          <label
              class="input input-bordered flex items-center gap-2 w-full"
              :class="{ 'input-error': hasError('name') }"
          >
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

          <label
              class="input input-bordered flex items-center gap-2 w-full"
              :class="{ 'input-error': hasError('surname') }"
          >
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

          <label
              class="input input-bordered flex items-center gap-2 w-full"
              :class="{ 'input-error': hasError('username') }"
          >
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
                autocomplete="new-password"
            />
          </label>

          <button type="submit" class="btn btn-neutral w-full" :disabled="loading" :aria-busy="loading">
            {{ loading ? 'Registering...' : 'Register' }}
          </button>
        </form>

        <div class="divider">OR</div>

        <!-- RouterLink is globally available in templates (Vue Router). [web:27] -->
        <RouterLink to="/login" class="btn btn-ghost w-full">
          Already have an account? Login
        </RouterLink>
      </div>
    </div>
  </div>
</template>