import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],

  resolve: {
    alias: {
      // Path alias: import X from '@/components/X'
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  server: {
    port: 5173,  // Development server port
    open: true
  },

  build: {
    target: 'esnext',
    minify: 'esbuild',
    sourcemap: false,
    rollupOptions: {
      output: {
        // Split vendors for better caching
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'mapbox': ['mapbox-gl']
        }
      }
    }
  }
})