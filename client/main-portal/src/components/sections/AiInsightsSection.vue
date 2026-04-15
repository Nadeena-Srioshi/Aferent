<template>
  <section class="bg-surface py-16 lg:py-24" aria-labelledby="ai-section-heading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

      <div class="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">

        <!-- Left: Feature cards -->
        <div class="space-y-4" aria-label="AI features list">
          <div
            v-for="feature in aiFeatures"
            :key="feature.title"
            class="flex items-start gap-4 bg-white border border-border rounded-2xl p-5 hover:border-ai/40 hover:shadow-sm transition-all duration-200"
          >
            <div class="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0" :class="feature.bg" aria-hidden="true">
              <component :is="feature.icon" class="w-5 h-5" :class="feature.iconColor" />
            </div>
            <div>
              <h3 class="font-semibold text-ink mb-1">{{ feature.title }}</h3>
              <p class="text-sm text-muted leading-relaxed">{{ feature.description }}</p>
            </div>
          </div>
        </div>

        <!-- Right: Copy -->
        <div>
          <span class="inline-block text-xs font-semibold text-ai uppercase tracking-widest mb-3">Powered by AI</span>
          <h2 id="ai-section-heading" class="text-3xl sm:text-4xl font-bold text-ink mb-5">
            Your personal health<br />intelligence layer
          </h2>
          <p class="text-muted text-lg leading-relaxed mb-6">
            Aferent's AI goes beyond scheduling. It learns from your health history to surface insights, flag potential risks, and keep you informed between appointments.
          </p>
          <p class="text-muted leading-relaxed mb-8">
            Personalized medication reminders, symptom pattern analysis, and health scoring — all private, secure, and designed to complement your care team, never replace them.
          </p>

          <div class="flex flex-col sm:flex-row gap-4">
            <RouterLink
              to="/about"
              class="inline-flex items-center justify-center gap-2 px-6 py-3.5 bg-ai text-white font-semibold rounded-xl hover:opacity-90 shadow-sm hover:shadow-md transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ai focus-visible:ring-offset-2"
            >
              <Sparkles class="w-5 h-5" aria-hidden="true" />
              Explore AI Tools
            </RouterLink>
            <RouterLink
              to="/about"
              class="inline-flex items-center justify-center gap-2 px-6 py-3.5 border-2 border-border text-ink font-semibold rounded-xl hover:border-ai hover:text-ai transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ai focus-visible:ring-offset-2"
            >
              How it works
            </RouterLink>
          </div>

          <!-- Trust note -->
          <p class="mt-6 text-xs text-muted flex items-center gap-2">
            <ShieldCheck class="w-4 h-4 text-success flex-shrink-0" aria-hidden="true" />
            Your data is encrypted, never sold, and always under your control.
          </p>
        </div>

      </div>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import { BarChart3, BellRing, Brain, ShieldCheck, Sparkles } from 'lucide-vue-next'

const aiFeatures = [
  {
    title: 'AI Symptom Detector',
    description: 'Describe how you feel and our AI maps your symptoms to potential conditions — helping you choose the right specialist.',
    icon: Brain,
    bg: 'bg-ai/10',
    iconColor: 'text-ai',
  },
  {
    title: 'Personalized Health Score',
    description: 'A dynamic score based on your vitals, appointment history, and lifestyle inputs — with actionable recommendations.',
    icon: BarChart3,
    bg: 'bg-primary/10',
    iconColor: 'text-primary',
  },
  {
    title: 'Medication Reminders',
    description: 'Smart reminders adapt to your schedule and alert you to potential drug interactions flagged from your records.',
    icon: BellRing,
    bg: 'bg-warning/10',
    iconColor: 'text-warning',
  },
]
</script>