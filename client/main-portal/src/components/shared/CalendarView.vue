<template>
  <div class="overflow-x-auto">
    <!-- Calendar header: month navigation -->
    <div class="flex items-center justify-between gap-3 mb-4">
      <button
        type="button"
        class="p-2 rounded-xl border border-border bg-surface hover:bg-card transition-colors"
        @click="prevMonth"
      >
        <ChevronLeft class="w-4 h-4 text-ink" />
      </button>
      <h3 class="font-bold text-ink text-base">
        {{ monthLabel }}
      </h3>
      <button
        type="button"
        class="p-2 rounded-xl border border-border bg-surface hover:bg-card transition-colors"
        @click="nextMonth"
      >
        <ChevronRight class="w-4 h-4 text-ink" />
      </button>
    </div>

    <!-- Day-of-week headers -->
    <div class="grid grid-cols-7 mb-1">
      <div
        v-for="day in DAY_LABELS"
        :key="day"
        class="text-center text-xs font-semibold text-muted py-2"
      >
        {{ day }}
      </div>
    </div>

    <!-- Calendar grid -->
    <div class="grid grid-cols-7 gap-px bg-border rounded-2xl overflow-hidden border border-border">
      <!-- Leading empty cells -->
      <div
        v-for="n in leadingBlanks"
        :key="`blank-start-${n}`"
        class="bg-surface min-h-24 p-1"
      ></div>

      <!-- Day cells -->
      <div
        v-for="day in daysInMonth"
        :key="day.dateStr"
        class="bg-card min-h-24 p-1.5 flex flex-col gap-1"
        :class="{ 'bg-surface/60': day.isPast }"
      >
        <!-- Day number -->
        <div class="flex items-center justify-between mb-0.5">
          <span
            class="text-xs font-bold w-6 h-6 flex items-center justify-center rounded-full"
            :class="
              day.isToday
                ? 'bg-primary text-white'
                : day.isPast
                  ? 'text-muted'
                  : 'text-ink'
            "
          >
            {{ day.dayNum }}
          </span>
          <span
            v-if="day.slots.length"
            class="text-[10px] font-semibold text-muted"
          >
            {{ day.availableCount }}/{{ day.slots.length }}
          </span>
        </div>

        <!-- Slot chips -->
        <template v-if="day.slots.length">
          <!-- Show up to MAX_VISIBLE chips -->
          <button
            v-for="slot in day.slots.slice(0, MAX_VISIBLE)"
            :key="slot.slotId"
            type="button"
            class="w-full text-left text-[10px] font-semibold px-1.5 py-0.5 rounded-md truncate leading-4 transition-colors"
            :class="chipClass(slot)"
            :disabled="Boolean(slot.booked)"
            :title="`${formatTime(slot.startTime)} – ${formatTime(slot.endTime)} · ${slot.type} · ${slot.hospitalName || ''}`"
            @click="$emit('select-slot', slot)"
          >
            {{ formatTime(slot.startTime) }}
            <span class="opacity-70">{{ slot.type === 'PHYSICAL' ? '·P' : '·V' }}</span>
          </button>

          <!-- Overflow indicator -->
          <button
            v-if="day.slots.length > MAX_VISIBLE"
            type="button"
            class="w-full text-[10px] font-semibold text-muted hover:text-ink transition-colors text-left px-1.5 py-0.5"
            @click="expandDay(day.dateStr)"
          >
            +{{ day.slots.length - MAX_VISIBLE }} more
          </button>
        </template>
      </div>

      <!-- Trailing empty cells -->
      <div
        v-for="n in trailingBlanks"
        :key="`blank-end-${n}`"
        class="bg-surface min-h-24 p-1"
      ></div>
    </div>

    <!-- Expanded day panel -->
    <transition name="slide-fade">
      <div v-if="expandedDay" class="mt-4 bg-surface border border-border rounded-2xl p-4">
        <div class="flex items-center justify-between gap-3 mb-3">
          <h4 class="font-bold text-ink">{{ expandedDayLabel }}</h4>
          <button
            type="button"
            class="text-xs font-semibold text-muted hover:text-ink transition-colors"
            @click="expandedDay = null"
          >
            Close
          </button>
        </div>
        <div class="space-y-2">
          <button
            v-for="slot in expandedSlots"
            :key="slot.slotId"
            type="button"
            class="w-full flex items-center justify-between gap-3 px-4 py-3 rounded-xl border text-left transition-colors"
            :class="expandedSlotClass(slot)"
            :disabled="Boolean(slot.booked)"
            @click="$emit('select-slot', slot)"
          >
            <div class="flex items-center gap-2.5">
              <span
                class="w-2 h-2 rounded-full shrink-0"
                :class="slot.booked ? 'bg-border' : slot.type === 'PHYSICAL' ? 'bg-blue-500' : 'bg-teal-500'"
              ></span>
              <div>
                <p class="text-sm font-semibold text-ink">
                  {{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}
                </p>
                <p class="text-xs text-muted">{{ expandedSlotLabel(slot) }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2 shrink-0">
              <span v-if="slot.consultationFee" class="text-xs font-semibold text-ink">
                Rs. {{ slot.consultationFee.toLocaleString() }}
              </span>
              <span
                class="text-xs font-semibold px-2 py-0.5 rounded-full"
                :class="
                  slot.booked
                    ? 'bg-warning/15 text-warning'
                    : selectedSlot?.slotId === slot.slotId
                      ? 'bg-primary text-white'
                      : 'bg-success/10 text-success'
                "
              >
                {{ slot.booked ? 'Booked' : selectedSlot?.slotId === slot.slotId ? 'Selected' : 'Select' }}
              </span>
            </div>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

// ─── Props & emits ────────────────────────────────────────────────────────────
const props = defineProps({
  slots: { type: Array, required: true },
  selectedType: { type: String, default: '' },
  selectedSlot: { type: Object, default: null },
})

const emit = defineEmits(['select-slot'])

// ─── Constants ────────────────────────────────────────────────────────────────
const DAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const MAX_VISIBLE = 3

// ─── Calendar navigation state ────────────────────────────────────────────────
const today = new Date()
const viewYear = ref(today.getFullYear())
const viewMonth = ref(today.getMonth()) // 0-indexed

const expandedDay = ref(null)  // 'YYYY-MM-DD' string

// ─── Computed: calendar metadata ──────────────────────────────────────────────
const monthLabel = computed(() => {
  return new Date(viewYear.value, viewMonth.value, 1).toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
  })
})

