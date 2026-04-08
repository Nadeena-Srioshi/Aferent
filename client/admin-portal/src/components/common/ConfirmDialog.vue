<template>
  <teleport to="body">
    <div v-if="isOpen" class="modal-backdrop" @click.self="confirmData.onCancel?.()">
      <div class="modal max-w-sm">
        <div class="modal-header">
          <h3 class="modal-title">{{ confirmData.title }}</h3>
        </div>
        <p style="color: var(--text-secondary)">{{ confirmData.message }}</p>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="confirmData.onCancel?.()">
            {{ confirmData.cancelText }}
          </button>
          <button
            class="btn"
            :class="confirmData.type === 'danger' ? 'btn-danger' : 'btn-primary'"
            @click="confirmData.onConfirm?.()"
          >
            {{ confirmData.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { useConfirm } from '@/composables/useConfirm'

const { isOpen, confirmData } = useConfirm()
</script>
