import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/components/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // Public
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },

    // Authenticated — all wrapped in the sidebar layout
    {
      path: '/',
      component: AppLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
        },
        {
          path: 'patients',
          name: 'patients',
          component: () => import('@/views/PatientsView.vue'),
        },
        {
          path: 'patients/:patientId',
          name: 'patient-detail',
          component: () => import('@/views/PatientDetailView.vue'),
        },
        {
          path: 'doctors',
          name: 'doctors',
          component: () => import('@/views/DoctorsView.vue'),
        },
        {
          path: 'appointments',
          name: 'appointments',
          component: () => import('@/views/AppointmentsView.vue'),
        },
        {
          path: 'payments',
          name: 'payments',
          component: () => import('@/views/PaymentsView.vue'),
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/NotificationsView.vue'),
        },
        {
          path: 'hospitals',
          name: 'hospitals',
          component: () => import('@/views/HospitalsView.vue'),
        },
        {
          path: 'specializations',
          name: 'specializations',
          component: () => import('@/views/SpecializationsView.vue'),
        },
      ],
    },

    // Catch-all
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard',
    },
  ],
})

// Navigation guard
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) return true

  if (!auth.token) return { name: 'login' }

  if (auth.role !== 'ADMIN') {
    auth.logout()
    return { name: 'login', query: { error: 'unauthorized' } }
  }

  return true
})

export default router