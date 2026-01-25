<script setup lang="ts">
/**
 * Root application component with responsive drawer navigation.
 * Drawer auto-closes on route change.
 */
import { ref, computed, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { Search, MapPin, Bike, User, PanelRightClose, PanelRightOpen } from 'lucide-vue-next'
import ToastContainer from '@/components/ToastContainer.vue'

const isDrawerOpen = ref(false)
const route = useRoute()

// Highlight active nav items based on current route
const isTripsActive = computed(() => route.path.startsWith('/trips'))
const isBikePathsActive = computed(() => route.path.startsWith('/bike-paths'))
const isProfileActive = computed(() =>
    ['/profile', '/login', '/register'].includes(route.path)
)

// Auto-close drawer on navigation
watch(() => route.path, () => {
  isDrawerOpen.value = false
})
</script>

<template>
  <div class="drawer lg:drawer-open">
    <input id="my-drawer-4" type="checkbox" class="drawer-toggle" v-model="isDrawerOpen" />

    <!-- Main Content Area -->
    <div class="drawer-content flex flex-col h-screen">
      <!-- Top Navigation Bar -->
      <nav class="navbar w-full bg-base-300 shrink-0">
        <label for="my-drawer-4" class="btn btn-square btn-ghost">
          <PanelRightOpen v-if="!isDrawerOpen" :size="16" />
          <PanelRightClose v-else :size="16" />
        </label>
        <div class="px-4 text-xl font-bold">Best Bike Paths</div>
      </nav>

      <!-- Page Content -->
      <div class="flex-1">
        <RouterView />
      </div>
    </div>

    <!-- Side Drawer Navigation -->
    <div class="drawer-side is-drawer-close:overflow-visible z-50">
      <label for="my-drawer-4" aria-label="close sidebar" class="drawer-overlay"></label>

      <!-- Drawer Menu -->
      <div class="flex min-h-full flex-col items-start bg-base-200 is-drawer-close:w-14 is-drawer-open:w-64">
        <ul class="menu w-full grow">
          <!-- Finder -->
          <li>
            <RouterLink to="/" class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="Finder">
              <Search :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Finder</span>
            </RouterLink>
          </li>
          <!-- Trips -->
          <li>
            <RouterLink to="/trips" :class="isTripsActive ? 'bg-neutral text-neutral-content' : ''" class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="Trips">
              <MapPin :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Trips</span>
            </RouterLink>
          </li>
          <!-- Bike Paths -->
          <li>
            <RouterLink to="/bike-paths" :class="isBikePathsActive ? 'bg-neutral text-neutral-content' : ''" class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="Bike Paths">
              <Bike :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Bike Paths</span>
            </RouterLink>
          </li>
          <!-- Profile -->
          <li>
            <RouterLink to="/profile" :class="isProfileActive ? 'bg-neutral text-neutral-content' : ''" class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="Profile">
              <User :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Profile</span>
            </RouterLink>
          </li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Global Toast Notifications -->
  <ToastContainer />
</template>