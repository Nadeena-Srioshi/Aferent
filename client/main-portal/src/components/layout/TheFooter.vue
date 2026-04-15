<template>
  <footer class="bg-ink text-white" role="contentinfo">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14">

      <!-- ── Main grid ──────────────────────────────────────── -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-10 mb-12">

        <!-- Brand column -->
        <div class="lg:col-span-1">
          <RouterLink
            to="/"
            class="flex items-center gap-2 mb-4 w-fit rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
            aria-label="Aferent — go to homepage"
          >
            <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0">
              <HeartPulse class="w-4.5 h-4.5 text-white" aria-hidden="true" />
            </div>
            <span class="text-xl font-bold tracking-tight">Aferent</span>
          </RouterLink>

          <p class="text-sm text-white/60 leading-relaxed mb-5">
            Smarter healthcare, closer to you. Book appointments, consult via video, and let AI guide your health journey.
          </p>

          <!-- Social icons -->
          <div class="flex gap-2" role="list" aria-label="Social media links">
            <a
              v-for="s in socials"
              :key="s.label"
              :href="s.href"
              :aria-label="s.label"
              role="listitem"
              target="_blank"
              rel="noopener noreferrer"
              class="w-9 h-9 rounded-lg bg-white/10 hover:bg-primary flex items-center justify-center transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
            >
              <component :is="s.icon" class="w-4 h-4" aria-hidden="true" />
            </a>
          </div>
        </div>

        <!-- Link columns -->
        <div v-for="section in footerLinks" :key="section.title">
          <h3 class="text-xs font-semibold text-white/40 uppercase tracking-widest mb-4">
            {{ section.title }}
          </h3>
          <ul class="space-y-2.5">
            <li v-for="link in section.links" :key="link.label">
              <RouterLink
                :to="link.to"
                class="text-sm text-white/70 hover:text-white transition-colors duration-150 focus-visible:outline-none focus-visible:underline"
              >
                {{ link.label }}
              </RouterLink>
            </li>
          </ul>
        </div>

      </div>

      <!-- ── Authenticated quick links ─────────────────────── -->
      <div
        v-if="isAuthenticated"
        class="mb-10 p-5 rounded-2xl border border-white/10 bg-white/5"
      >
        <p class="text-xs font-semibold text-white/40 uppercase tracking-widest mb-3">
          Quick Access
        </p>
        <div class="flex flex-wrap gap-3">
          <RouterLink
            v-for="item in quickLinks"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/10 hover:bg-primary text-sm font-medium text-white/80 hover:text-white transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
          >
            <component :is="item.icon" class="w-4 h-4" aria-hidden="true" />
            {{ item.label }}
          </RouterLink>
        </div>
      </div>

      <!-- ── Bottom bar ─────────────────────────────────────── -->
      <div class="border-t border-white/10 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
        <p class="text-xs text-white/40">
          © {{ year }} Aferent Health. All rights reserved.
        </p>
        <div class="flex flex-wrap justify-center gap-x-6 gap-y-1">
          <RouterLink
            v-for="legal in legalLinks"
            :key="legal.to"
            :to="legal.to"
            class="text-xs text-white/40 hover:text-white transition-colors focus-visible:outline-none focus-visible:underline"
          >
            {{ legal.label }}
          </RouterLink>
        </div>
      </div>

    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuth } from '@/stores/useAuth'
import {
  Facebook,
  FileText,
  HeartPulse,
  House,
  Instagram,
  Settings,
  Sparkles,
  Stethoscope,
  Twitter,
} from 'lucide-vue-next'

const auth = useAuth()
const { isAuthenticated } = storeToRefs(auth)

const year = new Date().getFullYear()

// ── Socials ───────────────────────────────────────────────────
const socials = [
  {
    label: 'Facebook',
    href: '#',
    icon: Facebook,
  },
  {
    label: 'Twitter / X',
    href: '#',
    icon: Twitter,
  },
  {
    label: 'Instagram',
    href: '#',
    icon: Instagram,
  },
]

// ── Footer link columns ───────────────────────────────────────
const footerLinks = [
  {
    title: 'For Patients',
    links: [
      { label: 'Find a Doctor',   to: '/find-doctor' },
      { label: 'Book Appointment',to: '/find-doctor' },
      { label: 'Telemedicine',    to: '/telemedicine' },
      { label: 'AI Symptom Check',to: '/ai-tools' },
      { label: 'Medical Records', to: '/records' },
    ],
  },
  {
    title: 'Specialties',
    links: [
      { label: 'Cardiology',    to: '/specialties/cardiology' },
      { label: 'Pediatrics',    to: '/specialties/pediatrics' },
      { label: 'Mental Health', to: '/specialties/mental-health' },
      { label: 'Dermatology',   to: '/specialties/dermatology' },
      { label: 'All Specialties',to: '/specialties' },
    ],
  },
  {
    title: 'Company',
    links: [
      { label: 'About Us',   to: '/about' },
      { label: 'Blog',       to: '/blog' },
      { label: 'Contact',    to: '/contact' },
      { label: 'Careers',    to: '/careers' },
      { label: 'Help Center',to: '/help' },
    ],
  },
]

// ── Authenticated quick-access strip ─────────────────────────
const quickLinks = [
  { label: 'Dashboard',     to: '/dashboard',    icon: House },
  { label: 'Appointments',  to: '/appointments', icon: Stethoscope },
  { label: 'Records',       to: '/records',      icon: FileText },
  { label: 'AI Tools',      to: '/ai-tools',     icon: Sparkles },
  { label: 'Settings',      to: '/settings',     icon: Settings },
]

// ── Legal links ───────────────────────────────────────────────
const legalLinks = [
  { label: 'Privacy Policy', to: '/privacy' },
  { label: 'Terms of Use',   to: '/terms' },
  { label: 'Accessibility',  to: '/accessibility' },
  { label: 'Cookie Policy',  to: '/cookies' },
]
</script>