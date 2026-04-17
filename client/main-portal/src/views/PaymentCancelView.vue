<template>
  <main id="main-content" class="min-h-screen bg-surface">
    <section class="max-w-3xl mx-auto px-4 sm:px-6 py-12">
      <div class="bg-card border border-border rounded-2xl p-6 sm:p-8 shadow-sm">
        <p class="text-xs font-semibold uppercase tracking-wide text-warning mb-2">Payment not completed</p>
        <h1 class="text-2xl sm:text-3xl font-bold text-ink">Checkout was cancelled</h1>
        <p class="mt-3 text-muted">
          Your appointment is still saved. You can return to appointments and complete payment whenever you are ready.
        </p>

        <div class="mt-6 flex flex-col sm:flex-row gap-3">
          <RouterLink
            to="/appointments"
            class="inline-flex items-center justify-center px-5 py-2.5 bg-primary text-white font-semibold rounded-xl hover:bg-action transition-colors"
          >
            Back to Appointments
          </RouterLink>
          <RouterLink
            to="/find-doctor"
            class="inline-flex items-center justify-center px-5 py-2.5 border border-border text-ink font-semibold rounded-xl hover:border-primary hover:text-primary transition-colors"
          >
            Find a Doctor
          </RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'

onMounted(() => {
  localStorage.removeItem('aferent.payment.pendingAppointmentId')
})
</script>
