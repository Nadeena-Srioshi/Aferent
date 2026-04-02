<template>
  <Teleport to="body">
    <div
      v-if="show"
      style="position: fixed; inset: 0; background: rgba(0,0,0,.25); display: grid; place-items: center; z-index: 9999;"
      role="status"
      aria-live="polite"
    >
      <div style="background: white; padding: .75rem 1rem; border-radius: .5rem; box-shadow: 0 8px 24px rgba(0,0,0,.12);">
        {{ message || 'Loading...' }}
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
defineProps<{
  show: boolean
  message?: string
}>()
</script>
