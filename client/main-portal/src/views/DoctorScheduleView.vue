<template>
  <main class="min-h-screen bg-surface">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 py-8 sm:py-10 space-y-6">
      <section class="rounded-3xl border border-border bg-card p-5 sm:p-7 shadow-sm">
        <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary mb-1">Doctor Workspace</p>
            <h1 class="text-2xl sm:text-3xl font-semibold text-ink">Availability & Overrides</h1>
            <p class="text-sm text-muted mt-1">Manage your weekly slots and add date-specific changes.</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <RouterLink
              to="/doctor/dashboard"
              class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-border bg-surface text-sm font-semibold text-ink hover:border-primary/40 hover:text-primary transition-colors"
            >
              <ArrowLeft class="w-4 h-4" />
              Dashboard
            </RouterLink>
            <RouterLink 
              to="/doctor/profile"
              class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-border bg-surface text-sm font-semibold text-ink hover:border-primary/40 hover:text-primary transition-colors"
            >
              <UserRound class="w-4 h-4" />
              Profile
            </RouterLink>
          </div>
        </div>
      </section>

      <section v-if="loading" class="rounded-3xl border border-border bg-card p-8 text-center">
        <div class="w-10 h-10 border-3 border-primary/20 border-t-primary rounded-full animate-spin mx-auto"></div>
        <p class="text-sm text-muted mt-3">Loading schedule management…</p>
      </section>

      <section v-else class="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <article class="xl:col-span-8 rounded-3xl border border-border bg-card p-5 sm:p-6 shadow-sm space-y-5">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="text-xl font-semibold text-ink">Weekly Availability</h2>
              <p class="text-sm text-muted mt-1">Set recurring slots for each weekday.</p>
            </div>
            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                @click="resetWeeklyDraft"
                class="px-4 py-2.5 rounded-xl border border-border text-sm font-semibold text-muted hover:bg-surface transition-colors"
              >
                Reset
              </button>
              <button
                type="button"
                @click="clearWeeklySchedule"
                :disabled="weeklySaving"
                class="px-4 py-2.5 rounded-xl border border-danger/30 text-sm font-semibold text-danger hover:bg-danger/5 transition-colors disabled:opacity-60"
              >
                Clear All
              </button>
              <button
                type="button"
                @click="saveWeeklySchedule"
                :disabled="weeklySaving"
                class="px-4 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold hover:bg-action transition-colors disabled:opacity-60"
              >
                <span v-if="weeklySaving" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                {{ weeklySaving ? 'Saving…' : 'Save Weekly Schedule' }}
              </button>
            </div>
          </div>

          <div v-if="weeklyError" class="px-4 py-3 rounded-xl bg-red-50 border border-red-100 text-xs text-danger">{{ weeklyError }}</div>
          <div v-if="weeklySuccess" class="px-4 py-3 rounded-xl bg-green-50 border border-green-100 text-xs text-success">Weekly schedule saved.</div>

          <div class="space-y-4">
            <div v-for="day in DAY_KEYS" :key="day.key" class="rounded-2xl border border-border p-4 bg-surface/60">
              <div class="flex items-center justify-between mb-3">
                <h3 class="text-sm font-semibold text-ink">{{ day.label }}</h3>
                <span class="text-xs text-muted">Add a session, then save weekly schedule</span>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-12 gap-2 rounded-xl border border-border bg-card p-2 mb-3">
                <input
                  v-model="weeklySlotDraft[day.key].startTime"
                  type="time"
                  class="sm:col-span-2 px-2.5 py-2 rounded-lg border border-border text-sm"
                />
                <input
                  v-model="weeklySlotDraft[day.key].endTime"
                  type="time"
                  class="sm:col-span-2 px-2.5 py-2 rounded-lg border border-border text-sm"
                />
                <select
                  v-model="weeklySlotDraft[day.key].type"
                  class="sm:col-span-3 px-2.5 py-2 rounded-lg border border-border text-sm bg-white"
                >
                  <option value="VIDEO">Video</option>
                  <option value="IN_PERSON">In person</option>
                </select>
                <select
                  v-model="weeklySlotDraft[day.key].hospital"
                  :disabled="weeklySlotDraft[day.key].type !== 'IN_PERSON'"
                  class="sm:col-span-4 px-2.5 py-2 rounded-lg border border-border text-sm bg-white disabled:opacity-60"
                >
                  <option value="">Select hospital</option>
                  <option v-for="hospital in hospitals" :key="hospital.id" :value="hospital.id">
                    {{ hospital.name }}<span v-if="hospital.city"> — {{ hospital.city }}</span>
                  </option>
                </select>
                <button
                  type="button"
                  @click="addWeeklySlot(day.key, day.label)"
                  class="sm:col-span-1 px-2.5 py-2 rounded-lg text-success hover:bg-green-50 text-sm font-semibold"
                  title="Add session"
                >
                  <Check class="w-4 h-4 mx-auto" />
                </button>
              </div>

              <div v-if="weeklyDraft[day.key].length === 0" class="text-xs text-muted">No sessions configured.</div>

              <div v-else class="space-y-2">
                <div
                  v-for="(slot, index) in weeklyDraft[day.key]"
                  :key="slot.localId"
                  class="grid grid-cols-1 sm:grid-cols-12 gap-2 rounded-xl border border-border bg-card p-2"
                >
                  <div class="sm:col-span-2 px-2.5 py-2 rounded-lg border border-border text-sm bg-surface font-medium text-ink">{{ slot.startTime || '--:--' }}</div>
                  <div class="sm:col-span-2 px-2.5 py-2 rounded-lg border border-border text-sm bg-surface font-medium text-ink">{{ slot.endTime || '--:--' }}</div>
                  <div class="sm:col-span-3 px-2.5 py-2 rounded-lg border border-border text-sm bg-surface font-medium text-ink">{{ slot.type === 'IN_PERSON' ? 'In person' : 'Video' }}</div>
                  <div class="sm:col-span-4 px-2.5 py-2 rounded-lg border border-border text-sm bg-surface text-muted truncate">
                    {{ slot.type === 'IN_PERSON' ? (hospitalNameById(slot.hospital) || 'No hospital') : 'Online session' }}
                  </div>
                  <button
                    type="button"
                    @click="removeWeeklySlot(day.key, index)"
                    class="sm:col-span-1 px-2.5 py-2 rounded-lg text-danger hover:bg-red-50 text-sm font-semibold"
                    title="Remove slot"
                  >
                    <Trash2 class="w-4 h-4 mx-auto" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </article>

        <aside class="xl:col-span-4 space-y-5">
          <article class="rounded-3xl border border-border bg-card p-5 sm:p-6 shadow-sm space-y-4">
            <div>
              <h2 class="text-xl font-semibold text-ink">Date Overrides</h2>
              <p class="text-sm text-muted mt-1">Block a date, add extra slots, or cancel a specific recurring session.</p>
            </div>

            <div v-if="overrideError" class="px-4 py-3 rounded-xl bg-red-50 border border-red-100 text-xs text-danger">{{ overrideError }}</div>

            <div class="space-y-3">
              <div>
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Date</label>
                <input v-model="overrideDraft.date" type="date" class="w-full px-3 py-2.5 rounded-xl border border-border text-sm" />
              </div>

              <div>
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Action</label>
                <select v-model="overrideDraft.action" class="w-full px-3 py-2.5 rounded-xl border border-border text-sm bg-white">
                  <option value="BLOCK">Block date</option>
                  <option value="ADD">Add extra slots</option>
                  <option value="CANCEL_SESSION">Cancel one recurring session</option>
                </select>
              </div>

              <div v-if="overrideDraft.action === 'CANCEL_SESSION'">
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Session</label>
                <select v-model="overrideDraft.sessionId" class="w-full px-3 py-2.5 rounded-xl border border-border text-sm bg-white">
                  <option value="">Select session</option>
                  <option v-for="session in cancelableSessions" :key="session.sessionId" :value="session.sessionId">
                    {{ session.startTime }} - {{ session.endTime }} ({{ session.type }})
                  </option>
                </select>
              </div>

              <div v-if="overrideDraft.action === 'ADD'" class="space-y-2">
                <div class="flex items-center justify-between">
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted">Extra Slots</label>
                  <button type="button" @click="addOverrideSlot" class="text-xs font-semibold text-primary hover:underline">+ Add slot</button>
                </div>

                <div v-if="overrideDraft.slots.length === 0" class="text-xs text-muted">No override slots added.</div>

                <div v-for="(slot, index) in overrideDraft.slots" :key="slot.localId" class="rounded-xl border border-border bg-surface p-2 space-y-2">
                  <div class="grid grid-cols-2 gap-2">
                    <input v-model="slot.startTime" type="time" class="px-2.5 py-2 rounded-lg border border-border text-sm" />
                    <input v-model="slot.endTime" type="time" class="px-2.5 py-2 rounded-lg border border-border text-sm" />
                  </div>
                  <div class="grid grid-cols-1 gap-2">
                    <select v-model="slot.type" class="px-2.5 py-2 rounded-lg border border-border text-sm bg-white">
                      <option value="VIDEO">Video</option>
                      <option value="IN_PERSON">In person</option>
                    </select>
                    <select v-model="slot.hospital" :disabled="slot.type !== 'IN_PERSON'" class="px-2.5 py-2 rounded-lg border border-border text-sm bg-white disabled:opacity-60">
                      <option value="">Select hospital</option>
                      <option v-for="hospital in hospitals" :key="hospital.id" :value="hospital.id">
                        {{ hospital.name }}<span v-if="hospital.city"> — {{ hospital.city }}</span>
                      </option>
                    </select>
                  </div>
                  <button type="button" @click="removeOverrideSlot(index)" class="text-xs font-semibold text-danger hover:underline">Remove</button>
                </div>
              </div>

              <div>
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Reason (optional)</label>
                <input v-model="overrideDraft.reason" type="text" placeholder="Holiday, conference, emergency" class="w-full px-3 py-2.5 rounded-xl border border-border text-sm" />
              </div>

              <button
                type="button"
                @click="createOverride"
                :disabled="overrideSaving"
                class="w-full px-4 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold hover:bg-action transition-colors disabled:opacity-60"
              >
                <span v-if="overrideSaving" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                {{ overrideSaving ? 'Saving…' : 'Save Override' }}
              </button>
            </div>
          </article>

          <article class="rounded-3xl border border-border bg-card p-5 sm:p-6 shadow-sm">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-lg font-semibold text-ink">Upcoming Overrides</h3>
              <button type="button" @click="loadScheduleData" class="text-xs font-semibold text-primary hover:underline">Refresh</button>
            </div>

            <div v-if="overrides.length === 0" class="text-sm text-muted">No overrides added.</div>

            <ul v-else class="space-y-2 max-h-96 overflow-y-auto pr-1">
              <li v-for="item in sortedOverrides" :key="item.id" class="rounded-xl border border-border bg-surface p-3">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="text-sm font-semibold text-ink">{{ formatDate(item.date) }}</p>
                    <p class="text-xs text-muted mt-0.5">{{ item.action }}</p>
                    <p v-if="item.reason" class="text-xs text-muted mt-1">{{ item.reason }}</p>
                  </div>
                  <button
                    type="button"
                    @click="removeOverride(item.id)"
                    class="text-xs font-semibold text-danger hover:underline"
                  >
                    Delete
                  </button>
                </div>
              </li>
            </ul>
          </article>
        </aside>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowLeft, Check, Trash2, UserRound } from 'lucide-vue-next'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import doctorService from '@/services/doctorService'
