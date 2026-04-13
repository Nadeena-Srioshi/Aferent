<template>
  <section class="bg-white py-16 lg:py-24" aria-labelledby="specialties-heading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

      <div class="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-10">
        <div>
          <span class="inline-block text-xs font-semibold text-primary uppercase tracking-widest mb-3">Browse by Care Area</span>
          <h2 id="specialties-heading" class="text-3xl sm:text-4xl font-bold text-ink">Top Specialties</h2>
        </div>
        <RouterLink
          to="/find-doctor"
          class="text-sm font-semibold text-primary hover:text-action underline-offset-2 hover:underline transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded"
        >
          View all specialties →
        </RouterLink>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4" role="list" aria-label="Medical specialties">
        <RouterLink
          v-for="specialty in specialties"
          :key="specialty.name"
          :to="'/find-doctor'"
          role="listitem"
          class="group flex flex-col items-center text-center p-5 rounded-2xl border border-border bg-white hover:border-primary hover:shadow-md transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
        >
          <div
            class="w-14 h-14 rounded-2xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-200"
            :class="specialty.bg"
            aria-hidden="true"
          >
            <component :is="specialty.icon" class="w-7 h-7" :class="specialty.color" />
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
import { Baby, Bone, Brain, Dna, Eye, FlaskConical, HeartPulse, Venus } from 'lucide-vue-next'

const specialties = [
  {
    name: 'Cardiology',
    slug: 'cardiology',
    doctorCount: '84',
    icon: HeartPulse,
    bg: 'bg-danger/10',
    color: 'text-danger',
  },
  {
    name: 'Pediatrics',
    slug: 'pediatrics',
    doctorCount: '62',
    icon: Baby,
    bg: 'bg-warning/10',
    color: 'text-warning',
  },
  {
    name: 'Mental Health',
    slug: 'mental-health',
    doctorCount: '120',
    icon: Brain,
    bg: 'bg-ai/10',
    color: 'text-ai',
  },
  {
    name: 'Dermatology',
    slug: 'dermatology',
    doctorCount: '48',
    icon: Eye,
    bg: 'bg-primary/10',
    color: 'text-primary',
  },
  {
    name: 'Neurology',
    slug: 'neurology',
    doctorCount: '37',
    icon: Dna,
    bg: 'bg-success/10',
    color: 'text-success',
  },
  {
    name: 'Orthopedics',
    slug: 'orthopedics',
    doctorCount: '55',
    icon: Bone,
    bg: 'bg-muted/10',
    color: 'text-muted',
  },
  {
    name: "Women's Health",
    slug: 'womens-health',
    doctorCount: '73',
    icon: Venus,
    bg: 'bg-danger/10',
    color: 'text-danger',
  },
  {
    name: 'Endocrinology',
    slug: 'endocrinology',
    doctorCount: '29',
    icon: FlaskConical,
    bg: 'bg-ai/10',
    color: 'text-ai',
  },
]
</script>