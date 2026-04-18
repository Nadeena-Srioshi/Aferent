<template>
  <main class="min-h-screen bg-surface">
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Header -->
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

      <!-- Loading / Error -->
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
        <!-- Sidebar -->
        <aside class="space-y-4">
          <div class="bg-card border border-border rounded-2xl p-5 sticky top-24">
            <h2 class="font-bold text-ink mb-4">Booking details</h2>

            <div class="space-y-4 text-sm">
              <!-- Doctor -->
              <div>
                <p class="text-muted mb-1">Doctor</p>
                <p class="font-semibold text-ink">{{ doctorLabel }}</p>
              </div>

              <!-- Specialization -->
              <div>
                <p class="text-muted mb-1">Specialization</p>
                <p class="font-semibold text-ink">{{ specializationLabel }}</p>
              </div>

              <!-- Status -->
              <div>
                <p class="text-muted mb-1">Status</p>
                <p class="font-semibold text-ink">{{ doctor?.status || '—' }}</p>
              </div>

              <!-- Date picker -->
              <div>
                <div class="flex items-center justify-between gap-2 mb-1">
                  <p class="text-muted">Date</p>
                  <button
                    v-if="selectedDate"
                    type="button"
                    class="text-xs font-semibold text-primary hover:text-action transition-colors"
                    @click="clearSelectedDate"
                  >
                    Clear
                  </button>
                </div>
                <input
                  v-model="selectedDate"
                  type="date"
                  class="w-full px-3 py-2 rounded-xl border border-border bg-surface text-ink"
                />
              </div>

              <!-- Appointment type -->
              <div>
                <div class="flex items-center justify-between gap-2 mb-1">
                  <p class="text-muted">Appointment type</p>
                  <button
                    v-if="selectedType"
                    type="button"
                    class="text-xs font-semibold text-primary hover:text-action transition-colors"
                    @click="clearSelectedType"
                  >
                    Clear
                  </button>
                </div>
                <div class="grid grid-cols-2 gap-2">
                  <button
                    v-for="type in appointmentTypes"
                    :key="type.value"
                    type="button"
                    class="px-3 py-2 rounded-xl border text-sm font-semibold transition-colors"
                    :class="
                      selectedType === type.value
                        ? 'bg-primary text-white border-primary'
                        : 'bg-card text-ink border-border hover:bg-surface'
                    "
                    @click="selectedType = type.value"
                  >
                    {{ type.label }}
                  </button>
                </div>
              </div>

              <!-- View mode toggle — only shown when date is selected -->
              <div v-if="selectedDate">
                <p class="text-muted mb-1">Slot view</p>
                <div class="grid grid-cols-2 gap-2">
                  <button
                    v-for="mode in dateBoundViewModes"
                    :key="mode.value"
                    type="button"
                    class="px-3 py-2 rounded-xl border text-sm font-semibold transition-colors"
                    :class="
                      viewMode === mode.value
                        ? 'bg-primary text-white border-primary'
                        : 'bg-card text-ink border-border hover:bg-surface'
                    "
                    @click="viewMode = mode.value"
                  >
                    {{ mode.label }}
                  </button>
                </div>
              </div>

              <!-- Legend — shown when no date is selected (calendar view) -->
              <div v-if="!selectedDate" class="pt-1">
                <p class="text-muted mb-2">Legend</p>
                <div class="flex flex-col gap-1.5">
                  <div class="flex items-center gap-2">
                    <span class="w-3 h-3 rounded-full bg-blue-500 shrink-0"></span>
                    <span class="text-xs text-ink">Physical</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="w-3 h-3 rounded-full bg-teal-500 shrink-0"></span>
                    <span class="text-xs text-ink">Video</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="w-3 h-3 rounded-full bg-border shrink-0"></span>
                    <span class="text-xs text-muted">Booked</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <!-- Main content -->
        <div class="space-y-6">
          <!-- Slots panel -->
          <div class="bg-card border border-border rounded-2xl p-5">
            <div class="flex items-center justify-between gap-3 mb-4">
              <div>
                <h2 class="text-xl font-bold text-ink">Available slots</h2>
                <p class="text-sm text-muted">{{ slotFilterSummary }}</p>
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

            <template v-else>
              <!-- ── Calendar view (no date selected) ── -->
              <CalendarView
                v-if="!selectedDate"
                :slots="slots"
                :selected-type="selectedType"
                :selected-slot="selectedSlot"
                @select-slot="selectSlot"
              />

              <!-- ── List view (date selected) ── -->
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
                  <div class="flex items-center gap-3">
                    <!-- Type color dot -->
                    <span
                      class="w-2.5 h-2.5 rounded-full shrink-0"
                      :class="slot.type === 'PHYSICAL' ? 'bg-blue-500' : 'bg-teal-500'"
                    ></span>
                    <div>
                      <p class="font-semibold text-ink">
                        {{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}
                      </p>
                      <p class="text-sm text-muted">{{ slotLabel(slot) }}</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-3 shrink-0">
                    <span v-if="slot.consultationFee" class="text-sm font-semibold text-ink">
                      Rs. {{ slot.consultationFee.toLocaleString() }}
                    </span>
                    <span
                      class="text-xs font-semibold px-2.5 py-1 rounded-full"
                      :class="slotBadgeClass(slot)"
                    >
                      {{ slot.booked ? 'Booked' : 'Select' }}
                    </span>
                  </div>
                </button>
              </div>

              <!-- ── Grid view (date selected) ── -->
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
                  <!-- Type indicator stripe -->
                  <div
                    class="w-full h-1 rounded-full mb-3"
                    :class="slot.booked ? 'bg-border' : slot.type === 'PHYSICAL' ? 'bg-blue-500' : 'bg-teal-500'"
                  ></div>
                  <p class="font-semibold text-ink text-sm">
                    {{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}
                  </p>
                  <p class="text-xs text-muted mt-1">{{ slotLabel(slot) }}</p>
                  <div class="mt-3 flex items-center justify-between gap-2">
                    <span
                      class="text-xs font-semibold px-2 py-0.5 inline-flex rounded-full"
                      :class="slotBadgeClass(slot)"
                    >
                      {{ slot.booked ? 'Booked' : 'Available' }}
                    </span>
                    <span v-if="slot.consultationFee" class="text-xs font-semibold text-ink">
                      Rs. {{ slot.consultationFee.toLocaleString() }}
                    </span>
                  </div>
                </button>
              </div>
            </template>
          </div>

          <!-- Selected slot -->
          <div class="bg-card border border-border rounded-2xl p-5">
            <h2 class="text-xl font-bold text-ink mb-4">Selected slot</h2>
            <div v-if="selectedSlot" class="space-y-3">
              <div class="flex flex-wrap gap-x-6 gap-y-1 text-sm">
                <p class="text-muted">
                  <span class="font-semibold text-ink">Date:</span>
                  {{ selectedSlot.date }}
                </p>
                <p class="text-muted">
                  <span class="font-semibold text-ink">Time:</span>
                  {{ formatTime(selectedSlot.startTime) }} – {{ formatTime(selectedSlot.endTime) }}
                </p>
                <p class="text-muted">
                  <span class="font-semibold text-ink">Type:</span>
                  {{ selectedSlot.type }}
                </p>
                <p v-if="selectedSlot.consultationFee" class="text-muted">
                  <span class="font-semibold text-ink">Fee:</span>
                  Rs. {{ selectedSlot.consultationFee.toLocaleString() }}
                </p>
                <p v-if="selectedSlot.hospitalName" class="text-muted">
                  <span class="font-semibold text-ink">Hospital:</span>
                  {{ selectedSlot.hospitalName }}
                  <span v-if="selectedSlot.hospitalLocation">, {{ selectedSlot.hospitalLocation }}</span>
                </p>
              </div>
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
import { getDoctorById, getSpecializations } from '@/services/doctorService'
import CalendarView from '@/components/shared/CalendarView.vue'

// ─── Route / stores ───────────────────────────────────────────────────────────
const route = useRoute()
const router = useRouter()
const auth = useAuth()
const notify = useNotificationStore()

// ─── State ────────────────────────────────────────────────────────────────────
const doctor = ref(null)
const specializations = ref([])
const slots = ref([])
const selectedSlot = ref(null)
const selectedType = ref('')
const selectedDate = ref('')
const loadingDoctor = ref(false)
const loadingSlots = ref(false)
const booking = ref(false)
const error = ref('')

// viewMode only applies when a date IS selected
const viewMode = ref('list')

const appointmentTypes = [
  { label: 'Physical', value: 'PHYSICAL' },
  { label: 'Video', value: 'VIDEO' },
]

const dateBoundViewModes = [
  { label: 'List', value: 'list' },
  { label: 'Grid', value: 'grid' },
]

// ─── Computed ─────────────────────────────────────────────────────────────────
const doctorId = computed(() => {
  const raw = route.params.doctorId
  return typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] : ''
})

const doctorLabel = computed(() => {
  if (!doctor.value) return 'Doctor'
  const name = [doctor.value.firstName, doctor.value.lastName].filter(Boolean).join(' ').trim()
  return name || doctor.value.name || doctor.value.doctorName || doctorId.value || 'Doctor'
})

const specializationLabel = computed(() => {
  const specialization = doctor.value?.specialization
  if (!specialization) return '—'
  const byId = specializations.value.find((s) => s?.id === specialization)
  if (byId?.name) return byId.name
  const byName = specializations.value.find(
    (s) => typeof s?.name === 'string' && s.name.toLowerCase() === String(specialization).toLowerCase(),
  )
  return byName?.name || specialization
})

const slotFilterSummary = computed(() => {
  if (!selectedDate.value && !selectedType.value) return 'Showing all upcoming slots on the calendar'
  const dateText = selectedDate.value || 'Any date'
  const typeText = selectedType.value
    ? selectedType.value.charAt(0) + selectedType.value.slice(1).toLowerCase()
    : 'Any type'
  return `${dateText} · ${typeText}`
})

// ─── Helpers ──────────────────────────────────────────────────────────────────
function normalizeDate(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : ''
}

function normalizeSlot(slot) {
  return { ...slot, booked: Boolean(slot?.booked) }
}

function slotLabel(slot) {
  const pieces = []
  if (slot?.type) pieces.push(slot.type.charAt(0) + slot.type.slice(1).toLowerCase())
  if (slot?.appointmentNumber != null) pieces.push(`#${slot.appointmentNumber}`)
  if (slot?.hospitalName) pieces.push(slot.hospitalName)
  return pieces.join(' · ') || 'Slot'
}

function formatTime(value) {
  if (!value) return '--:--'
  return String(value).slice(0, 5)
}

function slotCardClass(slot) {
  if (slot?.booked) return 'bg-surface border-border opacity-60 cursor-not-allowed'
  if (selectedSlot.value?.slotId === slot?.slotId) return 'bg-primary/10 border-primary ring-2 ring-primary/20'
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

function clearSelectedDate() {
  selectedDate.value = ''
}

function clearSelectedType() {
  selectedType.value = ''
}

// ─── Data fetching ────────────────────────────────────────────────────────────
async function loadDoctor() {
  if (!doctorId.value) throw new Error('Missing doctor id in route')
  loadingDoctor.value = true
  try {
    doctor.value = await getDoctorById(doctorId.value)
  } finally {
    loadingDoctor.value = false
  }
}

async function loadSpecializations() {
  try {
    specializations.value = await getSpecializations()
  } catch {
    specializations.value = []
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
    // Auto-select first available slot
    selectedSlot.value = slots.value.find((s) => !s.booked) ?? null
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

// ─── Booking ──────────────────────────────────────────────────────────────────
async function bookSelectedSlot() {
  if (!selectedSlot.value || selectedSlot.value.booked) return

  if (!auth.isAuthenticated) {
    await router.push({ name: 'register', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }

  if (!auth.isPatient) {
    notify.push('Only patients can book appointments from this page.', 'warning')
    return
  }

  booking.value = true
  try {
    const appointmentType = selectedType.value || selectedSlot.value.type
    const appointmentDate = selectedDate.value || selectedSlot.value.date

    if (!appointmentType || !appointmentDate) {
      notify.push('Please select a valid slot before booking.', 'warning')
      return
    }

    const payload = {
      doctorId: doctorId.value,
      scheduleId: selectedSlot.value.scheduleId,
      type: appointmentType,
      appointmentDate,
      patientName: auth.fullName,
      doctorName: doctorLabel.value,
    }

    if (appointmentType === 'VIDEO' && selectedSlot.value.videoSlotId) {
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

// ─── Watchers ─────────────────────────────────────────────────────────────────
watch(selectedType, () => loadSlots())
watch(selectedDate, () => loadSlots())

// ─── Init ─────────────────────────────────────────────────────────────────────
onMounted(async () => {
  selectedDate.value = normalizeDate(typeof route.query.date === 'string' ? route.query.date : '')
  const routeType = typeof route.query.type === 'string' ? route.query.type.trim().toUpperCase() : ''
  selectedType.value = routeType === 'PHYSICAL' || routeType === 'VIDEO' ? routeType : ''

  try {
    await loadSpecializations()
    await reloadAll()
  } catch (e) {
    error.value = e?.message || 'Unable to load booking page.'
  }
})
</script>