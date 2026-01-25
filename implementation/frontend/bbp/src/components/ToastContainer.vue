<script setup lang="ts">
/**
 * Toast notification container.
 * Displays toast messages in top-right corner.
 */
import { useToast } from '@/composables/useToast'

const { toasts } = useToast()

// Map toast types to DaisyUI alert classes
const getAlertClass = (type: string) => {
  const classes: Record<string, string> = {
    success: 'alert-success',
    error: 'alert-error',
    info: 'alert-info'
  }
  return classes[type] || ''
}
</script>

<template>
  <div class="toast toast-top toast-end">
    <TransitionGroup name="toast">
      <div v-for="toast in toasts" :key="toast.id"
           :class="['alert', getAlertClass(toast.type)]">
        <span>{{ toast.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>