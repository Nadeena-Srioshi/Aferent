<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'

// ── Types ─────────────────────────────────────────────────
interface Appointment {
  id: string
  patientId: string
  patientName: string
  patientEmail: string
  doctorId: string
  doctorName: string
  type: 'PHYSICAL' | 'VIDEO'
  status: string
  appointmentDate: string
  consultationFee: number | { video?: number; physical?: number } | null
  hospitalName?: string
  hospitalLocation?: string
  videoSlotStart?: string
  videoSlotEnd?: string
  paymentId?: string
  createdAt: string
  updatedAt: string
}

type ActionType = 'status' | 'cancel' | null

// ── State ─────────────────────────────────────────────────
const appointments = ref<Appointment[]>([])
const loading = ref(true)
const error = ref('')

// Filters
const search = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const dateFilter = ref('')
const page = ref(1)
const pageSize = 12

// Sort
const sortKey = ref<'date' | 'patient' | 'doctor' | 'fee'>('date')
const sortDir = ref<'asc' | 'desc'>('desc')

// Action modal
const showModal = ref(false)
const modalAction = ref<ActionType>(null)
const selectedAppt = ref<Appointment | null>(null)
const newStatus = ref('')
const actionLoading = ref(false)
const actionError = ref('')
const toast = ref<{ msg: string; type: 'success' | 'error' } | null>(null)
let toastTimer: ReturnType<typeof setTimeout> | null = null

// Detail drawer
const drawerAppt = ref<Appointment | null>(null)

// ── Fetch ─────────────────────────────────────────────────
onMounted(fetchAppointments)

async function fetchAppointments() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get('/appointments')
    const data = res.data
    appointments.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch {
    error.value = 'Failed to load appointments.'
  } finally {
    loading.value = false
  }
}

// ── Filtering / sorting / pagination ──────────────────────
const filtered = computed(() => {
  let list = [...appointments.value]

  const q = search.value.trim().toLowerCase()
  if (q) {
    list = list.filter(a =>
      a.patientName?.toLowerCase().includes(q) ||
      a.doctorName?.toLowerCase().includes(q) ||
      a.id?.toLowerCase().includes(q) ||
      a.patientId?.toLowerCase().includes(q)
    )
  }

  if (statusFilter.value) list = list.filter(a => a.status === statusFilter.value)
  if (typeFilter.value)   list = list.filter(a => a.type === typeFilter.value)
  if (dateFilter.value)   list = list.filter(a => a.appointmentDate?.slice(0, 10) === dateFilter.value)

  list.sort((a, b) => {
    let av: string | number = '', bv: string | number = ''
    if (sortKey.value === 'date')    { av = a.appointmentDate ?? ''; bv = b.appointmentDate ?? '' }
    if (sortKey.value === 'patient') { av = a.patientName ?? '';     bv = b.patientName ?? '' }
    if (sortKey.value === 'doctor')  { av = a.doctorName ?? '';      bv = b.doctorName ?? '' }
    if (sortKey.value === 'fee')     { av = getAppointmentFee(a);  bv = getAppointmentFee(b) }
    if (typeof av === 'number') return sortDir.value === 'asc' ? av - (bv as number) : (bv as number) - av
    return sortDir.value === 'asc'
      ? String(av).localeCompare(String(bv))
      : String(bv).localeCompare(String(av))
  })

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const paginated  = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

// Status breakdown counts for quick-filter pills
const statusCounts = computed(() => {
  const counts: Record<string, number> = {}
  for (const a of appointments.value) counts[a.status] = (counts[a.status] ?? 0) + 1
  return counts
})

function onFilterChange() { page.value = 1 }
function clearFilters() {
  search.value = ''; statusFilter.value = ''; typeFilter.value = ''; dateFilter.value = ''
  page.value = 1
}
function toggleSort(key: typeof sortKey.value) {
  if (sortKey.value === key) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortKey.value = key; sortDir.value = 'asc' }
  page.value = 1
}

// ── Status config ─────────────────────────────────────────
const STATUS_META: Record<string, { label: string; color: string; bg: string }> = {
  PENDING:                   { label: 'Pending',           color: '#92400e', bg: 'rgba(245,158,11,0.10)' },
  ACCEPTED_PENDING_PAYMENT:  { label: 'Awaiting payment',  color: '#1e40af', bg: 'rgba(74,158,255,0.10)' },
  CONFIRMED:                 { label: 'Confirmed',         color: '#065f46', bg: 'rgba(16,185,129,0.10)' },
  COMPLETED:                 { label: 'Completed',         color: '#3730a3', bg: 'rgba(99,102,241,0.10)' },
  CANCELLED:                 { label: 'Cancelled',         color: '#991b1b', bg: 'rgba(239,68,68,0.10)'  },
  REJECTED:                  { label: 'Rejected',          color: '#6b7280', bg: 'rgba(156,163,175,0.15)' },
}

const UPDATABLE_STATUSES = [
  'ACCEPTED_PENDING_PAYMENT',
  'CONFIRMED',
  'COMPLETED',
  'REJECTED',
]

function statusMeta(s: string) {
  return STATUS_META[s] ?? { label: s, color: '#6b7280', bg: 'rgba(156,163,175,0.15)' }
}

// ── Modal actions ─────────────────────────────────────────
function openStatusModal(appt: Appointment) {
  selectedAppt.value = appt
  newStatus.value = ''
  actionError.value = ''
  modalAction.value = 'status'
  showModal.value = true
}

function openCancelModal(appt: Appointment) {
  selectedAppt.value = appt
  actionError.value = ''
  modalAction.value = 'cancel'
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  selectedAppt.value = null
  modalAction.value = null
  newStatus.value = ''
  actionError.value = ''
  actionLoading.value = false
}

async function confirmAction() {
  if (!selectedAppt.value) return
  actionLoading.value = true
  actionError.value = ''

  try {
    if (modalAction.value === 'status') {
      if (!newStatus.value) { actionError.value = 'Please select a status.'; actionLoading.value = false; return }
      await api.patch(`/appointments/${selectedAppt.value.id}/status`, { status: newStatus.value })
      const idx = appointments.value.findIndex(a => a.id === selectedAppt.value!.id)
      if (idx !== -1) appointments.value[idx].status = newStatus.value
      showToast(`Status updated to "${statusMeta(newStatus.value).label}".`, 'success')
    } else {
      await api.patch(`/appointments/${selectedAppt.value.id}/cancel`)
      const idx = appointments.value.findIndex(a => a.id === selectedAppt.value!.id)
      if (idx !== -1) appointments.value[idx].status = 'CANCELLED'
      showToast('Appointment cancelled.', 'success')
    }
    closeModal()
  } catch (e: any) {
    actionError.value = e.response?.data?.message ?? 'Action failed. Please try again.'
  } finally {
    actionLoading.value = false
  }
}

function showToast(msg: string, type: 'success' | 'error') {
  toast.value = { msg, type }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = null }, 4000)
}

