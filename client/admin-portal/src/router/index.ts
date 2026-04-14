import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    //change for login change 
    {
      path: '/',
      redirect: '/login',
    },
    // {
    //   path: '/dashboard',
    //   name: 'dashboard',
    //   component: () => import('@/views/DashboardView.vue'),
    // },
    // {
    //   path: '/patients',
    //   name: 'patients',
    //   component: () => import('@/views/PatientsView.vue'),
    // },
    // {
    //   path: '/patients/:patientId',
    //   name: 'patient-detail',
    //   component: () => import('@/views/PatientDetailView.vue'),
    // },
    // {
    //   path: '/doctors',
    //   name: 'doctors',
    //   component: () => import('@/views/DoctorsView.vue'),
    // },
    // {
    //   path: '/appointments',
    //   name: 'appointments',
    //   component: () => import('@/views/AppointmentsView.vue'),
    // },
    // {
    //   path: '/payments',
    //   name: 'payments',
    //   component: () => import('@/views/PaymentsView.vue'),
    // },
    // {
    //   path: '/notifications',
    //   name: 'notifications',
    //   component: () => import('@/views/NotificationsView.vue'),
    // },
    //change for login change
    {
      // catch-all — redirect anything unknown to dashboard
      path: '/:pathMatch(.*)*',
      redirect: '/login',
    },
  ],
})

// Navigation guard — runs before every route change
router.beforeEach((to) => {
  const auth = useAuthStore()

  // Public routes (login) are always accessible
  if (to.meta.public) return true

  // No token → send to login
  if (!auth.token) return { name: 'login' }

  // Has token but role is not ADMIN → send to login with error flag
  if (auth.role !== 'ADMIN') {
    auth.logout()
    return { name: 'login', query: { error: 'unauthorized' } }
  }

  return true
})

export default router