import { resolveDoctorIdentity } from '@/services/doctorDashboardService'

const DAY_KEYS = [
  { key: 'monday', label: 'Monday' },
  { key: 'tuesday', label: 'Tuesday' },
  { key: 'wednesday', label: 'Wednesday' },
  { key: 'thursday', label: 'Thursday' },
  { key: 'friday', label: 'Friday' },
  { key: 'saturday', label: 'Saturday' },
  { key: 'sunday', label: 'Sunday' },
]

const auth = useAuth()
const notify = useNotificationStore()

const loading = ref(true)
const weeklySaving = ref(false)
const overrideSaving = ref(false)
const weeklyError = ref('')
const weeklySuccess = ref(false)
const overrideError = ref('')
const doctorId = ref('')
const hospitals = ref([])
const overrides = ref([])

const weeklyDraft = reactive(makeEmptyWeeklySchedule())
const weeklyInitial = ref(makeEmptyWeeklySchedule())
const weeklySlotDraft = reactive(makeWeeklySlotDraft())

const overrideDraft = reactive({
  date: '',
  action: 'BLOCK',
  slots: [],
  sessionId: '',
  reason: '',
})

function makeEmptySlot() {
  return {
    localId: crypto.randomUUID(),
    sessionId: '',
    startTime: '',
    endTime: '',
    type: 'VIDEO',
    hospital: '',
  }
}

