/**
 * Vue Router configuration with authentication guards.
 * Implements lazy loading for all route components.
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
    history: createWebHistory(),
    linkActiveClass: 'bg-neutral text-neutral-content',
    routes: [
        {
            path: '/',
            name: 'BikePathFinder',
            component: () => import('@/views/BikePathFinder.vue')
        },
        {
            path: '/trips',
            name: 'Trips',
            component: () => import('@/views/Trips.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/trips/create/manual',
            name: 'TripCreateManual',
            component: () => import('@/views/TripCreateManual.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/trips/:id',
            name: 'TripDetail',
            component: () => import('@/views/TripDetail.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths',
            name: 'BikePaths',
            component: () => import('@/views/BikePaths.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths/create/manual',
            name: 'BikePathCreateManual',
            component: () => import('@/views/BikePathCreateManual.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths/:id',
            name: 'BikePathDetail',
            component: () => import('@/views/BikePathDetail.vue'),
        },
        {
            path: '/profile',
            name: 'Profile',
            component: () => import('@/views/Profile.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/Login.vue')
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/Register.vue')
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('@/views/NotFound.vue')
        }
    ]
})

/**
 * Global navigation guard: redirects to login if route requires authentication.
 */
router.beforeEach((to, _from, next) => {
    const authStore = useAuthStore()
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next({ name: 'Login' })
    } else {
        next()
    }
})

export default router