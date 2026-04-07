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
  ],
})

export default router
