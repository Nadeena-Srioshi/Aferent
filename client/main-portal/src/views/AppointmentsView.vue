<template>
  <main id="main-content" class="min-h-screen bg-surface">
    <section class="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div>
          <h1 class="text-2xl sm:text-3xl font-bold text-ink">Appointments</h1>
          <p class="text-muted mt-1">
            <span v-if="isDoctor">Review video requests, manage appointment status, and complete consultations.</span>
            <span v-else>Track your bookings, complete payments, and manage upcoming visits.</span>
          </p>
        </div>
        <button
          class="px-4 py-2 border border-border rounded-xl text-sm font-semibold text-ink hover:border-primary hover:text-primary transition-colors"
          :disabled="loading"
          @click="loadPageData"
        >
          {{ loading ? 'Refreshing...' : 'Refresh' }}
        </button>
      </div>

      <div v-if="error" class="mb-6 rounded-xl border border-danger/20 bg-danger/10 px-4 py-3 text-sm text-danger">
        {{ error }}
      </div>

      <div class="bg-card border border-border rounded-2xl p-2 mb-6 flex flex-wrap gap-2">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
          :class="activeTab === tab.key ? 'bg-primary text-white' : 'text-muted hover:bg-surface hover:text-ink'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="loading" class="grid sm:grid-cols-2 gap-4">
        <div v-for="i in 4" :key="i" class="h-44 rounded-2xl border border-border bg-card animate-pulse"></div>
      </div>

      <template v-else>
        <div v-if="activeTab !== 'payments'" class="space-y-4">
          <div
            v-if="filteredAppointments.length === 0"
            class="bg-card border border-border rounded-2xl p-8 text-center"
          >
            <p class="text-lg font-semibold text-ink">No appointments found</p>
            <p class="text-muted mt-1">
              <span v-if="isDoctor">No appointments match this doctor view right now.</span>
              <span v-else>Try booking a doctor appointment to get started.</span>
            </p>
            <RouterLink
              v-if="!isDoctor"
              to="/find-doctor"
              class="inline-flex mt-4 px-4 py-2 rounded-xl bg-primary text-white font-semibold hover:bg-action transition-colors"
            >
              Find a Doctor
            </RouterLink>
          </div>

          <div v-for="appointment in filteredAppointments" :key="appointment.id" class="space-y-2">
            <Appointmentcard
              :appointment="toCardModel(appointment)"
              :show-actions="true"
            >
              <template #actions>
                <button
                  v-if="canJoinVideoCall(appointment)"
                  class="flex-1 py-2 text-xs font-semibold text-white bg-ai rounded-xl hover:opacity-90 transition-opacity disabled:opacity-60"
                  @click="joinVideoCall(appointment)"
                >
                  Join Video Call
                </button>

                <button
                  v-if="canInitiatePayment(appointment)"
                  class="flex-1 py-2 text-xs font-semibold text-white bg-primary rounded-xl hover:bg-action transition-colors disabled:opacity-60"
                  :disabled="actionLoadingId === appointment.id"
                  @click="startPayment(appointment)"
                >
                  {{ actionLoadingId === appointment.id ? 'Processing...' : 'Pay now' }}
                </button>

                <button
                  v-if="canCancel(appointment)"
                  class="flex-1 py-2 text-xs font-semibold text-danger border border-danger/20 rounded-xl hover:bg-danger/5 transition-colors disabled:opacity-60"
                  :disabled="actionLoadingId === appointment.id"
                  @click="promptCancel(appointment)"
                >
                  Cancel
                </button>

                <button
                  v-if="isPatient"
                  class="flex-1 py-2 text-xs font-semibold text-muted border border-border rounded-xl hover:border-primary hover:text-primary transition-colors"
                  @click="rescheduleAppointment(appointment)"
                >
                  Reschedule
                </button>

                <button
                  v-if="canDoctorRespond(appointment)"
                  class="flex-1 py-2 text-xs font-semibold text-white bg-success rounded-xl hover:opacity-90 transition-opacity disabled:opacity-60"
                  :disabled="actionLoadingId === appointment.id"
                  @click="doctorUpdateStatus(appointment, 'ACCEPTED_PENDING_PAYMENT')"
                >
                  Accept
                </button>

                <button
                  v-if="canDoctorRespond(appointment)"
                  class="flex-1 py-2 text-xs font-semibold text-danger border border-danger/20 rounded-xl hover:bg-danger/5 transition-colors disabled:opacity-60"
                  :disabled="actionLoadingId === appointment.id"
                  @click="doctorUpdateStatus(appointment, 'REJECTED')"
                >
                  Reject
                </button>

                <button
                  v-if="canDoctorComplete(appointment)"
                  class="flex-1 py-2 text-xs font-semibold text-white bg-ai rounded-xl hover:opacity-90 transition-opacity disabled:opacity-60"
                  :disabled="actionLoadingId === appointment.id"
                  @click="doctorCompleteAppointment(appointment)"
                >
                  Mark completed
                </button>
              </template>
            </Appointmentcard>

            <div class="px-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
              <span>ID: {{ appointment.id }}</span>
              <span>Status: {{ humanizeAppointmentStatus(appointment.status) }}</span>
              <span v-if="appointment.paymentId">Payment ID: {{ appointment.paymentId }}</span>
              <span v-if="appointment.videoSessionLink" class="text-ai">
                <RouterLink :to="{ name: 'video-call', params: { appointmentId: appointment.id } }" class="underline">
                  Join video call
                </RouterLink>
              </span>
            </div>
          </div>
        </div>

        <div v-else class="space-y-4">
          <div
            v-if="paymentHistory.length === 0"
            class="bg-card border border-border rounded-2xl p-8 text-center"
          >
            <p class="text-lg font-semibold text-ink">No payments yet</p>
            <p class="text-muted mt-1">Payment history will appear once you start checkout from an appointment.</p>
          </div>

          <div v-for="payment in paymentHistory" :key="payment.id" class="bg-card border border-border rounded-2xl p-5">
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="text-sm text-muted">Appointment {{ payment.appointmentId }}</p>
                <p class="text-lg font-bold text-ink">{{ formatMoney(payment.amount, payment.currency) }}</p>
                <p class="text-xs text-muted mt-1">{{ formatDateTime(payment.createdAt) }}</p>
              </div>
              <span class="text-xs font-semibold px-2.5 py-1 rounded-full" :class="paymentStatusMeta(payment.status).chip">
                {{ humanizePaymentStatus(payment.status) }}
              </span>
            </div>

            <div class="mt-4 flex flex-wrap gap-2">
              <button
                v-if="payment.checkoutUrl && String(payment.status || '').toUpperCase() === 'PENDING'"
                class="px-3 py-2 text-xs font-semibold rounded-xl border border-primary text-primary hover:bg-primary/5 transition-colors"
                @click="resumeCheckout(payment)"
              >
                Continue checkout
              </button>
              <RouterLink
                to="/appointments"
                class="px-3 py-2 text-xs font-semibold rounded-xl border border-border text-muted hover:border-primary hover:text-primary transition-colors"
              >
                View appointment
              </RouterLink>
            </div>
          </div>
        </div>
      </template>
    </section>

    <ConfirmDialog
      v-model="cancelDialogOpen"
      title="Cancel appointment?"
      description="Cancellation may trigger refund rules based on the appointment time."
      confirm-label="Yes, cancel"
      cancel-label="Keep appointment"
      variant="danger"
      @confirm="confirmCancel"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import Appointmentcard from '@/components/shared/Appointmentcard.vue'