// ── Detail drawer ─────────────────────────────────────────
function openDrawer(appt: Appointment) { drawerAppt.value = appt }
function closeDrawer() { drawerAppt.value = null }

// ── Helpers ────────────────────────────────────────────────
function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}
function formatDateTime(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleString('en-GB', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function canCancel(status: string) {
  return ['PENDING', 'CONFIRMED', 'ACCEPTED_PENDING_PAYMENT'].includes(status)
}
function canUpdateStatus(status: string) {
  return !['CANCELLED', 'COMPLETED', 'REJECTED'].includes(status)
}

function getFeeFromShape(fee: Appointment['consultationFee'], type?: Appointment['type']) {
  if (typeof fee === 'number' && Number.isFinite(fee)) return fee
  if (!fee || typeof fee !== 'object') return 0

  const feeByType = type === 'VIDEO' ? fee.video : fee.physical
  if (typeof feeByType === 'number' && Number.isFinite(feeByType)) return feeByType

  const fallback = typeof fee.physical === 'number' ? fee.physical : fee.video
  return Number.isFinite(fallback as number) ? (fallback as number) : 0
}

function getAppointmentFee(appt: Appointment) {
  return getFeeFromShape(appt.consultationFee, appt.type)
}
</script>

<template>
  <div class="appts-page">

    <!-- ── Toast ── -->
    <Transition name="toast-slide">
      <div v-if="toast" class="toast" :class="toast.type">
        <svg v-if="toast.type === 'success'" width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="7.5" cy="7.5" r="6.5" stroke="currentColor" stroke-width="1.3"/>
          <path d="M4.5 7.5L6.5 9.5L10.5 5.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <svg v-else width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="7.5" cy="7.5" r="6.5" stroke="currentColor" stroke-width="1.3"/>
          <path d="M7.5 4.5V8M7.5 10v.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
        {{ toast.msg }}
      </div>
    </Transition>

    <!-- ── Status quick-filter pills ── -->
    <div v-if="!loading && !error" class="status-pills">
      <button
        class="status-pill"
        :class="{ active: statusFilter === '' }"
        @click="statusFilter = ''; onFilterChange()"
      >
        All
        <span class="pill-count">{{ appointments.length }}</span>
      </button>
      <button
        v-for="(meta, key) in STATUS_META"
        :key="key"
        class="status-pill"
        :class="{ active: statusFilter === key }"
        :style="statusFilter === key ? { background: meta.bg, color: meta.color, borderColor: meta.color + '40' } : {}"
        @click="statusFilter = (statusFilter === key ? '' : key); onFilterChange()"
      >
        {{ meta.label }}
        <span class="pill-count">{{ statusCounts[key] ?? 0 }}</span>
      </button>
    </div>

    <!-- ── Search & filters row ── -->
    <div class="filters-bar">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.4"/>
          <path d="M10.5 10.5L13.5 13.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
        <input
          v-model="search" @input="onFilterChange"
          class="search-input" type="text"
          placeholder="Search patient, doctor or appointment ID…"
        />
        <button v-if="search" class="clear-btn" @click="search = ''; onFilterChange()">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M2 2L10 10M10 2L2 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <select v-model="typeFilter" @change="onFilterChange" class="filter-select">
        <option value="">All types</option>
        <option value="PHYSICAL">In-person</option>
        <option value="VIDEO">Video</option>
      </select>

      <input
        v-model="dateFilter" @change="onFilterChange"
        type="date" class="filter-select date-input"
        title="Filter by appointment date"
      />

      <button
        v-if="search || statusFilter || typeFilter || dateFilter"
        class="clear-all-btn" @click="clearFilters"
      >
        Clear filters
      </button>

      <div class="spacer"></div>

      <p class="result-count">{{ filtered.length }} result{{ filtered.length !== 1 ? 's' : '' }}</p>

      <button class="refresh-btn" @click="fetchAppointments" :disabled="loading" title="Refresh">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" :class="{ spinning: loading }">
          <path d="M13 7A6 6 0 112 4M2 1v3h3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>

    <!-- ── Skeleton ── -->
    <div v-if="loading" class="skeleton-table">
      <div class="skeleton-header"></div>
      <div v-for="i in 8" :key="i" class="skeleton-row"></div>
    </div>

    <!-- ── Error ── -->
    <div v-else-if="error" class="error-banner">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <circle cx="8" cy="8" r="7" stroke="#c0392b" stroke-width="1.4"/>
        <path d="M8 5v3.5M8 10.5v.5" stroke="#c0392b" stroke-width="1.4" stroke-linecap="round"/>
      </svg>
      {{ error }}
      <button @click="fetchAppointments" class="retry-btn">Retry</button>
    </div>

    <!-- ── Table ── -->
    <div v-else class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th class="sortable" @click="toggleSort('date')">
                Date
                <span class="sort-icon" :class="{ active: sortKey === 'date' }">
                  {{ sortKey === 'date' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th class="sortable" @click="toggleSort('patient')">
                Patient
                <span class="sort-icon" :class="{ active: sortKey === 'patient' }">
                  {{ sortKey === 'patient' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th class="sortable" @click="toggleSort('doctor')">
                Doctor
                <span class="sort-icon" :class="{ active: sortKey === 'doctor' }">
                  {{ sortKey === 'doctor' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th>Type</th>
              <th>Status</th>
              <th class="sortable" @click="toggleSort('fee')">
                Fee
                <span class="sort-icon" :class="{ active: sortKey === 'fee' }">
                  {{ sortKey === 'fee' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th class="th-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginated.length === 0">
              <td colspan="7" class="empty-cell">
                <div class="empty-state">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                    <rect x="4" y="5" width="24" height="22" rx="3" stroke="#d0d5dd" stroke-width="1.5"/>
                    <path d="M10 3V7M22 3V7M4 12H28" stroke="#d0d5dd" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                  <p>No appointments match your filters.</p>
                  <button class="link-btn" @click="clearFilters">Clear filters</button>
                </div>
              </td>
            </tr>

            <tr
              v-for="appt in paginated"
              :key="appt.id"
              class="data-row"
              @click="openDrawer(appt)"
            >
              <!-- Date -->
              <td class="td-date">
                <p class="date-main">{{ formatDate(appt.appointmentDate) }}</p>
                <p class="date-sub">{{ appt.id?.slice(0, 8) }}</p>
              </td>

              <!-- Patient -->
              <td class="td-person">
                <div class="person-cell">
                  <div class="mini-avatar patient-av">
                    {{ (appt.patientName ?? '?')[0].toUpperCase() }}
                  </div>
                  <div>
                    <p class="person-name">{{ appt.patientName ?? '—' }}</p>
                    <p class="person-sub">{{ appt.patientId ?? '' }}</p>
                  </div>
                </div>
              </td>

              <!-- Doctor -->
              <td class="td-person">
                <div class="person-cell">
                  <div class="mini-avatar doctor-av">
                    {{ (appt.doctorName ?? '?')[0].toUpperCase() }}
                  </div>
                  <div>
                    <p class="person-name">Dr. {{ appt.doctorName ?? '—' }}</p>
                    <p class="person-sub">{{ appt.doctorId ?? '' }}</p>
                  </div>
                </div>
              </td>

              <!-- Type -->
              <td>
                <span class="type-badge" :class="appt.type === 'VIDEO' ? 'video' : 'physical'">
                  <svg v-if="appt.type === 'VIDEO'" width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <rect x="1" y="2.5" width="7" height="7" rx="1" stroke="currentColor" stroke-width="1.2"/>
                    <path d="M8 4.5L11 3V9L8 7.5V4.5Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
                  </svg>
                  <svg v-else width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <path d="M6 1C6 1 2 3.5 2 6.5C2 8.43 3.79 10 6 10C8.21 10 10 8.43 10 6.5C10 3.5 6 1 6 1Z" stroke="currentColor" stroke-width="1.2"/>
                  </svg>
                  {{ appt.type === 'VIDEO' ? 'Video' : 'In-person' }}
                </span>
              </td>

              <!-- Status -->
              <td>
                <span
                  class="status-badge"
                  :style="{ background: statusMeta(appt.status).bg, color: statusMeta(appt.status).color }"
                >
                  {{ statusMeta(appt.status).label }}
                </span>
              </td>

              <!-- Fee -->
              <td class="td-fee">
                LKR {{ getAppointmentFee(appt).toLocaleString() }}
              </td>

              <!-- Actions -->
              <td class="td-actions" @click.stop>
                <div class="action-btns">
                  <button
                    v-if="canUpdateStatus(appt.status)"
                    class="act-btn act-status"
                    title="Update status"
                    @click="openStatusModal(appt)"
                  >
                    <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
                      <path d="M2 6.5H11M8 3.5L11 6.5L8 9.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    Status
                  </button>
                  <button
                    v-if="canCancel(appt.status)"
                    class="act-btn act-cancel"
                    title="Cancel appointment"
                    @click="openCancelModal(appt)"
                  >
                    <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
                      <circle cx="6.5" cy="6.5" r="5.5" stroke="currentColor" stroke-width="1.3"/>
                      <path d="M4 4L9 9M9 4L4 9" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                    </svg>
                    Cancel
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ── Pagination ── -->
      <div class="pagination" v-if="totalPages > 1">
        <button class="page-btn" :disabled="page === 1" @click="page--">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M8.5 3L5 7L8.5 11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="page-numbers">
          <template v-for="p in totalPages" :key="p">
            <button
              v-if="p === 1 || p === totalPages || Math.abs(p - page) <= 1"
              class="page-num" :class="{ active: p === page }" @click="page = p"
            >{{ p }}</button>
            <span v-else-if="Math.abs(p - page) === 2" class="page-ellipsis">…</span>
          </template>
        </div>
        <button class="page-btn" :disabled="page === totalPages" @click="page++">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M5.5 3L9 7L5.5 11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <span class="page-info">
          {{ (page - 1) * pageSize + 1 }}–{{ Math.min(page * pageSize, filtered.length) }}
          of {{ filtered.length }}
        </span>
      </div>
    </div>

    <!-- ════════════════════════════════════
         Detail drawer (slide in from right)
    ════════════════════════════════════ -->
    <Transition name="drawer">
      <div v-if="drawerAppt" class="drawer-overlay" @click.self="closeDrawer">
        <div class="drawer">
          <div class="drawer-header">
            <h3 class="drawer-title">Appointment detail</h3>
            <button class="drawer-close" @click="closeDrawer">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M3 3L13 13M13 3L3 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>

          <div class="drawer-body">

            <!-- ID + status -->
            <div class="drawer-section">
              <div class="drawer-id-row">
                <span class="drawer-id">{{ drawerAppt.id }}</span>
                <span
                  class="status-badge"
                  :style="{ background: statusMeta(drawerAppt.status).bg, color: statusMeta(drawerAppt.status).color }"
                >{{ statusMeta(drawerAppt.status).label }}</span>
              </div>
            </div>

            <!-- Type + date -->
            <div class="drawer-section">
              <div class="drawer-row">
                <span class="drawer-label">Type</span>
                <span class="type-badge" :class="drawerAppt.type === 'VIDEO' ? 'video' : 'physical'">
                  {{ drawerAppt.type === 'VIDEO' ? 'Video' : 'In-person' }}
                </span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">Date</span>
                <span class="drawer-value">{{ formatDate(drawerAppt.appointmentDate) }}</span>
              </div>
              <div v-if="drawerAppt.type === 'VIDEO' && drawerAppt.videoSlotStart" class="drawer-row">
                <span class="drawer-label">Video slot</span>
                <span class="drawer-value">{{ drawerAppt.videoSlotStart }} – {{ drawerAppt.videoSlotEnd }}</span>
              </div>
              <div v-if="drawerAppt.type === 'PHYSICAL' && drawerAppt.hospitalName" class="drawer-row">
                <span class="drawer-label">Hospital</span>
                <span class="drawer-value">{{ drawerAppt.hospitalName }}</span>
              </div>
              <div v-if="drawerAppt.hospitalLocation" class="drawer-row">
                <span class="drawer-label">Location</span>
                <span class="drawer-value">{{ drawerAppt.hospitalLocation }}</span>
              </div>
            </div>

            <!-- Patient -->
            <div class="drawer-section">
              <p class="drawer-section-title">Patient</p>
              <div class="drawer-row">
                <span class="drawer-label">Name</span>
                <span class="drawer-value">{{ drawerAppt.patientName ?? '—' }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">ID</span>
                <span class="drawer-value mono">{{ drawerAppt.patientId ?? '—' }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">Email</span>
                <span class="drawer-value">{{ drawerAppt.patientEmail ?? '—' }}</span>
              </div>
            </div>

            <!-- Doctor -->
            <div class="drawer-section">
              <p class="drawer-section-title">Doctor</p>
              <div class="drawer-row">
                <span class="drawer-label">Name</span>
                <span class="drawer-value">Dr. {{ drawerAppt.doctorName ?? '—' }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">ID</span>
                <span class="drawer-value mono">{{ drawerAppt.doctorId ?? '—' }}</span>
              </div>
            </div>

            <!-- Payment -->
            <div class="drawer-section">
              <p class="drawer-section-title">Payment</p>
              <div class="drawer-row">
                <span class="drawer-label">Fee</span>
                <span class="drawer-value fee-val">LKR {{ getAppointmentFee(drawerAppt) .toLocaleString() }}</span>
              </div>
              <div v-if="drawerAppt.paymentId" class="drawer-row">
                <span class="drawer-label">Payment ID</span>
                <span class="drawer-value mono">{{ drawerAppt.paymentId }}</span>
              </div>
            </div>

            <!-- Timestamps -->
            <div class="drawer-section no-border">
              <div class="drawer-row">
                <span class="drawer-label">Created</span>
                <span class="drawer-value">{{ formatDateTime(drawerAppt.createdAt) }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">Updated</span>
                <span class="drawer-value">{{ formatDateTime(drawerAppt.updatedAt) }}</span>
              </div>
            </div>
          </div>

          <!-- Drawer actions -->
          <div class="drawer-footer">
            <button
              v-if="canUpdateStatus(drawerAppt.status)"
              class="drawer-act-btn status-act"
              @click="closeDrawer(); openStatusModal(drawerAppt)"
            >
              Update status
            </button>
            <button
              v-if="canCancel(drawerAppt.status)"
              class="drawer-act-btn cancel-act"
              @click="closeDrawer(); openCancelModal(drawerAppt)"
            >
              Cancel appointment
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ════════════════════════════════════
         Action modal (status / cancel)
    ════════════════════════════════════ -->
    <Transition name="modal-fade">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal">

          <!-- Status update modal -->
          <template v-if="modalAction === 'status'">
            <div class="modal-icon status-icon">
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                <path d="M4 11H18M13 6L18 11L13 16" stroke="#4a9eff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <h3 class="modal-title">Update appointment status</h3>
            <p class="modal-sub">
              {{ selectedAppt?.patientName }} with Dr. {{ selectedAppt?.doctorName }}
              · {{ formatDate(selectedAppt?.appointmentDate ?? '') }}
            </p>

            <div class="status-select-grid">
              <button
                v-for="s in UPDATABLE_STATUSES"
                :key="s"
                class="status-option"
                :class="{ selected: newStatus === s }"
                :style="newStatus === s
                  ? { background: statusMeta(s).bg, borderColor: statusMeta(s).color, color: statusMeta(s).color }
                  : {}"
                @click="newStatus = s"
              >
                {{ statusMeta(s).label }}
              </button>
            </div>

            <p v-if="actionError" class="modal-error">{{ actionError }}</p>

            <div class="modal-actions">
              <button class="modal-cancel" @click="closeModal">Cancel</button>
              <button class="modal-confirm status-confirm" :disabled="actionLoading || !newStatus" @click="confirmAction">
                <span v-if="actionLoading" class="btn-spinner"></span>
                {{ actionLoading ? 'Updating…' : 'Update status' }}
              </button>
            </div>
          </template>

          <!-- Cancel modal -->
          <template v-else-if="modalAction === 'cancel'">
            <div class="modal-icon cancel-icon">
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                <circle cx="11" cy="11" r="9" stroke="#ef4444" stroke-width="1.5"/>
                <path d="M7 7L15 15M15 7L7 15" stroke="#ef4444" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
            </div>
            <h3 class="modal-title">Cancel appointment</h3>
            <p class="modal-body">
              Are you sure you want to cancel the appointment for
              <strong>{{ selectedAppt?.patientName }}</strong>
              with <strong>Dr. {{ selectedAppt?.doctorName }}</strong>
              on {{ formatDate(selectedAppt?.appointmentDate ?? '') }}?
              <br/><br/>
              This action cannot be undone.
            </p>
            <p v-if="actionError" class="modal-error">{{ actionError }}</p>
            <div class="modal-actions">
              <button class="modal-cancel" @click="closeModal">Keep appointment</button>
              <button class="modal-confirm cancel-confirm" :disabled="actionLoading" @click="confirmAction">
                <span v-if="actionLoading" class="btn-spinner cancel-spinner"></span>
                {{ actionLoading ? 'Cancelling…' : 'Yes, cancel it' }}
              </button>
            </div>
          </template>

        </div>
      </div>
    </Transition>

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700&family=DM+Sans:wght@400;500&display=swap');

.appts-page {
  font-family: 'DM Sans', sans-serif;
  animation: fadeIn 0.3s ease;
  position: relative;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Toast ───────────────────────────────────────────────── */
.toast {
  position: fixed;
  top: 20px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 18px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  z-index: 999;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
}
.toast.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; }
.toast.error   { background: #fdf2f2; border: 1px solid #f5c6c6; color: #c0392b; }

/* ── Status pills ────────────────────────────────────────── */
.status-pills {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 20px;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #5a6578;
  background: white;
  border: 1px solid #eaecf0;
  cursor: pointer;
  transition: all 0.15s;
}
.status-pill:hover { border-color: #d0d5dd; background: #f8f9fc; }
.status-pill.active { background: #0f2744; color: white; border-color: #0f2744; }
.pill-count {
  font-size: 11px;
  background: rgba(0,0,0,0.08);
  padding: 1px 5px;
  border-radius: 10px;
  font-weight: 600;
}
.status-pill.active .pill-count { background: rgba(255,255,255,0.2); }

/* ── Filters bar ─────────────────────────────────────────── */
.filters-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  align-items: center;
  flex-wrap: wrap;
}
.search-wrap {
  position: relative;
  flex: 1;
  min-width: 220px;
}
.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #a0aab8;
  pointer-events: none;
}
.search-input {
  width: 100%;
  height: 38px;
  padding: 0 34px;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #0f2744;
  background: white;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input::placeholder { color: #bbc4d0; }
.search-input:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.1); }
.clear-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #a0aab8;
  display: flex;
  align-items: center;
  padding: 4px;
}
.clear-btn:hover { color: #3d4a5c; }
.filter-select {
  height: 38px;
  padding: 0 10px;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #3d4a5c;
  background: white;
  outline: none;
  cursor: pointer;
}
.filter-select:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.1); }
.date-input { min-width: 150px; }
.clear-all-btn {
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  color: #ef4444;
  background: rgba(239,68,68,0.06);
  border: 1px solid rgba(239,68,68,0.2);
  border-radius: 8px;
  padding: 5px 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}
.clear-all-btn:hover { background: rgba(239,68,68,0.12); }
.spacer { flex: 1; }
.result-count { font-size: 12px; color: #9aa3b0; white-space: nowrap; margin: 0; }
.refresh-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  cursor: pointer;
  color: #5a6578;
  flex-shrink: 0;
  transition: background 0.15s;
}
.refresh-btn:hover:not(:disabled) { background: #f4f5f7; }
.refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.spinning { animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Skeleton ─────────────────────────────────────────────── */
.skeleton-table { background: white; border: 1px solid #eaecf0; border-radius: 12px; overflow: hidden; }
.skeleton-header { height: 44px; background: #f4f5f7; }
.skeleton-row {
  height: 58px;
  border-top: 1px solid #f0f2f5;
  background: linear-gradient(90deg, #f8f9fb 25%, #eef0f3 50%, #f8f9fb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Error ────────────────────────────────────────────────── */
.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fdf2f2;
  border: 1px solid #f5c6c6;
  border-radius: 10px;
  padding: 14px 18px;
  font-size: 14px;
  color: #c0392b;
}
.retry-btn {
  margin-left: auto;
  background: none;
  border: 1px solid #c0392b;
  color: #c0392b;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 13px;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
}

/* ── Table ───────────────────────────────────────────────── */
.table-card { background: white; border: 1px solid #eaecf0; border-radius: 12px; overflow: hidden; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }

thead tr { background: #f8f9fc; border-bottom: 1px solid #eaecf0; }
th {
  padding: 11px 14px;
  text-align: left;
  font-size: 11px;
  font-weight: 600;
  color: #8a94a6;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
  user-select: none;
}
th.sortable { cursor: pointer; }
th.sortable:hover { color: #0f2744; }
.sort-icon { margin-left: 4px; font-size: 10px; color: #bbc4d0; }
.sort-icon.active { color: #4a9eff; }

.data-row {
  border-bottom: 1px solid #f4f5f7;
  cursor: pointer;
  transition: background 0.12s;
}
.data-row:last-child { border-bottom: none; }
.data-row:hover { background: #f8f9fc; }

td { padding: 12px 14px; color: #3d4a5c; vertical-align: middle; }

.td-date { white-space: nowrap; }
.date-main { font-size: 13px; font-weight: 500; color: #0f2744; margin: 0 0 2px; }
.date-sub  { font-size: 11px; color: #9aa3b0; font-family: monospace; margin: 0; }

.td-person { min-width: 160px; }
.person-cell { display: flex; align-items: center; gap: 8px; }
.mini-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.patient-av { background: rgba(74,158,255,0.12);  color: #1a6fc4; }
.doctor-av  { background: rgba(16,185,129,0.12);  color: #065f46; }
.person-name { font-size: 13px; font-weight: 500; color: #0f2744; margin: 0 0 2px; white-space: nowrap; }
.person-sub  { font-size: 11px; color: #9aa3b0; font-family: monospace; margin: 0; }

/* Type badge */
.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  padding: 4px 9px;
  border-radius: 20px;
  white-space: nowrap;
}
.type-badge.video    { background: rgba(99,102,241,0.1);  color: #4338ca; }
.type-badge.physical { background: rgba(16,185,129,0.1); color: #065f46; }

/* Status badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 500;
  padding: 4px 9px;
  border-radius: 20px;
  white-space: nowrap;
}

.td-fee { font-weight: 500; color: #0f2744; white-space: nowrap; }

/* Action buttons */
.th-actions { width: 160px; }
.td-actions { width: 160px; }
.action-btns { display: flex; gap: 5px; }
.act-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  font-weight: 500;
  padding: 5px 9px;
  border-radius: 7px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
  border: 1px solid transparent;
}
.act-status {
  color: #1e40af;
  background: rgba(74,158,255,0.08);
  border-color: rgba(74,158,255,0.2);
}
.act-status:hover { background: rgba(74,158,255,0.15); }
.act-cancel {
  color: #991b1b;
  background: rgba(239,68,68,0.07);
  border-color: rgba(239,68,68,0.2);
}
.act-cancel:hover { background: rgba(239,68,68,0.13); }

/* Empty */
.empty-cell { text-align: center; padding: 56px 16px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-state p { font-size: 14px; color: #9aa3b0; margin: 0; }
.link-btn {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #4a9eff;
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 14px;
  border-top: 1px solid #f0f2f5;
}
.page-btn {
  width: 32px; height: 32px;
  border: 1px solid #eaecf0; border-radius: 7px;
  background: white; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: #3d4a5c; transition: background 0.12s;
}
.page-btn:hover:not(:disabled) { background: #f4f5f7; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-numbers { display: flex; align-items: center; gap: 2px; }
.page-num {
  min-width: 32px; height: 32px;
  border: 1px solid #eaecf0; border-radius: 7px;
  background: white;
  font-family: 'DM Sans', sans-serif; font-size: 13px; color: #3d4a5c;
  cursor: pointer; transition: background 0.12s; padding: 0 6px;
}
.page-num:hover { background: #f4f5f7; }
.page-num.active { background: #0f2744; color: white; border-color: #0f2744; font-weight: 500; }
.page-ellipsis { font-size: 13px; color: #9aa3b0; padding: 0 4px; }
.page-info { margin-left: auto; font-size: 12px; color: #9aa3b0; }

/* ════════════════════════
   Detail drawer
════════════════════════ */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,39,68,0.3);
  z-index: 150;
  display: flex;
  justify-content: flex-end;
}
.drawer {
  width: 380px;
  max-width: 100vw;
  height: 100%;
  background: white;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0,0,0,0.1);
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #eaecf0;
  flex-shrink: 0;
}
.drawer-title {
  font-family: 'Syne', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #0f2744;
  margin: 0;
}
.drawer-close {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none; cursor: pointer;
  color: #8a94a6; border-radius: 6px;
  transition: background 0.15s;
}
.drawer-close:hover { background: #f4f5f7; color: #3d4a5c; }

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px;
}
.drawer-section {
  padding: 16px 0;
  border-bottom: 1px solid #f4f5f7;
}
.drawer-section.no-border { border-bottom: none; }
.drawer-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #9aa3b0;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin: 0 0 10px;
}
.drawer-id-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.drawer-id {
  font-size: 12px;
  font-family: monospace;
  color: #5a6578;
  word-break: break-all;
}
.drawer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  gap: 12px;
}
.drawer-label {
  font-size: 12px;
  color: #9aa3b0;
  flex-shrink: 0;
}
.drawer-value {
  font-size: 13px;
  color: #0f2744;
  font-weight: 500;
  text-align: right;
}
.drawer-value.mono { font-family: monospace; font-size: 12px; }
.fee-val { color: #065f46; font-weight: 600; }

.drawer-footer {
  padding: 16px 24px;
  border-top: 1px solid #eaecf0;
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.drawer-act-btn {
  flex: 1;
  height: 40px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 500;
  border-radius: 9px;
  cursor: pointer;
  transition: background 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-act {
  color: #1e40af;
  background: rgba(74,158,255,0.08);
  border: 1px solid rgba(74,158,255,0.25);
}
.status-act:hover { background: rgba(74,158,255,0.15); }
.cancel-act {
  color: #991b1b;
  background: rgba(239,68,68,0.07);
  border: 1px solid rgba(239,68,68,0.2);
}
.cancel-act:hover { background: rgba(239,68,68,0.13); }

/* ════════════════════════
   Action modal
════════════════════════ */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,39,68,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: 16px;
}
.modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
}
.modal-icon {
  width: 52px; height: 52px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 4px;
}
.status-icon { background: rgba(74,158,255,0.1); }
.cancel-icon { background: rgba(239,68,68,0.1); }
.modal-title {
  font-family: 'Syne', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #0f2744;
  margin: 0;
}
.modal-sub {
  font-size: 13px;
  color: #8a94a6;
  margin: 0;
}
.modal-body {
  font-size: 14px;
  color: #5a6578;
  line-height: 1.65;
  margin: 0;
  text-align: center;
}
.modal-error {
  font-size: 13px;
  color: #c0392b;
  background: #fdf2f2;
  border: 1px solid #f5c6c6;
  border-radius: 8px;
  padding: 8px 14px;
  width: 100%;
  margin: 0;
  text-align: left;
}

/* Status select grid */
.status-select-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}
.status-option {
  height: 40px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #5a6578;
  background: #f8f9fc;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.15s;
}
.status-option:hover { border-color: #d0d5dd; background: #f0f2f5; }
.status-option.selected { font-weight: 600; }

/* Modal actions */
.modal-actions { display: flex; gap: 10px; width: 100%; margin-top: 4px; }
.modal-cancel {
  flex: 1; height: 42px;
  font-family: 'DM Sans', sans-serif; font-size: 14px; font-weight: 500;
  color: #3d4a5c; background: white; border: 1px solid #eaecf0;
  border-radius: 10px; cursor: pointer; transition: background 0.15s;
}
.modal-cancel:hover { background: #f4f5f7; }
.modal-confirm {
  flex: 1; height: 42px;
  display: flex; align-items: center; justify-content: center; gap: 7px;
  font-family: 'DM Sans', sans-serif; font-size: 14px; font-weight: 500;
  color: white; border: none; border-radius: 10px; cursor: pointer; transition: background 0.15s;
}
.modal-confirm:disabled { opacity: 0.55; cursor: not-allowed; }
.status-confirm { background: #4a9eff; }
.status-confirm:hover:not(:disabled) { background: #2d87f0; }
.cancel-confirm { background: #ef4444; }
.cancel-confirm:hover:not(:disabled) { background: #dc2626; }

.btn-spinner {
  width: 13px; height: 13px;
  border: 2px solid rgba(255,255,255,0.35);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ── Transitions ─────────────────────────────────────────── */
.toast-slide-enter-active, .toast-slide-leave-active { transition: all 0.25s ease; }
.toast-slide-enter-from, .toast-slide-leave-to { opacity: 0; transform: translateX(20px); }

.drawer-enter-active, .drawer-leave-active { transition: opacity 0.25s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-active .drawer, .drawer-leave-active .drawer { transition: transform 0.25s ease; }
.drawer-enter-from .drawer { transform: translateX(100%); }
.drawer-leave-to .drawer { transform: translateX(100%); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .modal { transition: transform 0.2s ease; }
.modal-fade-enter-from .modal { transform: scale(0.96) translateY(8px); }
</style>