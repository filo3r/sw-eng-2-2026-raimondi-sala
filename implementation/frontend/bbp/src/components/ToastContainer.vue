<script setup lang="ts">
/**
 * Toast notification container component.
 * Displays toast messages in top-right corner with DaisyUI styling and slide-in animations.
 * Manages toast lifecycle with auto-dismiss behavior from useToast composable.
 */
import { useToast } from '@/composables/useToast'

// Get singleton toast state from composable
const { toasts } = useToast()

/**
 * Maps toast type to corresponding DaisyUI alert class.
 * @param type - Toast type (success, error, or info)
 * @returns DaisyUI alert class name for styling
 */
const getAlertClass = (type: string) => {
  const classes: Record<string, string> = {
    success: 'alert-success', // Green background for success messages
    error: 'alert-error', // Red background for error messages
    info: 'alert-info' // Blue background for info messages
  }
  return classes[type] || '' // Fallback to empty string for unknown types
}
</script>

<template>
  <!-- Toast container positioned in top-right corner -->
  <div class="toast toast-top toast-end">
    <!-- Animate toast enter/leave with slide from right -->
    <TransitionGroup name="toast">
      <div v-for="toast in toasts" :key="toast.id"
           :class="['alert', getAlertClass(toast.type)]">
        <span>{{ toast.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
/* Toast slide-in/out animation from right side */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
/* Initial state: hidden and shifted right */
.toast-enter-from {
  opacity: 0;
  transform: translateX(30px);
}
/* Final leave state: hidden and shifted right */
.toast-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>