import ConfirmDialog from '@/components/shared/ConfirmDialog.vue'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import {
  cancelAppointment,
  completeAppointment,
  getPendingVideoAppointments,
  listAppointments,
  updateAppointmentStatus,
} from '@/services/appointmentService'
import { getPaymentByAppointment, initiatePayment, listPayments } from '@/services/paymentService'

const auth = useAuth()
const notify = useNotificationStore()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const actionLoadingId = ref('')
const activeTab = ref('upcoming')

const appointments = ref([])
const pendingVideo = ref([])
const payments = ref([])

const cancelDialogOpen = ref(false)
const appointmentToCancel = ref(null)

const isDoctor = computed(() => auth.isDoctor)
const isPatient = computed(() => auth.isPatient)
const currentRole = computed(() => (isDoctor.value ? 'DOCTOR' : 'PATIENT'))
const userId = computed(() => auth.user?.authId || '')
const userEmail = computed(() => auth.user?.email || '')

const tabs = computed(() => {
  if (isDoctor.value) {
    return [
      { key: 'all', label: 'All Appointments' },
      { key: 'pending-video', label: 'Pending Video' },
    ]
  }

  return [
    { key: 'upcoming', label: 'Upcoming' },
    { key: 'past', label: 'Past' },
    { key: 'payments', label: 'Payments' },
  ]
})

