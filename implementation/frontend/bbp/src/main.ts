/**
 * App entry point.
 * Creates the Vue app, registers Pinia + Vue Router, then mounts to `#app`.
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// Global styles (Mapbox GL CSS + app styles)
import 'mapbox-gl/dist/mapbox-gl.css'
import './styles/main.css'
import './styles/mapbox.css'

const app = createApp(App)
// Register Pinia as the app-wide store plugin
app.use(createPinia())
// Register the router plugin before mounting
app.use(router)
// Mounts the app into the DOM container
app.mount('#app')