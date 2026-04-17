<template>
  <main id="main-content" class="min-h-screen bg-surface">
    <section class="max-w-3xl mx-auto px-4 sm:px-6 py-12">
      <div class="bg-card border border-border rounded-2xl p-6 sm:p-8 shadow-sm">
        <p class="text-xs font-semibold uppercase tracking-wide mb-2" :class="statusTone">
          {{ statusTitle }}
        </p>
        <h1 class="text-2xl sm:text-3xl font-bold text-ink">Payment confirmation</h1>
        <p class="mt-3 text-muted">{{ description }}</p>

        <div class="mt-5 p-4 rounded-xl border border-border bg-surface/60 text-sm text-muted">
          <p v-if="sessionId"><span class="font-semibold text-ink">Session:</span> {{ sessionId }}</p>
          <p v-if="appointmentId" class="mt-1"><span class="font-semibold text-ink">Appointment:</span> {{ appointmentId }}</p>
          <p v-if="paymentStatus" class="mt-1"><span class="font-semibold text-ink">Payment status:</span> {{ paymentStatus }}</p>
        </div>

        <div class="mt-6 flex flex-col sm:flex-row gap-3">
          <RouterLink
            to="/appointments"
            class="inline-flex items-center justify-center px-5 py-2.5 bg-primary text-white font-semibold rounded-xl hover:bg-action transition-colors"
          >
            Go to Appointments
          </RouterLink>
          <RouterLink
            to="/medical-history"
            class="inline-flex items-center justify-center px-5 py-2.5 border border-border text-ink font-semibold rounded-xl hover:border-primary hover:text-primary transition-colors"
          >
            Continue
          </RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import { getPaymentByAppointment } from '@/services/paymentService'

const route = useRoute()
const auth = useAuth()
const notify = useNotificationStore()

const state = ref('loading')
const paymentStatus = ref('')

const sessionId = computed(() => String(route.query.session_id || ''))
const appointmentId = ref('')

const statusTitle = computed(() => {
  if (state.value === 'success') return 'Payment successful'
  if (state.value === 'pending') return 'Payment processing'
  if (state.value === 'failed') return 'Payment issue detected'
  return 'Verifying payment'
})

const statusTone = computed(() => {
  if (state.value === 'success') return 'text-success'
  if (state.value === 'pending') return 'text-warning'
  if (state.value === 'failed') return 'text-danger'
  return 'text-primary'
})

const description = computed(() => {
  if (state.value === 'success') return 'Your payment is confirmed. Your appointment status should now update to confirmed.'
  if (state.value === 'pending') return 'Your checkout succeeded, but backend confirmation is still syncing. Check your appointments in a few moments.'
  if (state.value === 'failed') return 'We could not verify a successful payment yet. You can retry from your appointments page.'
  return 'We are checking payment status with the server.'
})

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

async function reconcilePayment() {
  const candidate =
    String(route.query.appointmentId || '') ||
    localStorage.getItem('aferent.payment.pendingAppointmentId') ||
    ''

  appointmentId.value = candidate

  if (!candidate || !auth.token || !auth.user?.authId) {
    state.value = 'pending'
    return
  }

  for (let attempt = 0; attempt < 12; attempt += 1) {
    try {
      const payment = await getPaymentByAppointment({
        token: auth.token,
        userId: auth.user.authId,
        userEmail: auth.user.email,
        appointmentId: candidate,
      })

      const normalized = String(payment?.status || '').toUpperCase()
      paymentStatus.value = normalized

      if (normalized === 'SUCCESS' || normalized === 'REFUNDED') {
        state.value = 'success'
        localStorage.removeItem('aferent.payment.pendingAppointmentId')
        notify.push('Payment confirmed successfully.', 'success')
        return
      }

      if (normalized === 'FAILED' || normalized === 'CANCELLED') {
        state.value = 'failed'
        return
      }

      state.value = 'pending'
    } catch {
      state.value = 'pending'
    }

    await sleep(2500)
  }
}

onMounted(async () => {
  await reconcilePayment()
})
</script>