const filteredAppointments = computed(() => {
  if (isDoctor.value && activeTab.value === 'pending-video') {
    return sortAppointments(pendingVideo.value)
  }

  const sorted = sortAppointments(appointments.value)

  if (isDoctor.value || activeTab.value === 'all') {
    return sorted
  }

  if (activeTab.value === 'upcoming') {
    return sorted.filter((item) => !isPastStatus(item?.status))
  }

  if (activeTab.value === 'past') {
    return sorted.filter((item) => isPastStatus(item?.status))
  }

  return []
})

const paymentHistory = computed(() => {
  return [...payments.value].sort((a, b) => {
    const left = new Date(a?.createdAt || 0).getTime()
    const right = new Date(b?.createdAt || 0).getTime()
    return right - left
  })
})

function dateTimeFromAppointment(item) {
  const date = item?.appointmentDate
  const time = item?.calculatedTime || item?.videoSlotStart
  if (!date) return Number.NaN
  const iso = time ? `${date}T${String(time).slice(0, 5)}:00` : `${date}T00:00:00`
  return new Date(iso).getTime()
}

function sortAppointments(items) {
  return [...(Array.isArray(items) ? items : [])].sort((a, b) => dateTimeFromAppointment(a) - dateTimeFromAppointment(b))
}

function isPastStatus(status) {
  const value = String(status || '').toUpperCase()
  return ['COMPLETED', 'CANCELLED', 'CANCELLED_NO_REFUND', 'REJECTED'].includes(value)
}

function humanizeAppointmentStatus(status) {
  return String(status || '')
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ') || 'Unknown'
}

function humanizePaymentStatus(status) {
  return String(status || '')
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ') || 'Unknown'
}

function appointmentStatusForCard(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'CONFIRMED') return 'confirmed'
  if (value === 'COMPLETED') return 'completed'
  if (['CANCELLED', 'CANCELLED_NO_REFUND', 'REJECTED'].includes(value)) return 'cancelled'
  return 'pending'
}

function paymentStatusMeta(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'SUCCESS') return { chip: 'bg-success/10 text-success' }
  if (value === 'REFUNDED') return { chip: 'bg-primary/10 text-primary' }
  if (value === 'FAILED' || value === 'CANCELLED') return { chip: 'bg-danger/10 text-danger' }
  return { chip: 'bg-warning/10 text-warning' }
}

function toCardModel(appointment) {
  const type = String(appointment?.type || 'PHYSICAL').toLowerCase()
  const timeValue = appointment?.calculatedTime || appointment?.videoSlotStart || 'TBD'

  return {
    id: appointment?.id,
    doctorName: appointment?.doctorName || 'Doctor not specified',
    specialty: type === 'video' ? 'Telemedicine' : (appointment?.hospitalName || 'In-person visit'),
    date: formatDate(appointment?.appointmentDate),
    time: String(timeValue).slice(0, 5),
    type,
    status: appointmentStatusForCard(appointment?.status),
  }
}

function formatDate(value) {
  if (!value) return 'Date pending'
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date)
}

function formatDateTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date)
}

