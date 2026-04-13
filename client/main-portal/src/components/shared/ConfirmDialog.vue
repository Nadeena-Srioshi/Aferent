<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[400] flex items-end sm:items-center justify-center px-4 pb-4 sm:pb-0"
        role="dialog"
        :aria-modal="true"
        :aria-labelledby="titleId"
        :aria-describedby="descId"
        @keydown.esc="$emit('update:modelValue', false)"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-ink/40"
          aria-hidden="true"
          @click="$emit('update:modelValue', false)"
        ></div>

        <!-- Panel -->
        <div class="relative bg-card rounded-2xl border border-border shadow-xl p-6 w-full max-w-md z-10">
          <!-- Icon -->
          <div
            class="w-12 h-12 rounded-2xl flex items-center justify-center mb-4"
            :class="variant === 'danger' ? 'bg-danger/10' : 'bg-warning/10'"
            aria-hidden="true"
          >
            <svg class="w-6 h-6" :class="variant === 'danger' ? 'text-danger' : 'text-warning'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
            </svg>
          </div>

          <h2 :id="titleId" class="text-lg font-bold text-ink mb-2">{{ title }}</h2>
          <p :id="descId" class="text-sm text-muted leading-relaxed mb-6">{{ description }}</p>

          <div class="flex flex-col-reverse sm:flex-row gap-3">
            <button
              class="flex-1 py-2.5 border-2 border-border text-ink font-semibold rounded-xl hover:border-primary hover:text-primary transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
              @click="$emit('update:modelValue', false)"
            >
              {{ cancelLabel }}
            </button>
            <button
              class="flex-1 py-2.5 font-semibold rounded-xl transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2"
              :class="variant === 'danger'
                ? 'bg-danger text-white hover:opacity-90 focus-visible:ring-danger'
                : 'bg-warning text-white hover:opacity-90 focus-visible:ring-warning'"
              @click="$emit('confirm')"
            >
              {{ confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue:   { type: Boolean, default: false },
  title:        { type: String,  default: 'Are you sure?' },
  description:  { type: String,  default: 'This action cannot be undone.' },
  confirmLabel: { type: String,  default: 'Confirm' },
  cancelLabel:  { type: String,  default: 'Cancel' },
  variant:      { type: String,  default: 'danger', validator: v => ['danger', 'warning'].includes(v) },
})

defineEmits(['update:modelValue', 'confirm'])

const titleId = computed(() => `dialog-title-${Math.random().toString(36).slice(2, 7)}`)
const descId  = computed(() => `dialog-desc-${Math.random().toString(36).slice(2, 7)}`)
</script>