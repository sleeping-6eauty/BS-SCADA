import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/auth/Login.vue'
import Signup from '../views/auth/Signup.vue'
import Dashboard from '../views/dashboard/Dashboard.vue'
import Permission from '../views/admin/Permission.vue'
import EquipmentLife from '../views/life/EquipmentLife.vue'
import EquipmentMonitoring from '../views/EquipmentMonitoring/EquipmentMonitoring.vue'
import EquipmentDetail from '../views/EquipmentDetail/EquipmentDetail.vue'
import EquipmentAlarm from '../views/EquipmentAlarm/EquipmentAlarm.vue'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'login',
    component: Login,
  },
  {
    path: '/signup',
    name: 'signup',
    component: Signup,
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: Dashboard,
  },
  {
    path: '/equipment-monitor',
    name: 'equipment-monitor',
    component: EquipmentMonitoring,
  },
  {
    path: '/equipment-detail',
    name: 'equipment-detail',
    component: EquipmentDetail,
  },
  {
    path: '/equipment-alarm',
    name: 'equipment-alarm',
    component: EquipmentAlarm,
  },
  {
    path: '/admin/permission',
    name: 'permission',
    component: Permission,
  },
  {
    path: '/life',
    name: 'life',
    component: EquipmentLife,
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

const publicPages = ['/login', '/signup']

router.beforeEach((to, from, next) => {
  const authRequired = !publicPages.includes(to.path)
  const token = localStorage.getItem('token')
  if (authRequired && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
