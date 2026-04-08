<template>
  <!-- Teleport to body so toasts always sit above modals/overlays -->
  <Teleport to="body">
    <div
      aria-live="polite"
      aria-atomic="false"
      class="fixed bottom-4 right-4 z-[200] flex flex-col gap-3 w-full max-w-sm pointer-events-none"
      role="region"
      aria-label="Notifications"
    >
      <TransitionGroup
        tag="div"
        class="flex flex-col gap-3"
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0 translate-y-4 scale-95"
        enter-to-class="opacity-100 translate-y-0 scale-100"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="opacity-100 translate-y-0 scale-100"
        leave-to-class="opacity-0 translate-y-2 scale-95"
      >
        <div
          v-for="n in notifications"
          :key="n.id"
          class="pointer-events-auto flex items-start gap-3 bg-card border rounded-2xl shadow-lg px-4 py-3.5"
          :class="borderClass(n.type)"
          role="alert"
        >
          <!-- Icon -->
          <div
            class="mt-0.5 w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0"
            :class="iconBg(n.type)"
            aria-hidden="true"
          >
            <svg class="w-4 h-4" :class="iconColor(n.type)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" :d="iconPath(n.type)" />
            </svg>
          </div>

          <!-- Message -->
          <p class="flex-1 text-sm text-ink leading-snug pt-1">{{ n.message }}</p>

          <!-- Dismiss -->
          <button
            class="mt-0.5 p-1 rounded-lg text-muted hover:text-ink hover:bg-surface transition-colors flex-shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            :aria-label="`Dismiss notification: ${n.message}`"
            @click="dismiss(n.id)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useNotificationStore } from '@/stores/notificationStore'

const store = useNotificationStore()
const { notifications } = storeToRefs(store)
const { dismiss } = store

const borderClass = (type) => ({
  success: 'border-success/30',
  error:   'border-danger/30',
  warning: 'border-warning/30',
  info:    'border-primary/20',
}[type] ?? 'border-border')

const iconBg = (type) => ({
  success: 'bg-success/10',
  error:   'bg-danger/10',
  warning: 'bg-warning/10',
  info:    'bg-primary/10',
}[type] ?? 'bg-surface')

const iconColor = (type) => ({
  success: 'text-success',
  error:   'text-danger',
  warning: 'text-warning',
  info:    'text-primary',
}[type] ?? 'text-muted')

const iconPath = (type) => ({
  success: 'M5 13l4 4L19 7',
  error:   'M6 18L18 6M6 6l12 12',
  warning: 'M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z',
  info:    'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
}[type] ?? 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z')
</script>