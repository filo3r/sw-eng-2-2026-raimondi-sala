<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div id="app">
    <nav class="p-4 bg-gray-100">
      <RouterLink to="/example1" class="mr-4 text-blue-600 hover:underline">Example 1</RouterLink>
      <RouterLink to="/example2" class="mr-4 text-blue-600 hover:underline">Example 2</RouterLink>
      <RouterLink to="/example3" class="text-blue-600 hover:underline">Example 3</RouterLink>
    </nav>

    <main class="p-4">
      <RouterView />
    </main>
  </div>
</template>