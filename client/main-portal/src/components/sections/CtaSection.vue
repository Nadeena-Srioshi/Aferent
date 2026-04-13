<template>
  <section class="bg-white py-16 lg:py-24" aria-labelledby="cta-heading">
    <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 text-center">

      <div class="inline-flex items-center gap-2 bg-success/10 text-success text-xs font-semibold px-4 py-2 rounded-full mb-6 uppercase tracking-wider">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
        </svg>
        Free to get started
      </div>

      <h2 id="cta-heading" class="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-ink mb-5">
        Take charge of your health today
      </h2>
      <p class="text-muted text-lg leading-relaxed mb-10">
        Create your free patient account in under 2 minutes. Access your health records, book appointments, and connect with doctors from anywhere.
      </p>

      <div class="flex flex-col sm:flex-row gap-4 justify-center mb-8">
        <RouterLink
          to="/register"
          class="inline-flex items-center justify-center gap-2 px-8 py-4 bg-primary text-white font-bold rounded-xl hover:bg-action shadow-md hover:shadow-lg transition-all duration-200 text-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
        >
          Create Free Account
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
          </svg>
        </RouterLink>
        <RouterLink
          to="/find-doctor"
          class="inline-flex items-center justify-center gap-2 px-8 py-4 border-2 border-border text-ink font-semibold rounded-xl hover:border-primary hover:text-primary transition-all duration-200 text-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
        >
          Browse Doctors
        </RouterLink>
      </div>

      <!-- Reassurances -->
      <ul class="flex flex-wrap justify-center gap-x-8 gap-y-2" aria-label="Account benefits">
        <li v-for="item in perks" :key="item" class="flex items-center gap-1.5 text-sm text-muted">
          <svg class="w-4 h-4 text-success flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
          </svg>
          {{ item }}
        </li>
      </ul>

    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'

const perks = [
  'No credit card required',
  'Cancel anytime',
  'HIPAA compliant',
  'Your data stays private',
]
</script>