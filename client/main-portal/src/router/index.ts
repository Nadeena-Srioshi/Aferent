import { createRouter, createWebHistory } from 'vue-router'

import LandingView from '@/views/LandingView.vue'
import LoginView from '@/views/LoginView.vue'
import FindDoctorView from '@/views/FindDoctorView.vue'
import RegisterView from '@/views/RegisterView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: LandingView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/find-doctor', name: 'find-doctor', component: FindDoctorView },
    { path: '/register', name: 'register', component: RegisterView },
  ],
})

export default router