function formatMoney(amount, currency = 'USD') {
  const safeAmount = Number(amount || 0)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: String(currency || 'USD').toUpperCase(),
    minimumFractionDigits: 2,
  }).format(Number.isFinite(safeAmount) ? safeAmount : 0)
}

function canInitiatePayment(appointment) {
  if (!isPatient.value) return false
  const status = String(appointment?.status || '').toUpperCase()
  return ['PENDING_PAYMENT', 'ACCEPTED_PENDING_PAYMENT', 'PAYMENT_FAILED'].includes(status)
}

function canCancel(appointment) {
  if (!isPatient.value) return false
  const status = String(appointment?.status || '').toUpperCase()
  return !['CANCELLED', 'CANCELLED_NO_REFUND', 'COMPLETED'].includes(status)
}

function canDoctorRespond(appointment) {
  if (!isDoctor.value) return false
  return String(appointment?.status || '').toUpperCase() === 'PENDING_DOCTOR_APPROVAL'
}

function canDoctorComplete(appointment) {
  if (!isDoctor.value) return false
  return String(appointment?.status || '').toUpperCase() === 'CONFIRMED'
}

function canJoinVideoCall(appointment) {
  const status = String(appointment?.status || '').toUpperCase()
  const type = String(appointment?.type || '').toUpperCase()
  return type === 'VIDEO' && status === 'CONFIRMED'
}

function getAppointmentFee(appointment) {
  const fee = appointment?.consultationFee
  if (typeof fee === 'number' && Number.isFinite(fee)) return fee

  if (!fee || typeof fee !== 'object') return 0

  const isVideo = String(appointment?.type || '').toUpperCase() === 'VIDEO'
  const preferred = isVideo ? fee.video : fee.physical
  if (typeof preferred === 'number' && Number.isFinite(preferred)) return preferred

  const fallback = typeof fee.physical === 'number' ? fee.physical : fee.video
  return Number.isFinite(fallback) ? fallback : 0
}

function joinVideoCall(appointment) {
  if (!appointment?.id) return
  router.push({ name: 'video-call', params: { appointmentId: appointment.id } })
}

async function loadPageData() {
  if (!auth.token || !userId.value) {
    error.value = 'No active session found. Please sign in again.'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const tasks = [
      listAppointments({
        token: auth.token,
        userId: userId.value,
        userEmail: userEmail.value,
        role: currentRole.value,
      }),
    ]

    if (isDoctor.value) {
      tasks.push(
        getPendingVideoAppointments({
          token: auth.token,
          userId: userId.value,
          userEmail: userEmail.value,
          role: 'DOCTOR',
        }),
      )
    }

    if (isPatient.value) {
      tasks.push(
        listPayments({
          token: auth.token,
          userId: userId.value,
          userEmail: userEmail.value,
          role: 'PATIENT',
        }),
      )
    }

    const results = await Promise.allSettled(tasks)

    appointments.value = Array.isArray(results[0]?.value) ? results[0].value : []

    let cursor = 1
    if (isDoctor.value) {
      pendingVideo.value = results[cursor]?.status === 'fulfilled' && Array.isArray(results[cursor].value)
        ? results[cursor].value
        : []
      cursor += 1
    }

    if (isPatient.value) {
      payments.value = results[cursor]?.status === 'fulfilled' && Array.isArray(results[cursor].value)
        ? results[cursor].value
        : []
    }
  } catch (err) {
    error.value = err?.message || 'Failed to load appointments.'
  } finally {
    loading.value = false
  }
}

function rescheduleAppointment(appointment) {
  router.push({
    path: '/find-doctor',
    query: {
      doctorId: appointment?.doctorId || undefined,
      date: appointment?.appointmentDate || undefined,
    },
  })
}

function promptCancel(appointment) {
  appointmentToCancel.value = appointment
  cancelDialogOpen.value = true
}

