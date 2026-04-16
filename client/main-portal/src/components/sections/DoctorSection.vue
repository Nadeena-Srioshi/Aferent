<template>
  <section class="py-16 lg:py-20 relative overflow-hidden" aria-labelledby="doctor-cta-heading">
    <!-- Background accent -->
    <div class="absolute inset-0 bg-gradient-to-br from-[#004D51] to-[#006064]" aria-hidden="true"></div>
    <div class="absolute top-0 right-0 w-96 h-96 rounded-full opacity-10 bg-action translate-x-1/2 -translate-y-1/2" aria-hidden="true"></div>
    <div class="absolute bottom-0 left-0 w-64 h-64 rounded-full opacity-10 bg-white -translate-x-1/3 translate-y-1/3" aria-hidden="true"></div>

    <div class="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex flex-col lg:flex-row items-center gap-10 lg:gap-16">

        <!-- Left: content -->
        <div class="flex-1 text-center lg:text-left">
          <div class="inline-flex items-center gap-2 bg-white/15 text-white text-xs font-semibold px-4 py-2 rounded-full mb-5 uppercase tracking-wider">
            <Stethoscope class="w-3.5 h-3.5" aria-hidden="true" />
            For Healthcare Professionals
          </div>

          <h2 id="doctor-cta-heading" class="text-3xl sm:text-4xl font-extrabold text-white leading-tight mb-4">
            Are you a healthcare specialist?<br />
            <span class="text-white/70">Join us.</span>
          </h2>

          <p class="text-white/70 text-base leading-relaxed mb-8 max-w-lg mx-auto lg:mx-0">
            Connect with patients who need your expertise. Manage your schedule, consultations, and practice — all in one place. Verification is straightforward and support is always available.
          </p>

          <div class="flex flex-col sm:flex-row gap-3 justify-center lg:justify-start">
            <RouterLink
              to="/doctor-register"
              class="inline-flex items-center justify-center gap-2 px-7 py-3.5 bg-white text-primary font-bold rounded-xl hover:bg-white/90 shadow-md hover:shadow-lg transition-all duration-200 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-primary"
            >
              Register as a Doctor
              <ArrowRight class="w-4 h-4" aria-hidden="true" />
            </RouterLink>
            <RouterLink
              to="/login"
              class="inline-flex items-center justify-center gap-2 px-7 py-3.5 border-2 border-white/30 text-white font-semibold rounded-xl hover:border-white hover:bg-white/10 transition-all duration-200 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
            >
              Already registered? Sign in
            </RouterLink>
          </div>
        </div>

        <!-- Right: feature pills -->
        <div class="flex-shrink-0 grid grid-cols-2 gap-3 w-full max-w-xs sm:max-w-sm">
          <div
            v-for="feature in features"
            :key="feature.label"
            class="flex flex-col gap-2 bg-white/10 border border-white/15 rounded-2xl px-4 py-4 backdrop-blur-sm"
          >
            <div class="w-8 h-8 rounded-xl bg-white/15 flex items-center justify-center">
              <component :is="feature.icon" class="w-4 h-4 text-white" aria-hidden="true" />
            </div>
            <p class="text-white text-xs font-semibold leading-snug">{{ feature.label }}</p>
          </div>
        </div>

      </div>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import {
  Stethoscope,
  ArrowRight,
  CalendarCheck,
  ShieldCheck,
  Users,
  TrendingUp,
} from 'lucide-vue-next'

const features = [
  { icon: CalendarCheck, label: 'Flexible scheduling' },
  { icon: ShieldCheck,   label: 'Verified practice' },
  { icon: Users,         label: 'Growing patient base' },
  { icon: TrendingUp,    label: 'Practice analytics' },
]
</script>