<template>
  <section class="bg-card border border-border rounded-2xl p-5 sticky top-6">
    <div class="flex items-start justify-between gap-3 mb-4">
      <div>
        <h3 class="text-lg font-bold text-ink">Access to this record</h3>
        <p class="text-xs text-muted">Doctors with currently active access</p>
      </div>
      <span class="text-xs font-semibold px-2.5 py-1 rounded-full bg-primary/10 text-primary">
        {{ grants.length }} grant{{ grants.length === 1 ? '' : 's' }}
      </span>
    </div>

    <div v-if="!selectedDocument" class="rounded-xl border border-dashed border-border bg-surface p-5 text-sm text-muted">
      Select a medical record to see who can access it.
    </div>

    <div v-else>
      <div class="mb-4 rounded-xl border border-border bg-surface p-4">
        <p class="text-sm font-semibold text-ink truncate">{{ selectedDocument.displayName || selectedDocument.originalFileName }}</p>
        <p class="text-xs text-muted mt-1">
          {{ selectedDocument.documentSubType || selectedDocument.documentType }} · {{ selectedDocument.originalFileName }}
        </p>
      </div>

      <div v-if="grants.length" class="space-y-3">
        <div v-for="grant in grants" :key="grant.accessId" class="rounded-xl border border-border p-4 bg-white/40">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="font-semibold text-ink text-sm">{{ grant.doctorName || grant.doctorId }}</p>
              <p class="text-xs text-muted">{{ grant.doctorSpecialization || grant.doctorAuthId }}</p>
            </div>
            <span class="text-[11px] px-2 py-1 rounded-full bg-success/10 text-success font-semibold">Active</span>
          </div>

          <div class="mt-3 space-y-1 text-xs text-muted">
            <p>Appointment: <span class="text-ink font-medium">{{ grant.appointmentId }}</span></p>
            <p>Expires: <span class="text-ink font-medium">{{ formatDateTime(grant.expiresAt) }}</span></p>
          </div>
        </div>
      </div>

      <div v-else class="rounded-xl border border-dashed border-border bg-surface p-5 text-sm text-muted">
        No doctors currently have access to this record.
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  selectedDocument: { type: Object, default: null },
  grants: { type: Array, default: () => [] },
})

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>
