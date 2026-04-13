<template>
  <div
    class="bg-card border rounded-2xl p-5 transition-all duration-200"
    :class="statusBorder"
  >
    <div class="flex items-start gap-4">
      <!-- Type icon -->
      <div
        class="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0"
        :class="appointment.type === 'video' ? 'bg-ai/10' : 'bg-primary/10'"
        aria-hidden="true"
      >
        <svg class="w-5 h-5" :class="appointment.type === 'video' ? 'text-ai' : 'text-primary'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            :d="appointment.type === 'video'
              ? 'M15 10l4.553-2.069A1 1 0 0121 8.82v6.36a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z'
              : 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-2 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4'"
          />
        </svg>
      </div>

      <!-- Info -->
      <div class="flex-1 min-w-0">
        <div class="flex items-start justify-between gap-2 flex-wrap">
          <div>
            <h3 class="font-bold text-ink text-sm">{{ appointment.doctorName }}</h3>
            <p class="text-xs text-muted">{{ appointment.specialty }}</p>
          </div>
          <span
            class="text-xs font-semibold px-2.5 py-1 rounded-full flex-shrink-0"
            :class="statusChip"
          >
            {{ statusLabel }}
          </span>
        </div>

        <div class="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 text-xs text-muted">
          <span class="flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
            </svg>
            {{ appointment.date }}
          </span>
          <span class="flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            {{ appointment.time }}
          </span>
          <span class="capitalize">{{ appointment.type }} consult</span>
        </div>
      </div>
    </div>

    <!-- Action buttons (shown for upcoming appointments) -->
    <div v-if="showActions" class="flex flex-wrap gap-2 mt-4 pt-4 border-t border-border">
      <slot name="actions">
        <button
          v-if="appointment.type === 'video' && appointment.status === 'confirmed'"
          class="flex-1 py-2 text-xs font-semibold text-white bg-ai rounded-xl hover:opacity-90 transition-opacity focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ai focus-visible:ring-offset-2"
          @click="$emit('join', appointment)"
        >
          Join Video Call
        </button>
        <button
          class="flex-1 py-2 text-xs font-semibold text-muted border border-border rounded-xl hover:border-primary hover:text-primary transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          @click="$emit('reschedule', appointment)"
        >
          Reschedule
        </button>
        <button
          class="flex-1 py-2 text-xs font-semibold text-danger border border-danger/20 rounded-xl hover:bg-danger/5 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-danger"
          @click="$emit('cancel', appointment)"
        >
          Cancel
        </button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  appointment: {
    type: Object,
    required: true,
    // Expected shape: { id, doctorName, specialty, date, time, type, status }
  },
  showActions: { type: Boolean, default: true },
})

defineEmits(['join', 'reschedule', 'cancel'])

const statusMap = {
  confirmed:  { label: 'Confirmed',  chip: 'bg-success/10 text-success',  border: 'border-success/20' },
  pending:    { label: 'Pending',    chip: 'bg-warning/10 text-warning',  border: 'border-warning/20' },
  completed:  { label: 'Completed',  chip: 'bg-muted/10 text-muted',      border: 'border-border' },
  cancelled:  { label: 'Cancelled',  chip: 'bg-danger/10 text-danger',    border: 'border-danger/20' },
}

const current     = computed(() => statusMap[props.appointment.status] ?? statusMap.pending)
const statusLabel = computed(() => current.value.label)
const statusChip  = computed(() => current.value.chip)
const statusBorder = computed(() => current.value.border)
</script>