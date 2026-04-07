<template>
  <section class="relative bg-surface overflow-hidden" aria-labelledby="hero-heading">
    <!-- Subtle background pattern -->
    <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
      <div class="absolute top-0 right-0 w-[600px] h-[600px] bg-primary/5 rounded-full translate-x-1/3 -translate-y-1/4" />
      <div class="absolute bottom-0 left-0 w-[400px] h-[400px] bg-ai/5 rounded-full -translate-x-1/4 translate-y-1/4" />
      <!-- Dot grid pattern -->
      <svg class="absolute inset-0 w-full h-full opacity-[0.03]" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <pattern id="dots" x="0" y="0" width="24" height="24" patternUnits="userSpaceOnUse">
            <circle cx="2" cy="2" r="1.5" class="text-primary" fill="currentColor" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#dots)" />
      </svg>
    </div>

    <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-16 pb-10 lg:pt-24 lg:pb-16">
      <div class="grid lg:grid-cols-2 gap-12 items-center">

        <!-- Left: Copy -->
        <div>

          <h1 id="hero-heading" class="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-ink leading-tight tracking-tight mb-6">
            Your health,<br />
            <span class="text-primary">on your terms.</span>
          </h1>

          <p class="text-lg text-muted leading-relaxed mb-8 max-w-lg">
            Find the right doctor, book an appointment in minutes, and consult via secure video — all from one platform built around <em>you</em>.
          </p>

          <!-- CTA Buttons -->
          <div class="flex flex-col sm:flex-row gap-4 mb-10">
            <RouterLink
              to="/register"
              class="inline-flex items-center justify-center gap-2 px-7 py-4 bg-primary text-white font-semibold rounded-xl hover:bg-action shadow-sm hover:shadow-md transition-all duration-200 text-base focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
              </svg>
              Book an Appointment
            </RouterLink>
            <RouterLink
              to="/ai-tools"
              class="group inline-flex items-center justify-center gap-2 px-7 py-4 border-2 border-ai text-ai font-semibold rounded-xl hover:bg-ai transition-all duration-200 text-base focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ai focus-visible:ring-offset-2"
            >
              <svg class="w-5 h-5 text-ai group-hover:text-card transition-colors duration-200" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
              </svg>
              <span class="text-ai group-hover:text-card transition-colors duration-200">Try AI Symptom Check</span>
            </RouterLink>
          </div>


        </div>

        <!-- Right: Visual card stack -->
        <div class="relative hidden lg:flex items-center justify-center">
          <div class="relative w-full max-w-md">

            <!-- Main appointment card -->
            <div class="bg-card rounded-2xl border border-border shadow-lg p-6 mb-4">
              <div class="flex items-start justify-between mb-5">
                <div>
                  <p class="text-xs text-muted font-medium uppercase tracking-wider mb-1">Next Appointment</p>
                  <p class="font-bold text-ink text-lg">Dr. Sarah Mitchell</p>
                  <p class="text-sm text-muted">General Practitioner</p>
                </div>
                <span class="bg-success/10 text-success text-xs font-semibold px-3 py-1 rounded-full">Confirmed</span>
              </div>
              <div class="flex items-center gap-3 text-sm text-muted mb-4">
                <svg class="w-4 h-4 text-primary flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
                <span>Tomorrow, 10:30 AM</span>
                <span class="w-1 h-1 rounded-full bg-border" aria-hidden="true"></span>
                <svg class="w-4 h-4 text-primary flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.069A1 1 0 0121 8.82v6.36a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                </svg>
                <span>Video Consult</span>
              </div>
              <div class="w-full h-2 bg-surface rounded-full overflow-hidden">
                <div class="h-2 bg-primary rounded-full" style="width: 72%" role="progressbar" aria-valuenow="72" aria-valuemin="0" aria-valuemax="100" aria-label="72% of appointment preparation complete"></div>
              </div>
              <p class="text-xs text-muted mt-1.5">Profile 72% complete — add insurance to finish</p>
            </div>

            <!-- AI insight chip -->
            <div class="bg-ai/10 border border-ai/20 rounded-2xl p-4 flex items-start gap-3">
              <div class="w-9 h-9 rounded-xl bg-ai flex items-center justify-center flex-shrink-0" aria-hidden="true">
                <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
              </div>
              <div>
                <p class="text-xs font-semibold text-ai mb-0.5">AI Health Insight</p>
                <p class="text-sm text-ink">Your blood pressure trend improved <strong>12%</strong> this month. Keep it up!</p>
              </div>
            </div>

          </div>
        </div>

      </div>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'

const stats = [
  { value: '50K+', label: 'Patients served' },
  { value: '1,200+', label: 'Verified doctors' },
  { value: '4.9★', label: 'Average rating' },
  { value: '24/7', label: 'AI support' },
]
</script>