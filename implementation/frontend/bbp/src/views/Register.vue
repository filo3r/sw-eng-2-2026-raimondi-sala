<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Mail, Lock, UserCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useFieldError } from '@/composables/useFieldError'
import { register } from '@/services/auth'
import type { UserRegisterRequest } from '@/types/user'
import { catchApiError } from '@/utils/error'
import {
  USER_NAME_MAX_LENGTH,
  USER_SURNAME_MAX_LENGTH,
  USERNAME_MAX_LENGTH,
  EMAIL_MAX_LENGTH,
  PASSWORD_MIN_LENGTH
} from '@/constants/validation'

const router = useRouter()
const authStore = useAuthStore()
const { show } = useToast()
const { hasError, setError } = useFieldError()

const name = ref('')
const surname = ref('')
const username = ref('')
const email = ref('')
const password = ref('')
const loading = ref(false)

async function handleRegister() {
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
    authStore.setAuth(response.token, response.userId)
    show('Registration successful!', 'success')
    await router.push('/')
  } catch (error: any) {
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

        <form @submit.prevent="handleRegister" class="space-y-4 w-full self-stretch">
          <label class="input input-bordered flex items-center gap-2 w-full"
                 :class="{'input-error': hasError('name')}">
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

          <label class="input input-bordered flex items-center gap-2 w-full"
                 :class="{'input-error': hasError('surname')}">
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

          <label class="input input-bordered flex items-center gap-2 w-full"
                 :class="{'input-error': hasError('username')}">
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

          <label class="input input-bordered flex items-center gap-2 w-full"
                 :class="{'input-error': hasError('email')}">
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

          <label class="input input-bordered flex items-center gap-2 w-full"
                 :class="{'input-error': hasError('password')}">
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