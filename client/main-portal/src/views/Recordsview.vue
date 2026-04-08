<template>
  <main id="main-content" class="min-h-screen bg-surface">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 py-10">

      <!-- Header -->
      <div class="flex items-center justify-between gap-4 mb-8 flex-wrap">
        <div>
          <h1 class="text-2xl font-bold text-ink mb-1">Medical Records</h1>
          <p class="text-muted">Your health history, prescriptions, and lab results — all in one place.</p>
        </div>
        <button
          class="inline-flex items-center gap-2 px-5 py-2.5 border-2 border-border text-ink font-semibold rounded-xl hover:border-primary hover:text-primary transition-colors text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          @click="exportRecords"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          Export PDF
        </button>
      </div>

      <!-- Tabs -->
      <div class="flex gap-1 mb-6 border-b border-border overflow-x-auto" role="tablist" aria-label="Record categories">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          role="tab"
          :aria-selected="activeTab === tab.key"
          :class="[
            'px-5 py-3 text-sm font-semibold whitespace-nowrap border-b-2 -mb-px transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-t-lg',
            activeTab === tab.key ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-ink'
          ]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Records list -->
      <!-- TODO: bind to recordsService — implement GET /records?type=activeTab -->
      <div role="tabpanel" :aria-label="currentTab?.label">
        <div v-if="mockRecords[activeTab]?.length" class="space-y-3">
          <div
            v-for="record in mockRecords[activeTab]"
            :key="record.id"
            class="bg-card border border-border rounded-2xl p-5 flex items-start gap-4 hover:shadow-sm transition-shadow"
          >
            <!-- Icon -->
            <div class="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0" :class="record.iconBg" aria-hidden="true">
              <svg class="w-5 h-5" :class="record.iconColor" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="record.icon"/>
              </svg>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-2 flex-wrap">
                <div>
                  <h3 class="font-semibold text-ink text-sm">{{ record.title }}</h3>
                  <p class="text-xs text-muted mt-0.5">{{ record.doctor }} · {{ record.date }}</p>
                </div>
                <span class="text-xs font-semibold px-2.5 py-1 rounded-full flex-shrink-0" :class="record.statusClass">
                  {{ record.status }}
                </span>
              </div>
              <p v-if="record.notes" class="text-xs text-muted mt-2 leading-relaxed">{{ record.notes }}</p>
            </div>

            <button
              class="flex-shrink-0 p-2 rounded-lg text-muted hover:text-primary hover:bg-primary/10 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              :aria-label="`Download ${record.title}`"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Empty state -->
        <div v-else class="bg-card border border-border rounded-2xl">
          <div class="flex flex-col items-center justify-center py-16 px-6 text-center">
            <div class="w-16 h-16 rounded-2xl bg-surface border border-border flex items-center justify-center mb-4" aria-hidden="true">
              <svg class="w-8 h-8 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </div>
            <h3 class="font-semibold text-ink mb-1">No {{ currentTab?.label.toLowerCase() }} yet</h3>
            <p class="text-sm text-muted">Records from your consultations will appear here.</p>
          </div>
        </div>
      </div>

    </div>
  </main>
</template>

<script setup>
import { ref, computed } from 'vue'
// TODO: import recordsService from '@/services/recordsService'

const activeTab = ref('visits')

const tabs = [
  { key: 'visits',        label: 'Visit Summaries' },
  { key: 'prescriptions', label: 'Prescriptions' },
  { key: 'labs',          label: 'Lab Results' },
  { key: 'imaging',       label: 'Imaging' },
  { key: 'referrals',     label: 'Referrals' },
]

const currentTab = computed(() => tabs.find(t => t.key === activeTab.value))

// ── Mock data — replace with API data ──────────────────────────
const mockRecords = {
  visits: [
    { id: 1, title: 'General Checkup',         doctor: 'Dr. Sarah Mitchell', date: '14 Mar 2026', status: 'Completed', statusClass: 'bg-success/10 text-success', notes: 'Blood pressure slightly elevated. Monitor for 2 weeks. Follow-up booked.',        icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z', iconBg: 'bg-primary/10', iconColor: 'text-primary' },
    { id: 2, title: 'Mental Health Consultation', doctor: 'Dr. Riya Patel', date: '2 Feb 2026',   status: 'Completed', statusClass: 'bg-success/10 text-success', notes: 'Discussed sleep patterns. CBT exercises recommended.',                        icon: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z', iconBg: 'bg-ai/10', iconColor: 'text-ai' },
  ],
  prescriptions: [
    { id: 3, title: 'Amlodipine 5mg',           doctor: 'Dr. Sarah Mitchell', date: '14 Mar 2026', status: 'Active',    statusClass: 'bg-warning/10 text-warning', notes: 'Once daily. Take in the morning. 30-day supply with 2 refills.',              icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z', iconBg: 'bg-warning/10', iconColor: 'text-warning' },
  ],
  labs: [],
  imaging: [],
  referrals: [],
}

function exportRecords() {
  // TODO: call recordsService.exportPdf(activeTab.value)
  console.log('Export PDF — wire to recordsService')
}
</script>