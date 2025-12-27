import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
    history: createWebHistory(),
    linkActiveClass: 'bg-neutral text-neutral-content',
    routes: [
        {
            path: '/',
            name: 'BikePathFinder',
            component: () => import('@/views/BikePathFinderView.vue')
        },
        {
            path: '/trips',
            name: 'Trips',
            component: () => import('@/views/TripsView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/trips/create/manual',
            name: 'TripCreateManual',
            component: () => import('@/views/TripCreateManualView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/trips/:id',
            name: 'TripDetail',
            component: () => import('@/views/TripDetailView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths',
            name: 'BikePaths',
            component: () => import('@/views/BikePathsView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths/create/manual',
            name: 'BikePathCreateManual',
            component: () => import('@/views/BikePathCreateManualView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/bike-paths/:id',
            name: 'BikePathDetail',
            component: () => import('@/views/BikePathDetailView.vue'),
        },
        {
            path: '/profile',
            name: 'Profile',
            component: () => import('@/views/ProfileView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/LoginView.vue')
        },
        {
            path: '/register',
            name: 'Register',
            component: () => import('@/views/RegisterView.vue')
        }
    ]
})

// Route guard to protect authenticated routes
router.beforeEach((to, _from, next) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next({ name: 'Login' })
    } else {
        next()
    }
})

export default router