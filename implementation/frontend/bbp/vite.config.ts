import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * Vite configuration for Vue 3 application.
 */
export default defineConfig({
  // Enable Vue 3 SFC (Single File Component) support
  plugins: [vue()],
  resolve: {
    alias: {
      // Allow '@/...' imports to reference src directory
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173, // Development server port
    open: true // Auto-open browser on dev server start
  },
  build: {
    target: 'esnext', // Modern JS syntax (no legacy browser support)
    minify: 'esbuild', // Fast minification with esbuild
    sourcemap: false, // Disable source maps in production
    rollupOptions: {
      output: {
        // Code splitting: separate vendor libraries for better caching
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'mapbox': ['mapbox-gl'],
          'ui-vendor': ['lucide-vue-next']
        }
      }
    }
  }
})