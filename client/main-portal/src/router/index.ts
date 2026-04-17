import { createRouter, createWebHistory } from 'vue-router'

import LandingView from '@/views/LandingView.vue'
import LoginView from '@/views/LoginView.vue'
import FindDoctorView from '@/views/FindDoctorView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ProfileView from '@/views/ProfileView.vue'
import AboutView from '@/views/Aboutview.vue'
import RecordsView from '@/views/Recordsview.vue'
import AppointmentsView from '@/views/AppointmentsView.vue'
import MedicalHistoryView from '@/views/MedicalHistoryView.vue'
import DoctorRegisterView from '@/views/DoctorRegisterView.vue'
import SymptomView from '@/views/SymptomView.vue'
import DoctorDashboardView from '@/views/DoctorDashboardView.vue'
import DoctorProfileView from '@/views/DoctorProfileView.vue'
import DoctorScheduleView from '@/views/DoctorScheduleView.vue'
import { useAuth } from '@/stores/useAuth'

function normalizeRole(role: unknown) {
  return typeof role === 'string' ? role.trim().toUpperCase() : ''
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: LandingView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/about', name: 'about', component: AboutView },
    { path: '/find-doctor', name: 'find-doctor', component: FindDoctorView },
    { path: '/appointments', name: 'appointments', component: AppointmentsView },
    { path: '/medical-history', name: 'medical-history', component: MedicalHistoryView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/profile', name: 'profile', component: ProfileView },
    { path: '/records', name: 'records', component: RecordsView },
    { path: '/doctor-register', name: 'doctor-register', component: DoctorRegisterView },
    { path: '/symptoms', name: 'symptoms-detector', component: SymptomView },
    {
      path: '/doctor/profile',
      name: 'doctor-profile',
      component: DoctorProfileView,
      meta: { requiresAuth: true, roles: ['DOCTOR'] },
    },
    {
      path: '/doctor/dashboard',
      name: 'doctor-dashboard',
      component: DoctorDashboardView,
      meta: { requiresAuth: true, roles: ['DOCTOR'] },
    },
    {
      path: '/doctor/schedule',
      name: 'doctor-schedule',
      component: DoctorScheduleView,
      meta: { requiresAuth: true, roles: ['DOCTOR'] },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  const requiresAuth = Boolean(to.meta?.requiresAuth)
  const routeRoles = Array.isArray(to.meta?.roles)
    ? (to.meta.roles as string[]).map((role) => normalizeRole(role))
    : []

  if (auth.token && !auth.user) {
    await auth.fetchMe()
  }

  if (requiresAuth && !auth.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (routeRoles.length > 0) {
    const userRole = normalizeRole(auth.user?.role)
    if (!routeRoles.includes(userRole)) {
      return { name: 'landing' }
    }
  }

  return true
})

export default router
