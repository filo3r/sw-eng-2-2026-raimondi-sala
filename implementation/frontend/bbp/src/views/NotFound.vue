<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Home, ArrowLeft } from 'lucide-vue-next'

const router = useRouter()

function goHome() {
  router.push('/')
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="flex h-screen items-center justify-center">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
      <div class="card-body items-center text-center">
        <h1 class="text-6xl font-bold text-error mb-4">404</h1>
        <h2 class="text-2xl font-bold mb-2">Page Not Found</h2>
        <p class="text-gray-600 mb-6">
          The page you're looking for doesn't exist or has been moved.
        </p>
        <div class="flex gap-2">
          <button @click="goBack" class="btn btn-ghost">
            <ArrowLeft :size="20" />
            Go Back
          </button>
          <button @click="goHome" class="btn btn-neutral">
            <Home :size="20" />
            Go Home
          </button>
        </div>
      </div>
    </div>
  </div>
</template>