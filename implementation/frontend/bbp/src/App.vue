<script setup lang="ts">
/**
 * Root application component with responsive DaisyUI drawer navigation.
 * Uses a checkbox-driven drawer (DaisyUI) controlled via v-model and auto-closes on route changes.
 */
import { ref, computed, watch } from 'vue'
import { RouterView, RouterLink, useRoute } from 'vue-router'
import { Search, MapPin, Bike, User, PanelRightClose, PanelRightOpen } from 'lucide-vue-next'
import ToastContainer from '@/components/ToastContainer.vue'

/** Controls the DaisyUI drawer checkbox state (open/closed). */
const isDrawerOpen = ref(false)

/** Reactive current route; watch specific properties (path) instead of the whole object. */
const route = useRoute()

/**
 * Active state helpers for nav items.
 * Uses `route.path` matching for highlight styling.
 */
const isTripsActive = computed(() => route.path.startsWith('/trips'))
const isBikePathsActive = computed(() => route.path.startsWith('/bike-paths'))
const isProfileActive = computed(() => ['/profile', '/login', '/register'].includes(route.path))

/**
 * Auto-close the drawer on navigation to keep UX clean on mobile.
 * Watching `route.path` is preferred over watching the whole route object. [page:1]
 */
watch(
    () => route.path,
    () => {
      isDrawerOpen.value = false
    }
)
</script>

<template>
  <!-- DaisyUI drawer: checkbox (.drawer-toggle) controls sidebar visibility. [page:2] -->
  <div class="drawer lg:drawer-open">
    <input id="my-drawer-4" type="checkbox" class="drawer-toggle" v-model="isDrawerOpen" />

    <!-- Main Content Area -->
    <div class="drawer-content flex flex-col h-screen">
      <!-- Top Navigation Bar -->
      <nav class="navbar w-full bg-base-200 shrink-0">
        <!-- Label toggles the hidden checkbox via "for" attribute. [page:2] -->
        <label for="my-drawer-4" class="btn btn-square btn-ghost" aria-label="Toggle navigation drawer">
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
      <label for="my-drawer-4" aria-label="Close sidebar" class="drawer-overlay"></label>

      <!-- Drawer Menu -->
      <div class="flex min-h-full flex-col items-start bg-base-200 is-drawer-close:w-14 is-drawer-open:w-64">
        <ul class="menu w-full grow">
          <li>
            <RouterLink
                to="/"
                class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
                data-tip="Finder"
            >
              <Search :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Finder</span>
            </RouterLink>
          </li>

          <li>
            <RouterLink
                to="/trips"
                :class="isTripsActive ? 'bg-neutral text-neutral-content' : ''"
                class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
                data-tip="Trips"
            >
              <MapPin :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Trips</span>
            </RouterLink>
          </li>

          <li>
            <RouterLink
                to="/bike-paths"
                :class="isBikePathsActive ? 'bg-neutral text-neutral-content' : ''"
                class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
                data-tip="Bike Paths"
            >
              <Bike :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Bike Paths</span>
            </RouterLink>
          </li>

          <li>
            <RouterLink
                to="/profile"
                :class="isProfileActive ? 'bg-neutral text-neutral-content' : ''"
                class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
                data-tip="Profile"
            >
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