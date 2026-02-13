import { createRouter, createWebHistory } from 'vue-router'
import GoodsDetail from '@/views/GoodsDetail.vue'

const routes = [
    {
        path: '/',
        name: 'UserHome',
        component: () => import('@/views/UserHome.vue')
    },
    {
        path: '/order-confirm',
        name: 'OrderConfirm',
        component: () => import('@/views/OrderConfirm.vue')
    },
    {
        path: '/goods/:id',
        name: 'ProductDetail',
        component: GoodsDetail
    },
    {
        path: '/manage',
        component: () => import('@/views/ManageLayout.vue'),
        children: [
            {
                path: '',
                redirect: '/manage/home'
            },
            {
                path: 'home',
                name: 'ManageHome',
                component: () => import('@/views/manage/Dashboard.vue')
            },
            {
                path: 'products',
                name: 'ManageProducts',
                component: () => import('@/views/manage/ProductManage.vue')
            },
            {
                path: 'categories',
                name: 'ManageCategories',
                component: () => import('@/views/manage/CategoryManage.vue')
            },
            {
                path: 'orders',
                name: 'ManageOrders',
                component: () => import('@/views/manage/OrderManage.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
