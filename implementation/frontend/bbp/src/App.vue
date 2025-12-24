<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { Search, MapPin, Bike, User, PanelRightClose, PanelRightOpen } from 'lucide-vue-next'
import ToastContainer from '@/components/ToastContainer.vue'

const isDrawerOpen = ref(false)
const route = useRoute()

const isTripsActive = computed(() => route.path.startsWith('/trips'))
const isBikePathsActive = computed(() => route.path.startsWith('/bike-paths'))
const isProfileActive = computed(() =>
    ['/profile', '/login', '/register'].includes(route.path)
)

watch(() => route.path, () => {
  isDrawerOpen.value = false
})
</script>

<template>
  <div class="drawer lg:drawer-open">
    <input id="my-drawer-4" type="checkbox" class="drawer-toggle" v-model="isDrawerOpen" />
    <div class="drawer-content flex flex-col h-screen">
      <nav class="navbar w-full bg-base-300 shrink-0">
        <label for="my-drawer-4" class="btn btn-square btn-ghost">
          <PanelRightOpen v-if="!isDrawerOpen" :size="16" />
          <PanelRightClose v-else :size="16" />
        </label>
        <div class="px-4">Best Bike Paths</div>
      </nav>
      <div class="flex-1">
        <RouterView />
      </div>
    </div>

    <div class="drawer-side is-drawer-close:overflow-visible">
      <label for="my-drawer-4" aria-label="close sidebar" class="drawer-overlay"></label>
      <div class="flex min-h-full flex-col items-start bg-base-200 is-drawer-close:w-14 is-drawer-open:w-64">
        <ul class="menu w-full grow">
          <li>
            <RouterLink to="/" class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="Finder">
              <Search :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Finder</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink to="/trips" :class="isTripsActive ? 'bg-neutral text-neutral-content' : ''" class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="Trips">
              <MapPin :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Trips</span>
            </RouterLink>
          </li>
          <li>
            <RouterLink to="/bike-paths" :class="isBikePathsActive ? 'bg-neutral text-neutral-content' : ''" class="is-drawer-close:tooltip is-drawer-close:tooltip-right" data-tip="Bike Paths">
              <Bike :size="16" class="my-1.5" />
              <span class="is-drawer-close:hidden whitespace-nowrap">Bike Paths</span>
            </RouterLink>
          </li>
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

  <ToastContainer />
</template>