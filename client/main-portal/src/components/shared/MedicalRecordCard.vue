<template>
  <article
    class="bg-card border rounded-2xl p-4 transition-all cursor-pointer"
    :class="selected ? 'border-primary shadow-sm ring-1 ring-primary/20' : 'border-border hover:border-primary/50 hover:shadow-sm'"
    @click="$emit('select')"
  >
    <div class="flex items-start gap-4">
      <div class="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0" :class="iconBg" aria-hidden="true">
        <component :is="icon" class="w-5 h-5" :class="iconColor" />
      </div>

      <div class="flex-1 min-w-0">
        <div class="flex items-start justify-between gap-3 flex-wrap">
          <div class="min-w-0">
            <h3 class="font-semibold text-ink text-sm truncate">{{ title }}</h3>
            <p class="text-xs text-muted mt-0.5 truncate">{{ subtitle }}</p>
          </div>

          <div class="flex items-center gap-2 flex-shrink-0">
            <span class="text-[11px] font-semibold px-2.5 py-1 rounded-full" :class="statusClass">{{ statusLabel }}</span>
            <span v-if="accessCount > 0" class="text-[11px] font-semibold px-2.5 py-1 rounded-full bg-primary/10 text-primary">
              {{ accessCount }} access
            </span>
          </div>
        </div>

        <p v-if="description" class="text-xs text-muted mt-2 leading-relaxed line-clamp-2">{{ description }}</p>

        <div class="mt-3 flex flex-wrap items-center gap-2 text-[11px] text-muted">
          <span v-if="metaLine" class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-surface border border-border">{{ metaLine }}</span>
          <span class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-surface border border-border">Uploaded {{ uploadedAtLabel }}</span>
          <span class="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-surface border border-border">{{ contentType }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { FileText, ScanLine, ShieldCheck, FileUp, Stethoscope } from 'lucide-vue-next'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, required: true },
  description: { type: String, default: '' },
  statusLabel: { type: String, default: 'Uploaded' },
  metaLine: { type: String, default: '' },
  uploadedAtLabel: { type: String, default: '—' },
  contentType: { type: String, default: '—' },
  accessCount: { type: Number, default: 0 },
  selected: { type: Boolean, default: false },
  kind: { type: String, default: 'MEDICAL_REPORT' },
})

defineEmits(['select'])

const icon = computed(() => {
  if (props.kind === 'IMAGING') return ScanLine
  if (props.kind === 'SCAN') return ScanLine
  if (props.kind === 'PRESCRIPTION') return ShieldCheck
  if (props.kind === 'REFERRAL') return FileUp
  if (props.kind === 'DISCHARGE_SUMMARY') return Stethoscope
  if (props.kind === 'QR_CODE') return FileUp
  if (props.kind === 'LAB_REPORT') return FileText
  return FileText
})

const iconBg = computed(() => {
  if (props.kind === 'PRESCRIPTION') return 'bg-warning/10'
  if (props.kind === 'IMAGING' || props.kind === 'SCAN') return 'bg-ai/10'
  if (props.kind === 'DISCHARGE_SUMMARY') return 'bg-success/10'
  if (props.kind === 'QR_CODE') return 'bg-primary/10'
  return 'bg-success/10'
})

const iconColor = computed(() => {
  if (props.kind === 'PRESCRIPTION') return 'text-warning'
  if (props.kind === 'IMAGING' || props.kind === 'SCAN') return 'text-ai'
  if (props.kind === 'DISCHARGE_SUMMARY') return 'text-success'
  if (props.kind === 'QR_CODE') return 'text-primary'
  return 'text-success'
})

const statusClass = computed(() => {
  if (props.kind === 'PRESCRIPTION') return 'bg-warning/10 text-warning'
  if (props.kind === 'SCAN') return 'bg-ai/10 text-ai'
  if (props.kind === 'QR_CODE') return 'bg-primary/10 text-primary'
  return 'bg-success/10 text-success'
})
</script>