async function confirmCancel() {
  const appointment = appointmentToCancel.value
  cancelDialogOpen.value = false
  appointmentToCancel.value = null

  if (!appointment?.id) return

  actionLoadingId.value = appointment.id
  try {
    const updated = await cancelAppointment({
      token: auth.token,
      userId: userId.value,
      userEmail: userEmail.value,
      appointmentId: appointment.id,
      role: 'PATIENT',
    })

    const status = String(updated?.status || '').toUpperCase()
    if (status === 'CANCELLED') {
      notify.push('Appointment cancelled. You are eligible for refund processing.', 'success')
    } else if (status === 'CANCELLED_NO_REFUND') {
      notify.push('Appointment cancelled. Refund window has passed.', 'warning')
    } else {
      notify.push('Appointment cancelled successfully.', 'success')
    }

    await loadPageData()
  } catch (err) {
    notify.push(err?.message || 'Failed to cancel appointment.', 'error')
  } finally {
    actionLoadingId.value = ''
  }
}

async function startPayment(appointment) {
  if (!appointment?.id) return

  const amount = Number(getAppointmentFee(appointment) || 0)
  if (!Number.isFinite(amount) || amount <= 0) {
    notify.push('This appointment has an invalid consultation fee.', 'error')
    return
  }

  actionLoadingId.value = appointment.id
  try {
    const payment = await initiatePayment({
      token: auth.token,
      userId: userId.value,
      userEmail: userEmail.value,
      role: 'PATIENT',
      payload: {
        appointmentId: appointment.id,
        amount,
        appointmentType: appointment.type,
        doctorId: appointment.doctorId,
      },
    })

    if (!payment?.checkoutUrl) {
      throw new Error('Checkout URL was not returned.')
    }

    localStorage.setItem('aferent.payment.pendingAppointmentId', appointment.id)
    window.location.assign(payment.checkoutUrl)
  } catch (err) {
    if (err?.status === 409) {
      try {
        const existing = await getPaymentByAppointment({
          token: auth.token,
          userId: userId.value,
          userEmail: userEmail.value,
          appointmentId: appointment.id,
          role: 'PATIENT',
        })
        if (existing?.checkoutUrl && String(existing?.status || '').toUpperCase() === 'PENDING') {
          localStorage.setItem('aferent.payment.pendingAppointmentId', appointment.id)
          window.location.assign(existing.checkoutUrl)
          return
        }
      } catch {
        // Fall through and surface original error.
      }
    }
    notify.push(err?.message || 'Unable to start payment.', 'error')
  } finally {
    actionLoadingId.value = ''
  }
}

async function doctorUpdateStatus(appointment, status) {
  if (!appointment?.id) return
  actionLoadingId.value = appointment.id

  try {
    await updateAppointmentStatus({
      token: auth.token,
      userId: userId.value,
      userEmail: userEmail.value,
      appointmentId: appointment.id,
      status,
      role: 'DOCTOR',
    })

    notify.push(`Appointment ${status === 'REJECTED' ? 'rejected' : 'accepted'} successfully.`, 'success')
    await loadPageData()
  } catch (err) {
    notify.push(err?.message || 'Unable to update appointment status.', 'error')
  } finally {
    actionLoadingId.value = ''
  }
}

async function doctorCompleteAppointment(appointment) {
  if (!appointment?.id) return
  actionLoadingId.value = appointment.id

  try {
    await completeAppointment({
      token: auth.token,
      userId: userId.value,
      userEmail: userEmail.value,
      appointmentId: appointment.id,
      role: 'DOCTOR',
    })
    notify.push('Appointment marked as completed.', 'success')
    await loadPageData()
  } catch (err) {
    notify.push(err?.message || 'Unable to complete appointment.', 'error')
  } finally {
    actionLoadingId.value = ''
  }
}

function resumeCheckout(payment) {
  if (!payment?.checkoutUrl) return
  if (payment?.appointmentId) {
    localStorage.setItem('aferent.payment.pendingAppointmentId', payment.appointmentId)
  }
  window.location.assign(payment.checkoutUrl)
}

onMounted(async () => {
  activeTab.value = isDoctor.value ? 'all' : 'upcoming'
  await loadPageData()
})
</script>
