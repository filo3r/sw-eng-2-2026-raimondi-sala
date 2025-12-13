import { createRouter, createWebHistory } from 'vue-router'

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
            component: () => import('@/views/TripsView.vue')
        },
        {
            path: '/trips/create/manual',
            name: 'TripCreateManual',
            component: () => import('@/views/TripCreateManualView.vue')
        },
        {
            path: '/trips/:id',
            name: 'TripDetail',
            component: () => import('@/views/TripDetailView.vue')
        },
        {
            path: '/bike-paths',
            name: 'BikePaths',
            component: () => import('@/views/BikePathsView.vue')
        },
        {
            path: '/bike-paths/create/manual',
            name: 'BikePathCreateManual',
            component: () => import('@/views/BikePathCreateManualView.vue')
        },
        {
            path: '/bike-paths/:id',
            name: 'BikePathDetail',
            component: () => import('@/views/BikePathDetailView.vue')
        },
        {
            path: '/profile',
            name: 'Profile',
            component: () => import('@/views/ProfileView.vue')
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

// TODO: Consider adding route guards to protect authenticated routes

export default router