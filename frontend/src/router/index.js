import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/auth/Login.vue'
import Signup from '../views/auth/Signup.vue'
import Dashboard from '../views/dashboard/Dashboard.vue'
import Permission from '../views/admin/Permission.vue'
import EquipmentLife from '../life/EquipmentLife.vue'
import EquipmentMonitoring from '../views/EquipmentMonitoring/EquipmentMonitoring.vue'
import EquipmentDetail from '../views/EquipmentDetail/EquipmentDetail.vue'

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

export default router