function makeEmptyWeeklySchedule() {
  return {
    monday: [],
    tuesday: [],
    wednesday: [],
    thursday: [],
    friday: [],
    saturday: [],
    sunday: [],
  }
}

function makeWeeklySlotDraft() {
  return {
    monday: makeEmptySlot(),
    tuesday: makeEmptySlot(),
    wednesday: makeEmptySlot(),
    thursday: makeEmptySlot(),
    friday: makeEmptySlot(),
    saturday: makeEmptySlot(),
    sunday: makeEmptySlot(),
  }
}

function deepClone(value) {
  return JSON.parse(JSON.stringify(value))
}

function mapSlots(slots) {
  if (!Array.isArray(slots)) return []
  return slots.map((slot) => ({
    localId: crypto.randomUUID(),
    sessionId: slot?.sessionId || '',
    startTime: slot?.startTime || '',
    endTime: slot?.endTime || '',
    type: slot?.type || 'VIDEO',
    hospital: slot?.hospital || '',
  }))
}

function hydrateWeeklyDraft(schedule) {
  for (const day of DAY_KEYS) {
    weeklyDraft[day.key] = mapSlots(schedule?.[day.key])
  }
}

function resetWeeklyDraft() {
  hydrateWeeklyDraft(weeklyInitial.value)
  for (const day of DAY_KEYS) {
    weeklySlotDraft[day.key] = makeEmptySlot()
  }
  weeklyError.value = ''
  weeklySuccess.value = false
}