const daysInMonthCount = computed(() =>
  new Date(viewYear.value, viewMonth.value + 1, 0).getDate(),
)

// Day of week the 1st falls on (0 = Sunday)
const leadingBlanks = computed(() =>
  new Date(viewYear.value, viewMonth.value, 1).getDay(),
)

const trailingBlanks = computed(() => {
  const total = leadingBlanks.value + daysInMonthCount.value
  const remainder = total % 7
  return remainder === 0 ? 0 : 7 - remainder
})

// ─── Slots grouped by date ────────────────────────────────────────────────────
const slotsByDate = computed(() => {
  const map = new Map()
  for (const slot of props.slots) {
    if (!slot.date) continue
    if (!map.has(slot.date)) map.set(slot.date, [])
    map.get(slot.date).push(slot)
  }
  // Sort slots within each day by startTime
  for (const [, list] of map) {
    list.sort((a, b) => (a.startTime > b.startTime ? 1 : -1))
  }
  return map
})

// ─── Day cells data ───────────────────────────────────────────────────────────
const daysInMonth = computed(() => {
  const todayStr = toDateStr(today)
  return Array.from({ length: daysInMonthCount.value }, (_, i) => {
    const dayNum = i + 1
    const dateStr = `${viewYear.value}-${String(viewMonth.value + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`
    const daySlots = slotsByDate.value.get(dateStr) ?? []
    return {
      dayNum,
      dateStr,
      slots: daySlots,
      availableCount: daySlots.filter((s) => !s.booked).length,
      isToday: dateStr === todayStr,
      isPast: dateStr < todayStr,
    }
  })
})

// ─── Expanded day ─────────────────────────────────────────────────────────────
const expandedSlots = computed(() => slotsByDate.value.get(expandedDay.value) ?? [])

const expandedDayLabel = computed(() => {
  if (!expandedDay.value) return ''
  const d = new Date(expandedDay.value + 'T00:00:00')
  return d.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })
})

function expandDay(dateStr) {
  expandedDay.value = expandedDay.value === dateStr ? null : dateStr
}

// ─── Navigation ───────────────────────────────────────────────────────────────
function prevMonth() {
  if (viewMonth.value === 0) {
    viewMonth.value = 11
    viewYear.value--
  } else {
    viewMonth.value--
  }
  expandedDay.value = null
}

function nextMonth() {
  if (viewMonth.value === 11) {
    viewMonth.value = 0
    viewYear.value++
  } else {
    viewMonth.value++
  }
  expandedDay.value = null
}

// ─── Styling helpers ──────────────────────────────────────────────────────────
function chipClass(slot) {
  if (slot.booked) return 'bg-border/40 text-muted cursor-not-allowed'
  if (props.selectedSlot?.slotId === slot.slotId) return 'bg-primary text-white ring-1 ring-primary'
  if (slot.type === 'PHYSICAL') return 'bg-blue-500/15 text-blue-600 hover:bg-blue-500/30'
  return 'bg-teal-500/15 text-teal-600 hover:bg-teal-500/30'
}

function expandedSlotClass(slot) {
  if (slot.booked) return 'bg-surface border-border opacity-60 cursor-not-allowed'
  if (props.selectedSlot?.slotId === slot.slotId) return 'bg-primary/10 border-primary ring-2 ring-primary/20'
  return 'bg-card border-border hover:bg-surface hover:border-primary/30'
}

function expandedSlotLabel(slot) {
  const pieces = []
  if (slot.type) pieces.push(slot.type.charAt(0) + slot.type.slice(1).toLowerCase())
  if (slot.appointmentNumber != null) pieces.push(`#${slot.appointmentNumber}`)
  if (slot.hospitalName) pieces.push(slot.hospitalName)
  if (slot.hospitalLocation) pieces.push(slot.hospitalLocation)
  return pieces.join(' · ')
}

// ─── Utils ────────────────────────────────────────────────────────────────────
function formatTime(value) {
  if (!value) return '--:--'
  return String(value).slice(0, 5)
}

function toDateStr(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>