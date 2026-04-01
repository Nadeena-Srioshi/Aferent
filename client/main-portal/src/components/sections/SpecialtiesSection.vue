<template>
  <section class="bg-white py-16 lg:py-24" aria-labelledby="specialties-heading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

      <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-10">
        <div>
          <span class="inline-block text-xs font-semibold text-primary uppercase tracking-widest mb-3">Browse by Care Area</span>
          <h2 id="specialties-heading" class="text-3xl sm:text-4xl font-bold text-ink">Top Specialties</h2>
        </div>
        <RouterLink
          to="/specialties"
          class="text-sm font-semibold text-primary hover:text-action underline-offset-2 hover:underline transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded"
        >
          View all specialties →
        </RouterLink>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4" role="list" aria-label="Medical specialties">
        <RouterLink
          v-for="specialty in specialties"
          :key="specialty.name"
          :to="`/specialties/${specialty.slug}`"
          role="listitem"
          class="group flex flex-col items-center text-center p-5 rounded-2xl border border-border bg-white hover:border-primary hover:shadow-md transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
        >
          <div
            class="w-14 h-14 rounded-2xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-200"
            :class="specialty.bg"
            aria-hidden="true"
          >
            <svg class="w-7 h-7" :class="specialty.color" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" :d="specialty.icon"/>
            </svg>
          </div>
          <h3 class="font-semibold text-ink text-sm leading-tight mb-1 group-hover:text-primary transition-colors">{{ specialty.name }}</h3>
          <p class="text-xs text-muted">{{ specialty.doctorCount }} doctors</p>
        </RouterLink>
      </div>

    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'

const specialties = [
  {
    name: 'Cardiology',
    slug: 'cardiology',
    doctorCount: '84',
    icon: 'M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z',
    bg: 'bg-danger/10',
    color: 'text-danger',
  },
  {
    name: 'Pediatrics',
    slug: 'pediatrics',
    doctorCount: '62',
    icon: 'M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
    bg: 'bg-warning/10',
    color: 'text-warning',
  },
  {
    name: 'Mental Health',
    slug: 'mental-health',
    doctorCount: '120',
    icon: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z',
    bg: 'bg-ai/10',
    color: 'text-ai',
  },
  {
    name: 'Dermatology',
    slug: 'dermatology',
    doctorCount: '48',
    icon: 'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01',
    bg: 'bg-primary/10',
    color: 'text-primary',
  },
  {
    name: 'Neurology',
    slug: 'neurology',
    doctorCount: '37',
    icon: 'M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z',
    bg: 'bg-success/10',
    color: 'text-success',
  },
  {
    name: 'Orthopedics',
    slug: 'orthopedics',
    doctorCount: '55',
    icon: 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4',
    bg: 'bg-muted/10',
    color: 'text-muted',
  },
  {
    name: "Women's Health",
    slug: 'womens-health',
    doctorCount: '73',
    icon: 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
    bg: 'bg-danger/10',
    color: 'text-danger',
  },
  {
    name: 'Endocrinology',
    slug: 'endocrinology',
    doctorCount: '29',
    icon: 'M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.153-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z',
    bg: 'bg-ai/10',
    color: 'text-ai',
  },
]
</script>