function addWeeklySlot(dayKey, dayLabel) {
  try {
    const draft = weeklySlotDraft[dayKey]
    const candidate = {
      sessionId: '',
      startTime: draft.startTime,
      endTime: draft.endTime,
      type: draft.type,
      hospital: draft.hospital,
    }
    validateSlotSet([candidate], dayLabel)

    weeklyDraft[dayKey].push({
      localId: crypto.randomUUID(),
      sessionId: '',
      startTime: draft.startTime,
      endTime: draft.endTime,
      type: draft.type,
      hospital: draft.type === 'IN_PERSON' ? draft.hospital : '',
    })
    weeklySlotDraft[dayKey] = makeEmptySlot()
    weeklyError.value = ''
  } catch (error) {
    weeklyError.value = error?.message || 'Invalid slot details.'
  }
}

function removeWeeklySlot(dayKey, index) {
  weeklyDraft[dayKey].splice(index, 1)
}

function hospitalNameById(id) {
  if (!id) return ''
  return hospitals.value.find((item) => item.id === id)?.name || id
}

function addOverrideSlot() {
  overrideDraft.slots.push(makeEmptySlot())
}

function removeOverrideSlot(index) {
  overrideDraft.slots.splice(index, 1)
}

function normalizeSlot(slot) {
  return {
    sessionId: slot.sessionId || undefined,
    startTime: slot.startTime,
    endTime: slot.endTime,
    type: slot.type,
    hospital: slot.type === 'IN_PERSON' ? slot.hospital : null,
  }
}

