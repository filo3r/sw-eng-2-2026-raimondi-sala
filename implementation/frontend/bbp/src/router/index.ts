import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            redirect: '/example1'
        },
        {
            path: '/example1',
            name: 'Example1',
            component: () => import('@/views/Example1View.vue')
        },
        {
            path: '/example2',
            name: 'Example2',
            component: () => import('@/views/Example2View.vue')
        },
        {
            path: '/example3',
            name: 'Example3',
            component: () => import('@/views/Example3View.vue')
        }
    ]
})

export default router