<template>
  <main class="min-h-screen bg-surface">
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="flex items-center justify-between gap-4 mb-6">
        <div>
          <p class="text-sm text-muted mb-1">Book an appointment</p>
          <h1 class="text-3xl font-bold text-ink">{{ doctorLabel }}</h1>
        </div>

        <RouterLink
          to="/find-doctor"
          class="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-border bg-card text-ink font-semibold hover:bg-surface transition-colors"
        >
          <ArrowLeft class="w-4 h-4" />
          Back to search
        </RouterLink>
      </div>

      <div v-if="loadingDoctor || loadingSlots" class="bg-card border border-border rounded-2xl p-6 mb-6">
        <p class="text-muted">Loading booking details…</p>
      </div>

      <div v-else-if="error" class="bg-card border border-border rounded-2xl p-6 mb-6">
        <p class="text-danger font-semibold mb-2">{{ error }}</p>
        <button
          type="button"
          class="px-4 py-2 rounded-xl bg-primary text-white font-semibold hover:bg-action transition-colors"
          @click="reloadAll"
        >
          Try again
        </button>
      </div>

      <div v-else class="grid grid-cols-1 xl:grid-cols-[320px_minmax(0,1fr)] gap-6">
        <aside class="space-y-4">
          <div class="bg-card border border-border rounded-2xl p-5 sticky top-24">
            <h2 class="font-bold text-ink mb-4">Booking details</h2>

            <div class="space-y-4 text-sm">
              <div>
                <p class="text-muted mb-1">Doctor</p>
                <p class="font-semibold text-ink">{{ doctorLabel }}</p>
              </div>
              <div>
                <p class="text-muted mb-1">Specialization</p>
                <p class="font-semibold text-ink">{{ doctor?.specialization || '—' }}</p>
              </div>
              <div>
                <p class="text-muted mb-1">Status</p>
                <p class="font-semibold text-ink">{{ doctor?.status || '—' }}</p>
              </div>
              <div>
                <p class="text-muted mb-1">Date</p>
                <input
                  v-model="selectedDate"
                  type="date"
                  class="w-full px-3 py-2 rounded-xl border border-border bg-surface text-ink"
                >
              </div>
              <div>
                <p class="text-muted mb-1">Appointment type</p>
                <div class="grid grid-cols-2 gap-2">
                  <button
                    v-for="type in appointmentTypes"
                    :key="type.value"
                    type="button"
                    class="px-3 py-2 rounded-xl border text-sm font-semibold transition-colors"
                    :class="selectedType === type.value ? 'bg-primary text-white border-primary' : 'bg-card text-ink border-border hover:bg-surface'"
                    @click="selectedType = type.value"
                  >
                    {{ type.label }}
                  </button>
                </div>
              </div>
              <div>
                <p class="text-muted mb-1">Slot view</p>
                <div class="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    class="px-3 py-2 rounded-xl border text-sm font-semibold transition-colors"
                    :class="viewMode === 'list' ? 'bg-primary text-white border-primary' : 'bg-card text-ink border-border hover:bg-surface'"
                    @click="viewMode = 'list'"
                  >
                    List
                  </button>
                  <button
                    type="button"
                    class="px-3 py-2 rounded-xl border text-sm font-semibold transition-colors"
                    :class="viewMode === 'grid' ? 'bg-primary text-white border-primary' : 'bg-card text-ink border-border hover:bg-surface'"
                    @click="viewMode = 'grid'"
                  >
                    Calendar
                  </button>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <div class="space-y-6">
          <div class="bg-card border border-border rounded-2xl p-5">
            <div class="flex items-center justify-between gap-3 mb-4">
              <div>
                <h2 class="text-xl font-bold text-ink">Available slots</h2>
                <p class="text-sm text-muted">Choose a slot for {{ selectedDate }} · {{ selectedType }}</p>
              </div>
              <button
                type="button"
                class="px-4 py-2 rounded-xl border border-border bg-surface text-sm font-semibold text-ink hover:bg-card transition-colors"
                @click="loadSlots"
              >
                Refresh
              </button>
            </div>

            <div v-if="!slots.length" class="rounded-2xl border border-dashed border-border p-8 text-center">
              <p class="text-muted">No slots found for the selected filters.</p>
            </div>

            <div v-else-if="viewMode === 'list'" class="space-y-3">
              <button
                v-for="slot in slots"
                :key="slot.slotId"
                type="button"
                class="w-full flex items-center justify-between gap-4 px-4 py-4 rounded-2xl border text-left transition-colors"
                :class="slotCardClass(slot)"
                :disabled="Boolean(slot.booked)"
                @click="selectSlot(slot)"
              >
                <div>
                  <p class="font-semibold text-ink">{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</p>
                  <p class="text-sm text-muted">{{ slotLabel(slot) }}</p>
                </div>
                <span class="text-xs font-semibold px-2.5 py-1 rounded-full" :class="slotBadgeClass(slot)">
                  {{ slot.booked ? 'Booked' : 'Select' }}
                </span>
              </button>
            </div>

            <div v-else class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              <button
                v-for="slot in slots"
                :key="slot.slotId"
                type="button"
                class="min-h-28 rounded-2xl border p-4 text-left transition-colors"
                :class="slotCardClass(slot)"
                :disabled="Boolean(slot.booked)"
                @click="selectSlot(slot)"
              >
                <p class="font-semibold text-ink">{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</p>
                <p class="text-sm text-muted mt-1">{{ slotLabel(slot) }}</p>
                <p class="mt-3 text-xs font-semibold px-2.5 py-1 inline-flex rounded-full" :class="slotBadgeClass(slot)">
                  {{ slot.booked ? 'Booked' : 'Available' }}
                </p>
              </button>
            </div>
          </div>

          <div class="bg-card border border-border rounded-2xl p-5">
            <h2 class="text-xl font-bold text-ink mb-4">Selected slot</h2>
            <div v-if="selectedSlot" class="space-y-3">
              <p class="text-muted">{{ selectedSlot.date }} · {{ formatTime(selectedSlot.startTime) }} - {{ formatTime(selectedSlot.endTime) }}</p>
              <p class="text-sm text-muted">{{ slotLabel(selectedSlot) }}</p>
              <button
                type="button"
                class="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-primary text-white font-semibold hover:bg-action transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="booking || !selectedSlot || selectedSlot.booked"
                @click="bookSelectedSlot"
              >
                <CalendarCheck2 class="w-4 h-4" />
                {{ booking ? 'Booking…' : 'Book appointment' }}
              </button>
            </div>
            <p v-else class="text-muted">Pick a slot above to continue.</p>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { ArrowLeft, CalendarCheck2 } from 'lucide-vue-next'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import { bookAppointment, getDoctorAvailableSlots } from '@/services/appointmentService'
import { getDoctorById } from '@/services/doctorService'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const notify = useNotificationStore()

const doctor = ref(null)
const slots = ref([])
const selectedSlot = ref(null)
const selectedType = ref('PHYSICAL')
const selectedDate = ref('')
const loadingDoctor = ref(false)
const loadingSlots = ref(false)
const booking = ref(false)
const error = ref('')
const appointmentTypes = [
  { label: 'Physical', value: 'PHYSICAL' },
  { label: 'Video', value: 'VIDEO' },
]

const doctorId = computed(() => {
  const raw = route.params.doctorId
  return typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
})

const doctorLabel = computed(() => {
  if (!doctor.value) return 'Doctor'
  const name = [doctor.value.firstName, doctor.value.lastName].filter(Boolean).join(' ').trim()
  return name || doctor.value.name || doctor.value.doctorName || doctorId.value || 'Doctor'
})

function todayValue() {
  return new Date().toISOString().slice(0, 10)
}

function normalizeDate(value) {
  if (typeof value === 'string' && value.trim()) return value.trim()
  return todayValue()
}

function normalizeSlot(slot) {
  return {
    ...slot,
    booked: Boolean(slot?.booked),
  }
}

function slotLabel(slot) {
  const pieces = []
  if (slot?.type) pieces.push(slot.type)
  if (slot?.appointmentNumber != null) pieces.push(`#${slot.appointmentNumber}`)
  if (slot?.hospitalName) pieces.push(slot.hospitalName)
  return pieces.join(' · ') || 'Slot'
}

function formatTime(value) {
  if (!value) return '--:--'
  return String(value).slice(0, 5)
}

function slotCardClass(slot) {
  if (slot?.booked) {
    return 'bg-surface border-border opacity-60 cursor-not-allowed'
  }
  if (selectedSlot.value?.slotId === slot?.slotId) {
    return 'bg-primary/10 border-primary ring-2 ring-primary/20'
  }
  return 'bg-card border-border hover:bg-surface hover:border-primary/30'
}

function slotBadgeClass(slot) {
  if (slot?.booked) return 'bg-warning/15 text-warning'
  if (selectedSlot.value?.slotId === slot?.slotId) return 'bg-primary text-white'
  return 'bg-success/10 text-success'
}

function selectSlot(slot) {
  if (slot?.booked) return
  selectedSlot.value = slot
}

async function loadDoctor() {
  if (!doctorId.value) {
    throw new Error('Missing doctor id in route')
  }

  loadingDoctor.value = true
  try {
    doctor.value = await getDoctorById(doctorId.value)
  } finally {
    loadingDoctor.value = false
  }
}

async function loadSlots() {
  if (!doctorId.value) return

  loadingSlots.value = true
  error.value = ''
  try {
    const result = await getDoctorAvailableSlots({
      doctorId: doctorId.value,
      type: selectedType.value || undefined,
      date: selectedDate.value || undefined,
    })
    slots.value = Array.isArray(result) ? result.map(normalizeSlot) : []
    selectedSlot.value = slots.value.find((slot) => !slot.booked) || null
  } catch (e) {
    slots.value = []
    selectedSlot.value = null
    error.value = e?.message || 'Unable to load slots.'
  } finally {
    loadingSlots.value = false
  }
}

async function reloadAll() {
  await loadDoctor()
  await loadSlots()
}

async function bookSelectedSlot() {
  if (!selectedSlot.value || selectedSlot.value.booked) return

  if (!auth.isAuthenticated) {
    const currentUrl = router.currentRoute.value.fullPath
    await router.push({ name: 'register', query: { redirect: currentUrl } })
    return
  }

  if (!auth.isPatient) {
    notify.push('Only patients can book appointments from this page.', 'warning')
    return
  }

  booking.value = true
  try {
    const payload = {
      doctorId: doctorId.value,
      scheduleId: selectedSlot.value.scheduleId,
      type: selectedType.value,
      appointmentDate: selectedDate.value,
      patientName: auth.fullName,
      doctorName: doctorLabel.value,
    }

    if (selectedType.value === 'VIDEO' && selectedSlot.value.videoSlotId) {
      payload.videoSlotId = selectedSlot.value.videoSlotId
    }

    await bookAppointment({
      token: auth.token,
      userId: auth.user?.authId,
      userEmail: auth.user?.email,
      role: 'PATIENT',
      payload,
    })

    notify.push('Appointment booked successfully.', 'success')
    await router.push({ name: 'appointments' })
  } catch (e) {
    notify.push(e?.message || 'Booking failed.', 'error')
  } finally {
    booking.value = false
  }
}

watch(selectedType, () => {
  loadSlots()
})

watch(selectedDate, () => {
  loadSlots()
})

onMounted(async () => {
  selectedDate.value = normalizeDate(typeof route.query.date === 'string' ? route.query.date : '')
  selectedType.value = typeof route.query.type === 'string' && route.query.type.trim()
    ? route.query.type.trim().toUpperCase()
    : 'PHYSICAL'

  try {
    await reloadAll()
  } catch (e) {
    error.value = e?.message || 'Unable to load booking page.'
  }
})
</script>
