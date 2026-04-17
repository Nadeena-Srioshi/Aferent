<template>
  <span
    class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wide"
    :class="styles.badge"
  >
    <component :is="icon" :size="13" :stroke-width="2.5" />
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { ShieldCheck, ShieldAlert, ShieldOff } from 'lucide-vue-next'

const props = defineProps({
  tier: { type: String, required: true },
})

const map = {
  high:   { label: 'High Confidence',   icon: ShieldCheck, badge: 'bg-green-100 text-green-700'   },
  medium: { label: 'Medium Confidence', icon: ShieldAlert, badge: 'bg-yellow-100 text-yellow-700' },
  low:    { label: 'Low Confidence',    icon: ShieldOff,   badge: 'bg-red-100 text-red-700'       },
}

const styles = computed(() => map[props.tier] ?? map.low)
const label  = computed(() => styles.value.label)
const icon   = computed(() => styles.value.icon)
</script>