<template>
  <article class="bg-card border border-border rounded-2xl p-5 flex items-start gap-4 hover:shadow-sm transition-shadow">
    <div class="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0" :class="iconBg" aria-hidden="true">
      <component :is="icon" class="w-5 h-5" :class="iconColor" />
    </div>

    <div class="flex-1 min-w-0">
      <div class="flex items-start justify-between gap-2 flex-wrap">
        <div>
          <h3 class="font-semibold text-ink text-sm">{{ title }}</h3>
          <p class="text-xs text-muted mt-0.5">{{ subtitle }}</p>
        </div>
        <span class="text-xs font-semibold px-2.5 py-1 rounded-full flex-shrink-0" :class="statusClass">
          {{ statusLabel }}
        </span>
      </div>

      <p v-if="description" class="text-xs text-muted mt-2 leading-relaxed">{{ description }}</p>

      <div v-if="metaLine" class="mt-2 text-[11px] text-muted">{{ metaLine }}</div>

      <div v-if="symptoms?.length" class="mt-3">
        <p class="text-[11px] font-semibold text-muted uppercase tracking-wide mb-2">Symptoms</p>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="symptom in symptoms"
            :key="symptom"
            class="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] bg-surface border border-border text-ink"
          >
            {{ symptom }}
          </span>
        </div>
      </div>

      <div v-if="medications?.length" class="mt-3">
        <p class="text-[11px] font-semibold text-muted uppercase tracking-wide mb-2">Prescription</p>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="medication in medications"
            :key="medication"
            class="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] bg-warning/10 border border-warning/20 text-warning"
          >
            {{ medication }}
          </span>
        </div>
      </div>

      <div v-if="followUpLine" class="mt-2 text-[11px] text-muted">{{ followUpLine }}</div>
    </div>

    <button
      v-if="actionLabel"
      class="flex-shrink-0 inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold border border-border text-ink hover:text-primary hover:border-primary transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
      :disabled="actionLoading"
      :aria-label="actionAriaLabel || actionLabel"
      @click="$emit('action')"
    >
      <component :is="actionIcon" class="w-3.5 h-3.5" aria-hidden="true" />
      <span>{{ actionLoading ? actionLoadingLabel : actionLabel }}</span>
    </button>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { HeartPulse, ReceiptText, FileDown } from 'lucide-vue-next'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, required: true },
  description: { type: String, default: '' },
  statusLabel: { type: String, default: 'Recorded' },
  metaLine: { type: String, default: '' },
  prescriptionId: { type: String, default: '' },
  medications: { type: Array, default: () => [] },
  symptoms: { type: Array, default: () => [] },
  followUpLine: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  actionAriaLabel: { type: String, default: '' },
  actionLoading: { type: Boolean, default: false },
  actionLoadingLabel: { type: String, default: 'Loading…' },
})

defineEmits(['action'])

const icon = computed(() => (props.prescriptionId ? ReceiptText : HeartPulse))
const actionIcon = computed(() => FileDown)

const iconBg = computed(() => (props.prescriptionId ? 'bg-warning/10' : 'bg-primary/10'))
const iconColor = computed(() => (props.prescriptionId ? 'text-warning' : 'text-primary'))

const statusClass = computed(() => {
  if (props.prescriptionId) return 'bg-warning/10 text-warning'
  return 'bg-success/10 text-success'
})
</script>
