<template>
  <section class="bg-surface py-16 lg:py-24" aria-labelledby="how-it-works-heading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

      <!-- Heading -->
      <div class="text-center mb-14">
        <span class="inline-block text-xs font-semibold text-primary uppercase tracking-widest mb-3">Simple & Fast</span>
        <h2 id="how-it-works-heading" class="text-3xl sm:text-4xl font-bold text-ink mb-4">
          Healthcare in 3 steps
        </h2>
        <p class="text-muted text-lg max-w-xl mx-auto">
          From finding the right doctor to your video consultation — it's easier than you think.
        </p>
      </div>

      <!-- Steps -->
      <div class="relative">
        <!-- Connector line (desktop) -->
        <div class="hidden lg:block absolute top-14 left-1/2 -translate-x-1/2 w-2/3 h-0.5 bg-border" aria-hidden="true">
          <div class="h-full bg-primary/30 rounded-full" style="width: 100%"></div>
        </div>

        <ol class="grid grid-cols-1 sm:grid-cols-3 gap-8 relative" aria-label="3-step booking process">
          <li
            v-for="(step, i) in steps"
            :key="step.title"
            class="flex flex-col items-center text-center"
          >
            <!-- Step number -->
            <div class="relative mb-6">
              <div
                class="w-16 h-16 rounded-2xl flex items-center justify-center shadow-sm transition-transform duration-200 hover:-translate-y-1"
                :class="step.bgClass"
              >
                <component :is="'svg'" class="w-7 h-7" :class="step.iconClass" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="step.icon" />
                </component>
              </div>
              <span
                class="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-primary text-white text-xs font-bold flex items-center justify-center"
                aria-hidden="true"
              >{{ i + 1 }}</span>
            </div>

            <h3 class="text-lg font-bold text-ink mb-2">{{ step.title }}</h3>
            <p class="text-sm text-muted leading-relaxed max-w-xs">{{ step.description }}</p>
          </li>
        </ol>
      </div>

      <!-- CTA -->
      <div class="mt-14 text-center">
        <RouterLink
          to="/find-doctor"
          class="inline-flex items-center gap-2 px-8 py-4 bg-primary text-white font-semibold rounded-xl hover:bg-action shadow-sm hover:shadow-md transition-all duration-200 text-base focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
        >
          Start Your Journey
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
          </svg>
        </RouterLink>
      </div>

    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'

const steps = [
  {
    title: 'Select a Doctor',
    description: 'Search by specialty, condition, or name. Read verified reviews and choose the best fit for your health needs.',
    icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
    bgClass: 'bg-primary/10',
    iconClass: 'text-primary',
  },
  {
    title: 'Book a Time Slot',
    description: 'See real-time availability and book instantly. Choose in-person or video — whatever works for you.',
    icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
    bgClass: 'bg-warning/10',
    iconClass: 'text-warning',
  },
  {
    title: 'Talk via Video',
    description: 'Connect with your doctor from home via our secure, encrypted video platform. No travel, no waiting rooms.',
    icon: 'M15 10l4.553-2.069A1 1 0 0121 8.82v6.36a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z',
    bgClass: 'bg-success/10',
    iconClass: 'text-success',
  },
]
</script>