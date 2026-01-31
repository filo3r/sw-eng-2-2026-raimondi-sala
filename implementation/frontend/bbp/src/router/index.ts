/**
 * Vue Router configuration with authentication guards and lazy-loaded components.
 * Defines application routes, navigation structure, and access control rules.
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * Main application router instance.
 * Uses HTML5 history mode and lazy loading for optimal performance.
 */
const router = createRouter({
    history: createWebHistory(),
    linkActiveClass: 'bg-neutral text-neutral-content', // DaisyUI active link styling
    routes: [
        {
            path: '/',
            name: 'BikePathFinder',
            component: () => import('@/views/BikePathFinder.vue') // Public: bike path discovery
        },
        {
            path: '/trips',
            name: 'Trips',
            component: () => import('@/views/Trips.vue'), // Protected: user's trip list
            meta: { requiresAuth: true }
        },
        {
            path: '/trips/create/manual',
            name: 'TripCreateManual',
            component: () => import('@/views/TripCreateManual.vue'), // Protected: manual trip recording
            meta: { requiresAuth: true }
        },
        {
            path: '/trips/:id',
            name: 'TripDetail',
            component: () => import('@/views/TripDetail.vue'), // Protected: single trip details
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths',
            name: 'BikePaths',
            component: () => import('@/views/BikePaths.vue'), // Protected: user's bike path list
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths/create/manual',
            name: 'BikePathCreateManual',
            component: () => import('@/views/BikePathCreateManual.vue'), // Protected: manual bike path creation
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths/:id',
            name: 'BikePathDetail',
            component: () => import('@/views/BikePathDetail.vue'), // Public: single bike path details
        },
        {
            path: '/profile',
            name: 'Profile',
            component: () => import('@/views/Profile.vue'), // Protected: user profile management
            meta: { requiresAuth: true }
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/Login.vue') // Public: user authentication
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/Register.vue') // Public: user registration
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('@/views/NotFound.vue') // Catch-all: 404 page
        }
    ]
})

/**
 * Global navigation guard that enforces authentication requirements.
 * Redirects unauthenticated users to login page when accessing protected routes.
 * Routes with meta.requiresAuth=true require valid JWT token in auth store.
 */
router.beforeEach((to, _from, next) => {
    const authStore = useAuthStore()
    // Check if route requires authentication and user is not authenticated
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next({ name: 'Login' }) // Redirect to login page
    } else {
        next() // Allow navigation
    }
})

export default router