function isValidTime(value) {
  return /^([01]\d|2[0-3]):[0-5]\d$/.test(value)
}

function toMinutes(value) {
  const [h, m] = value.split(':').map(Number)
  return h * 60 + m
}

function validateSlotSet(slots, scope) {
  for (const slot of slots) {
    if (!isValidTime(slot.startTime) || !isValidTime(slot.endTime)) {
      throw new Error(`${scope}: Use HH:mm time format.`)
    }

    if (toMinutes(slot.startTime) >= toMinutes(slot.endTime)) {
      throw new Error(`${scope}: startTime must be before endTime.`)
    }

    if (!slot.type) {
      throw new Error(`${scope}: Session type is required.`)
    }

    if (slot.type === 'IN_PERSON' && !slot.hospital) {
      throw new Error(`${scope}: In-person slots need a hospital.`)
    }
  }

  const sorted = [...slots].sort((a, b) => toMinutes(a.startTime) - toMinutes(b.startTime))
  for (let i = 1; i < sorted.length; i += 1) {
    const prev = sorted[i - 1]
    const current = sorted[i]
    if (toMinutes(current.startTime) < toMinutes(prev.endTime)) {
      throw new Error(`${scope}: Slots overlap.`)
    }
  }
}

function buildWeeklyPayload() {
  const payload = {}
  for (const day of DAY_KEYS) {
    const daySlots = weeklyDraft[day.key].map(normalizeSlot)
    validateSlotSet(daySlots, day.label)
    payload[day.key] = daySlots
  }
  return payload
}

function resetOverrideDraft() {
  overrideDraft.date = ''
  overrideDraft.action = 'BLOCK'
  overrideDraft.slots = []
  overrideDraft.sessionId = ''
  overrideDraft.reason = ''
}

const selectedDayKey = computed(() => {
  if (!overrideDraft.date) return ''
  const date = new Date(`${overrideDraft.date}T00:00:00`)
  if (Number.isNaN(date.getTime())) return ''
  const map = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
  return map[date.getDay()] || ''
})

const cancelableSessions = computed(() => {
  if (!selectedDayKey.value) return []
  return (weeklyDraft[selectedDayKey.value] || []).filter((slot) => Boolean(slot.sessionId))
})

const sortedOverrides = computed(() => {
  return [...overrides.value].sort((a, b) => String(a.date).localeCompare(String(b.date)))
})

async function resolveDoctorId() {
  if (doctorId.value) return doctorId.value

  if (auth.token && !auth.user) {
    await auth.fetchMe()
  }

  if (!auth.token) {
    throw new Error('Please sign in again.')
  }

  const identity = await resolveDoctorIdentity({
    token: auth.token,
    authId: auth.user?.authId,
    email: auth.user?.email,
  })

  if (!identity?.doctorId) {
    throw new Error('Could not find doctor record. Try again after account verification.')
  }

  doctorId.value = identity.doctorId
  return doctorId.value
}

async function loadScheduleData() {
  loading.value = true
  weeklyError.value = ''
  overrideError.value = ''

  try {
    const id = await resolveDoctorId()

    const [weeklyResult, overridesResult, hospitalsResult] = await Promise.allSettled([
      doctorService.getWeeklySchedule({ doctorId: id }),
      doctorService.getScheduleOverrides({ doctorId: id }),
      doctorService.getMyHospitals({ token: auth.token }),
    ])

    if (hospitalsResult.status === 'fulfilled') {
      hospitals.value = Array.isArray(hospitalsResult.value) ? hospitalsResult.value : []
    }

    if (weeklyResult.status === 'fulfilled') {
      hydrateWeeklyDraft(weeklyResult.value || {})
      weeklyInitial.value = deepClone(weeklyResult.value || makeEmptyWeeklySchedule())
    } else {
      hydrateWeeklyDraft(makeEmptyWeeklySchedule())
      weeklyInitial.value = deepClone(makeEmptyWeeklySchedule())
    }

    if (overridesResult.status === 'fulfilled') {
      overrides.value = Array.isArray(overridesResult.value) ? overridesResult.value : []
    } else {
      overrides.value = []
    }
  } catch (error) {
    weeklyError.value = error?.message || 'Could not load schedule data.'
  } finally {
    loading.value = false
  }
}

