import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'
import AdminLayout from '../layout/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'routes',
        component: () => import('../views/Routes.vue'),
        meta: { title: '线路管理' }
      },
      {
        path: 'stops',
        component: () => import('../views/Stops.vue'),
        meta: { title: '站点管理' }
      },
      {
        path: 'vehicles',
        component: () => import('../views/Vehicles.vue'),
        meta: { title: '车辆管理' }
      },
      {
        path: 'shifts',
        component: () => import('../views/Shifts.vue'),
        meta: { title: '班次管理' }
      },
      {
        path: 'reservations',
        component: () => import('../views/Reservations.vue'),
        meta: { title: '预约记录' }
      },
      {
        path: 'statistics',
        component: () => import('../views/Statistics.vue'),
        meta: { title: '预约统计' }
      },
      {
        path: 'suggestions',
        component: () => import('../views/Suggestions.vue'),
        meta: { title: '调度建议' }
      },
      {
        path: 'monitor',
        component: () => import('../views/Monitor.vue'),
        meta: { title: '车辆监控' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = getToken()
  if (to.path !== '/login' && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
  document.title = to.meta?.title ? `${to.meta.title} · 清途校车调度` : '清途校车调度 · 管理端'
  return true
})

export default router
