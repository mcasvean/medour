import { createRouter, createWebHistory } from 'vue-router'
import type { Router } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    guestOnly?: boolean
  }
}

export const routes = [
  {
    path: '/',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/register',
    component: () => import('../views/RegisterView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/admin/users',
    component: () => import('../views/AdminUsersView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/account',
    component: () => import('../views/AccountView.vue'),
    meta: { requiresAuth: true }
  }
]

export function setupGuard(r: Router) {
  r.beforeEach((to) => {
    const auth = useAuthStore()
    if (to.meta.requiresAuth && !auth.isAuthenticated) return '/login'
    if (to.meta.requiresAdmin && auth.user?.role !== 'ADMIN') return '/'
    if (to.meta.guestOnly && auth.isAuthenticated) return '/'
  })
}

export const router = createRouter({
  history: createWebHistory(),
  routes
})

setupGuard(router)

export default router

