<template>
  <main id="main-content" class="bg-surface">

    <!-- Hero -->
    <section class="bg-white border-b border-border py-16 lg:py-24" aria-labelledby="about-heading">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 text-center">
        <span class="inline-block text-xs font-semibold text-primary uppercase tracking-widest mb-4">Our Mission</span>
        <h1 id="about-heading" class="text-4xl sm:text-5xl font-extrabold text-ink leading-tight mb-6">
          Healthcare that's built<br />around the <span class="text-primary">patient</span>.
        </h1>
        <p class="text-lg text-muted leading-relaxed max-w-2xl mx-auto">
          Aferent was founded on a simple belief: access to quality healthcare should never depend on where you live, how much time you have, or how comfortable you feel navigating a clinic. We're changing that.
        </p>
      </div>
    </section>

    <!-- Values -->
    <section class="py-16 lg:py-24" aria-labelledby="values-heading">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 id="values-heading" class="text-3xl font-bold text-ink mb-3">What drives us</h2>
        </div>
        <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <div v-for="value in values" :key="value.title" class="bg-card border border-border rounded-2xl p-6">
            <div class="w-12 h-12 rounded-2xl flex items-center justify-center mb-4" :class="value.bg" aria-hidden="true">
              <component :is="value.icon" class="w-6 h-6" :class="value.color" />
            </div>
            <h3 class="font-bold text-ink mb-2">{{ value.title }}</h3>
            <p class="text-sm text-muted leading-relaxed">{{ value.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats band -->
    <section class="bg-primary py-12" aria-label="Platform statistics">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-8 text-center" role="list">
          <div v-for="s in stats" :key="s.label" role="listitem">
            <div class="text-3xl font-extrabold text-white mb-1">{{ s.value }}</div>
            <div class="text-sm text-white/70">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="bg-white py-16 lg:py-20 text-center" aria-labelledby="about-cta">
      <div class="max-w-2xl mx-auto px-4 sm:px-6">
        <h2 id="about-cta" class="text-3xl font-bold text-ink mb-4">Ready to experience better care?</h2>
        <p class="text-muted mb-8">Join thousands of patients who've made Aferent part of their health routine.</p>
        <RouterLink
          to="/register"
          class="inline-flex items-center gap-2 px-8 py-4 bg-primary text-white font-bold rounded-xl hover:bg-action shadow-md transition-all duration-200 text-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
        >
          Get Started — It's Free
          <ArrowRight class="w-5 h-5" aria-hidden="true" />
        </RouterLink>
      </div>
    </section>

  </main>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import { Accessibility, ArrowRight, Eye, HeartHandshake, RefreshCw, ShieldCheck, Sparkles } from 'lucide-vue-next'

const values = [
  { title: 'Patient First',      description: 'Every design decision, product feature, and partnership is evaluated through one lens: does it serve the patient better?', icon: HeartHandshake, bg: 'bg-danger/10', color: 'text-danger' },
  { title: 'Radical Transparency', description: 'No hidden fees, no opaque algorithms. You always know what you\'re getting and why our AI makes the recommendations it does.', icon: Eye, bg: 'bg-primary/10', color: 'text-primary' },
  { title: 'Inclusive Access',   description: 'We build for every patient — including those with disabilities, low-bandwidth connections, and limited technical experience.', icon: Accessibility, bg: 'bg-ai/10', color: 'text-ai' },
  { title: 'Privacy by Design',  description: 'Your health data is yours. We never sell it, and our systems are architected so even we can\'t read your medical records.', icon: ShieldCheck, bg: 'bg-success/10', color: 'text-success' },
  { title: 'AI that Assists',    description: 'Our AI supports doctors and patients — it never replaces clinical judgment. Every AI output is a suggestion, not a decision.', icon: Sparkles, bg: 'bg-warning/10', color: 'text-warning' },
  { title: 'Continuous Improvement', description: 'We ship improvements every week based on patient feedback. If something doesn\'t serve you, we fix it — fast.', icon: RefreshCw, bg: 'bg-ai/10', color: 'text-ai' },
]

const stats = [
  { value: '2022', label: 'Founded' },
  { value: '50K+', label: 'Patients served' },
  { value: '12',   label: 'Countries' },
  { value: '4.9★', label: 'App Store rating' },
]
</script>