async function saveWeeklySchedule() {
  weeklyError.value = ''
  weeklySuccess.value = false
  weeklySaving.value = true

  try {
    const id = await resolveDoctorId()
    const payload = buildWeeklyPayload()
    const saved = await doctorService.setWeeklySchedule({ token: auth.token, doctorId: id, payload })

    hydrateWeeklyDraft(saved || {})
    weeklyInitial.value = deepClone(saved || makeEmptyWeeklySchedule())
    weeklySuccess.value = true
    notify.push('Weekly schedule updated.', 'success')
  } catch (error) {
    weeklyError.value = error?.message || 'Could not save weekly schedule.'
  } finally {
    weeklySaving.value = false
  }
}

async function clearWeeklySchedule() {
  if (!confirm('Delete all weekly slots?')) return

  weeklyError.value = ''
  weeklySuccess.value = false
  weeklySaving.value = true

  try {
    const id = await resolveDoctorId()
    await doctorService.deleteWeeklySchedule({ token: auth.token, doctorId: id })

    hydrateWeeklyDraft(makeEmptyWeeklySchedule())
    weeklyInitial.value = deepClone(makeEmptyWeeklySchedule())
    notify.push('Weekly schedule cleared.', 'success')
  } catch (error) {
    weeklyError.value = error?.message || 'Could not clear weekly schedule.'
  } finally {
    weeklySaving.value = false
  }
}

async function createOverride() {
  overrideError.value = ''
  overrideSaving.value = true

  try {
    const id = await resolveDoctorId()

    if (!overrideDraft.date) {
      throw new Error('Override date is required.')
    }

    const payload = {
      date: overrideDraft.date,
      action: overrideDraft.action,
    }

    if (overrideDraft.reason.trim()) {
      payload.reason = overrideDraft.reason.trim()
    }

    if (overrideDraft.action === 'ADD') {
      if (!overrideDraft.slots.length) {
        throw new Error('Add at least one slot for ADD override.')
      }
      const slots = overrideDraft.slots.map(normalizeSlot)
      validateSlotSet(slots, 'Override slots')
      payload.slots = slots
    }

    if (overrideDraft.action === 'CANCEL_SESSION') {
      if (!overrideDraft.sessionId) {
        throw new Error('Select a session to cancel.')
      }
      payload.sessionId = overrideDraft.sessionId
    }

    const created = await doctorService.addScheduleOverride({
      token: auth.token,
      doctorId: id,
      payload,
    })

    overrides.value = [created, ...overrides.value.filter((item) => item.id !== created.id)]
    resetOverrideDraft()
    notify.push('Override saved.', 'success')
  } catch (error) {
    overrideError.value = error?.message || 'Could not save override.'
  } finally {
    overrideSaving.value = false
  }
}

async function removeOverride(overrideId) {
  if (!confirm('Delete this override?')) return

  try {
    const id = await resolveDoctorId()
    await doctorService.deleteScheduleOverride({ token: auth.token, doctorId: id, overrideId })
    overrides.value = overrides.value.filter((item) => item.id !== overrideId)
    notify.push('Override deleted.', 'success')
  } catch (error) {
    overrideError.value = error?.message || 'Could not delete override.'
  }
}

function formatDate(value) {
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date)
}

onMounted(() => {
  void loadScheduleData()
})
</script>
