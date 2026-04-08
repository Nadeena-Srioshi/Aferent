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
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
                <path d="M9 2C5.13 2 2 5.13 2 9s3.13 7 7 7 7-3.13 7-7-3.13-7-7-7zm0 3a2 2 0 110 4 2 2 0 010-4zm0 9.2a5.01 5.01 0 01-4.2-2.28C4.81 10.34 7.27 9.8 9 9.8c1.72 0 4.18.54 4.2 2.12A5.01 5.01 0 019 14.2z" fill="white"/>
              </svg>
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
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path :d="s.svgPath"/>
              </svg>
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
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon"/>
            </svg>
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

const auth = useAuth()
const { isAuthenticated } = storeToRefs(auth)

const year = new Date().getFullYear()

// ── Socials ───────────────────────────────────────────────────
const socials = [
  {
    label: 'Facebook',
    href: '#',
    svgPath: 'M18 2h-3a5 5 0 00-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 011-1h3z',
  },
  {
    label: 'Twitter / X',
    href: '#',
    svgPath: 'M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z',
  },
  {
    label: 'Instagram',
    href: '#',
    svgPath: 'M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z',
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
  { label: 'Dashboard',     to: '/dashboard',    icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
  { label: 'Appointments',  to: '/appointments', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
  { label: 'Records',       to: '/records',      icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
  { label: 'AI Tools',      to: '/ai-tools',     icon: 'M13 10V3L4 14h7v7l9-11h-7z' },
  { label: 'Settings',      to: '/settings',     icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z' },
]

// ── Legal links ───────────────────────────────────────────────
const legalLinks = [
  { label: 'Privacy Policy', to: '/privacy' },
  { label: 'Terms of Use',   to: '/terms' },
  { label: 'Accessibility',  to: '/accessibility' },
  { label: 'Cookie Policy',  to: '/cookies' },
]
</script>