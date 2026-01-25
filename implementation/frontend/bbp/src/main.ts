/**
 * Application entry point.
 * Initializes Vue app with Pinia store and Vue Router.
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// Global styles
import 'mapbox-gl/dist/mapbox-gl.css'
import './styles/main.css'
import './styles/mapbox.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')