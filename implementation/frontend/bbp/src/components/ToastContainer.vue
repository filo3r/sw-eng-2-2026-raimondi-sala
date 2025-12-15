<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <div class="toast toast-top toast-end">
    <div v-for="toast in toasts" :key="toast.id"
         :class="[
           'alert',
           toast.type === 'success' ? 'alert-success' : '',
           toast.type === 'error' ? 'alert-error' : '',
           toast.type === 'info' ? 'alert-info' : ''
         ]">
      <span>{{ toast.message }}</span>
    </div>
  </div>
</template>