<template>
  <section class="bg-primary py-12" aria-label="Platform trust metrics">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-8" role="list">
        <div
          v-for="metric in metrics"
          :key="metric.label"
          class="text-center"
          role="listitem"
        >
          <div class="text-3xl sm:text-4xl font-extrabold text-white mb-1">{{ metric.value }}</div>
          <div class="text-sm text-white/70 font-medium">{{ metric.label }}</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
const metrics = [
  { value: '50,000+', label: 'Patients Helped' },
  { value: '1,200+', label: 'Verified Doctors' },
  { value: '98%', label: 'Satisfaction Rate' },
  { value: '< 2 min', label: 'Average Booking Time' },
]
</script>