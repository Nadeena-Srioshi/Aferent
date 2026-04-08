import { createRouter, createWebHistory } from 'vue-router'
// import { useAuthStore } from '@/stores/auth'  // re-enable when backend is ready

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
    },
    {
      path: '/',
      component: () => import('@/views/Layout.vue'),
      children: [
        { path: '', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
        { path: 'users', name: 'Users', component: () => import('@/views/users/UserList.vue') },
        { path: 'hospitals', name: 'Hospitals', component: () => import('@/views/hospitals/HospitalList.vue') },
        { path: 'hospitals/add', name: 'AddHospital', component: () => import('@/views/hospitals/AddHospital.vue') },
        { path: 'hospitals/:id/edit', name: 'EditHospital', component: () => import('@/views/hospitals/EditHospital.vue') },
        { path: 'hospitals/:id', name: 'HospitalDetails', component: () => import('@/views/hospitals/HospitalDetails.vue') },
        { path: 'doctors/verifications', name: 'DoctorVerifications', component: () => import('@/views/doctors/VerificationList.vue') },
        { path: 'doctors/verifications/:id', name: 'VerificationDetails', component: () => import('@/views/doctors/VerificationDetails.vue') },
        { path: 'specializations', name: 'Specializations', component: () => import('@/views/specializations/SpecializationList.vue') },
        { path: 'specializations/add', name: 'AddSpecialization', component: () => import('@/views/specializations/AddSpecialization.vue') },
        { path: 'specializations/:id/edit', name: 'EditSpecialization', component: () => import('@/views/specializations/EditSpecialization.vue') },
        { path: 'payments', name: 'Payments', component: () => import('@/views/payments/PaymentList.vue') },
        { path: 'payments/analytics', name: 'PaymentAnalytics', component: () => import('@/views/payments/PaymentAnalytics.vue') },
        { path: 'refunds', name: 'Refunds', component: () => import('@/views/payments/RefundManagement.vue') },
        { path: 'content/faqs', name: 'FAQs', component: () => import('@/views/content/FAQManagement.vue') },
        { path: 'content/terms', name: 'Terms', component: () => import('@/views/content/TermsConditions.vue') },
        { path: 'content/privacy', name: 'Privacy', component: () => import('@/views/content/PrivacyPolicy.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') },
  ],
})

// Auth guard — uncomment when backend is ready
// router.beforeEach((to, _from, next) => {
//   const authStore = useAuthStore()
//   const requiresAuth = to.matched.some((r) => r.meta.requiresAuth !== false)
//   if (requiresAuth && !authStore.isAuthenticated) next('/login')
//   else if (to.path === '/login' && authStore.isAuthenticated) next('/')
//   else next()
// })

export default router