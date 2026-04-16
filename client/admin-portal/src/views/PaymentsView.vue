<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'

// ── Types ─────────────────────────────────────────────────
interface Payment {
  id: string
  appointmentId: string
  patientId: string
  patientName?: string
  doctorId: string
  doctorName?: string
  amount: number
  status: string
  method?: string
  transactionRef?: string
  createdAt: string
  updatedAt: string
}

// ── State ─────────────────────────────────────────────────
const payments = ref<Payment[]>([])
const loading = ref(true)
const error = ref('')

const search = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 12
const sortKey = ref<'date' | 'amount' | 'patient'>('date')
const sortDir = ref<'asc' | 'desc'>('desc')

// Refund modal
const showRefundModal = ref(false)
const refundTarget = ref<Payment | null>(null)
const refundLoading = ref(false)
const refundError = ref('')

// Detail drawer
const drawerPayment = ref<Payment | null>(null)

// Toast
const toast = ref<{ msg: string; type: 'success' | 'error' } | null>(null)
let toastTimer: ReturnType<typeof setTimeout> | null = null

// ── Fetch ─────────────────────────────────────────────────
onMounted(fetchPayments)

async function fetchPayments() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get('/payments')
    const data = res.data
    payments.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch {
    error.value = 'Failed to load payments.'
  } finally {
    loading.value = false
  }
}

// ── Filtering / sorting / pagination ──────────────────────
const filtered = computed(() => {
  let list = [...payments.value]

  const q = search.value.trim().toLowerCase()
  if (q) {
    list = list.filter(p =>
      p.id?.toLowerCase().includes(q) ||
      p.patientName?.toLowerCase().includes(q) ||
      p.doctorName?.toLowerCase().includes(q) ||
      p.transactionRef?.toLowerCase().includes(q) ||
      p.appointmentId?.toLowerCase().includes(q)
    )
  }

  if (statusFilter.value) list = list.filter(p => p.status === statusFilter.value)

  list.sort((a, b) => {
    if (sortKey.value === 'amount') {
      return sortDir.value === 'asc'
        ? (a.amount ?? 0) - (b.amount ?? 0)
        : (b.amount ?? 0) - (a.amount ?? 0)
    }
    const av = sortKey.value === 'patient' ? (a.patientName ?? '') : (a.createdAt ?? '')
    const bv = sortKey.value === 'patient' ? (b.patientName ?? '') : (b.createdAt ?? '')
    return sortDir.value === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av)
  })

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const paginated  = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))

// ── Revenue summary cards ──────────────────────────────────
const summary = computed(() => {
  const all = payments.value
  const paid    = all.filter(p => ['PAID', 'COMPLETED'].includes(p.status))
  const pending = all.filter(p => p.status === 'PENDING')
  const refunded = all.filter(p => p.status === 'REFUNDED')

  return {
    total:         all.length,
    totalRevenue:  paid.reduce((s, p) => s + (p.amount ?? 0), 0),
    pendingAmount: pending.reduce((s, p) => s + (p.amount ?? 0), 0),
    refundedAmount: refunded.reduce((s, p) => s + (p.amount ?? 0), 0),
    paidCount:     paid.length,
    pendingCount:  pending.length,
    refundedCount: refunded.length,
  }
})

// Status counts for pills
const statusCounts = computed(() => {
  const c: Record<string, number> = {}
  for (const p of payments.value) c[p.status] = (c[p.status] ?? 0) + 1
  return c
})

function onFilterChange() { page.value = 1 }
function clearFilters() { search.value = ''; statusFilter.value = ''; page.value = 1 }
function toggleSort(key: typeof sortKey.value) {
  if (sortKey.value === key) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortKey.value = key; sortDir.value = 'asc' }
  page.value = 1
}

// ── Status config ──────────────────────────────────────────
const STATUS_META: Record<string, { label: string; color: string; bg: string }> = {
  PENDING:   { label: 'Pending',   color: '#92400e', bg: 'rgba(245,158,11,0.10)' },
  PAID:      { label: 'Paid',      color: '#065f46', bg: 'rgba(16,185,129,0.10)' },
  COMPLETED: { label: 'Completed', color: '#3730a3', bg: 'rgba(99,102,241,0.10)' },
  FAILED:    { label: 'Failed',    color: '#991b1b', bg: 'rgba(239,68,68,0.10)'  },
  REFUNDED:  { label: 'Refunded',  color: '#1e40af', bg: 'rgba(74,158,255,0.10)' },
}

function statusMeta(s: string) {
  return STATUS_META[s] ?? { label: s, color: '#6b7280', bg: 'rgba(156,163,175,0.15)' }
}

function canRefund(status: string) {
  return ['PAID', 'COMPLETED'].includes(status)
}

// ── Refund ─────────────────────────────────────────────────
function promptRefund(payment: Payment) {
  refundTarget.value = payment
  refundError.value = ''
  showRefundModal.value = true
}

function closeRefundModal() {
  showRefundModal.value = false
  refundTarget.value = null
  refundError.value = ''
  refundLoading.value = false
}

async function confirmRefund() {
  if (!refundTarget.value) return
  refundLoading.value = true
  refundError.value = ''

  try {
    await api.post(`/payments/${refundTarget.value.id}/refund`)
    // Update local state — no re-fetch needed
    const idx = payments.value.findIndex(p => p.id === refundTarget.value!.id)
    if (idx !== -1) payments.value[idx].status = 'REFUNDED'
    showToast(`Refund processed for LKR ${(refundTarget.value.amount ?? 0).toLocaleString()}.`, 'success')
    // Close drawer if it was showing this payment
    if (drawerPayment.value?.id === refundTarget.value.id) {
      drawerPayment.value.status = 'REFUNDED'
    }
    closeRefundModal()
  } catch (e: any) {
    refundError.value = e.response?.data?.message ?? 'Refund failed. Please try again.'
  } finally {
    refundLoading.value = false
  }
}

// ── Toast ──────────────────────────────────────────────────
function showToast(msg: string, type: 'success' | 'error') {
  toast.value = { msg, type }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = null }, 4500)
}

// ── Drawer ─────────────────────────────────────────────────
function openDrawer(p: Payment) { drawerPayment.value = p }
function closeDrawer() { drawerPayment.value = null }

// ── Helpers ────────────────────────────────────────────────
function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}
function formatDateTime(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleString('en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}
function formatCurrency(n: number) {
  return 'LKR ' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<template>
  <div class="payments-page">

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

    <!-- ── Summary cards ── -->
    <div v-if="!loading && !error" class="summary-grid">
      <div class="summary-card">
        <div class="summary-icon" style="background: rgba(16,185,129,0.1)">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" style="color:#10b981">
            <rect x="1" y="4" width="16" height="11" rx="2" stroke="currentColor" stroke-width="1.4"/>
            <path d="M1 8H17" stroke="currentColor" stroke-width="1.4"/>
            <circle cx="5" cy="12" r="1" fill="currentColor"/>
          </svg>
        </div>
        <div>
          <p class="summary-label">Total revenue</p>
          <p class="summary-value green">{{ formatCurrency(summary.totalRevenue) }}</p>
          <p class="summary-sub">{{ summary.paidCount }} paid transaction{{ summary.paidCount !== 1 ? 's' : '' }}</p>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-icon" style="background: rgba(245,158,11,0.1)">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" style="color:#f59e0b">
            <circle cx="9" cy="9" r="7.5" stroke="currentColor" stroke-width="1.4"/>
            <path d="M9 5.5V9.5L11.5 12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div>
          <p class="summary-label">Pending payments</p>
          <p class="summary-value amber">{{ formatCurrency(summary.pendingAmount) }}</p>
          <p class="summary-sub">{{ summary.pendingCount }} awaiting payment</p>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-icon" style="background: rgba(74,158,255,0.1)">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" style="color:#4a9eff">
            <path d="M3 9H15M9 3L15 9L9 15" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div>
          <p class="summary-label">Total refunded</p>
          <p class="summary-value blue">{{ formatCurrency(summary.refundedAmount) }}</p>
          <p class="summary-sub">{{ summary.refundedCount }} refund{{ summary.refundedCount !== 1 ? 's' : '' }} issued</p>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-icon" style="background: rgba(139,92,246,0.1)">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none" style="color:#8b5cf6">
            <rect x="2" y="2" width="14" height="14" rx="2" stroke="currentColor" stroke-width="1.4"/>
            <path d="M6 9H12M9 6V12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
          </svg>
        </div>
        <div>
          <p class="summary-label">All transactions</p>
          <p class="summary-value purple">{{ summary.total }}</p>
          <p class="summary-sub">across all statuses</p>
        </div>
      </div>
    </div>

    <!-- ── Status pills ── -->
    <div v-if="!loading && !error" class="status-pills">
      <button
        class="status-pill" :class="{ active: statusFilter === '' }"
        @click="statusFilter = ''; onFilterChange()"
      >
        All <span class="pill-count">{{ payments.length }}</span>
      </button>
      <button
        v-for="(meta, key) in STATUS_META" :key="key"
        class="status-pill" :class="{ active: statusFilter === key }"
        :style="statusFilter === key ? { background: meta.bg, color: meta.color, borderColor: meta.color + '40' } : {}"
        @click="statusFilter = (statusFilter === key ? '' : key); onFilterChange()"
      >
        {{ meta.label }} <span class="pill-count">{{ statusCounts[key] ?? 0 }}</span>
      </button>
    </div>

    <!-- ── Filters bar ── -->
    <div class="filters-bar">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.4"/>
          <path d="M10.5 10.5L13.5 13.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
        <input
          v-model="search" @input="onFilterChange"
          class="search-input" type="text"
          placeholder="Search by ID, patient, doctor or transaction ref…"
        />
        <button v-if="search" class="clear-btn" @click="search = ''; onFilterChange()">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M2 2L10 10M10 2L2 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <button
        v-if="search || statusFilter"
        class="clear-all-btn" @click="clearFilters"
      >
        Clear filters
      </button>

      <div class="spacer"></div>
      <p class="result-count">{{ filtered.length }} result{{ filtered.length !== 1 ? 's' : '' }}</p>

      <button class="refresh-btn" @click="fetchPayments" :disabled="loading" title="Refresh">
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
      <button @click="fetchPayments" class="retry-btn">Retry</button>
    </div>

    <!-- ── Table ── -->
    <div v-else class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Payment ID</th>
              <th class="sortable" @click="toggleSort('patient')">
                Patient
                <span class="sort-icon" :class="{ active: sortKey === 'patient' }">
                  {{ sortKey === 'patient' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th>Doctor</th>
              <th class="sortable" @click="toggleSort('amount')">
                Amount
                <span class="sort-icon" :class="{ active: sortKey === 'amount' }">
                  {{ sortKey === 'amount' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th>Method</th>
              <th>Status</th>
              <th class="sortable" @click="toggleSort('date')">
                Date
                <span class="sort-icon" :class="{ active: sortKey === 'date' }">
                  {{ sortKey === 'date' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th class="th-action">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginated.length === 0">
              <td colspan="8" class="empty-cell">
                <div class="empty-state">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                    <rect x="2" y="7" width="28" height="18" rx="3" stroke="#d0d5dd" stroke-width="1.5"/>
                    <path d="M2 13H30" stroke="#d0d5dd" stroke-width="1.5"/>
                    <circle cx="8" cy="20" r="2" stroke="#d0d5dd" stroke-width="1.5"/>
                  </svg>
                  <p>No payments match your filters.</p>
                  <button class="link-btn" @click="clearFilters">Clear filters</button>
                </div>
              </td>
            </tr>

            <tr
              v-for="payment in paginated"
              :key="payment.id"
              class="data-row"
              @click="openDrawer(payment)"
            >
              <!-- Payment ID -->
              <td>
                <div class="id-cell">
                  <span class="id-text">{{ payment.id?.slice(0, 10) }}…</span>
                  <span v-if="payment.transactionRef" class="txn-ref">{{ payment.transactionRef }}</span>
                </div>
              </td>

              <!-- Patient -->
              <td class="td-person">
                <div class="person-cell">
                  <div class="mini-avatar patient-av">
                    {{ (payment.patientName ?? '?')[0].toUpperCase() }}
                  </div>
                  <div>
                    <p class="person-name">{{ payment.patientName ?? '—' }}</p>
                    <p class="person-sub">{{ payment.patientId ?? '' }}</p>
                  </div>
                </div>
              </td>

              <!-- Doctor -->
              <td>
                <p class="person-name">Dr. {{ payment.doctorName ?? '—' }}</p>
                <p class="person-sub">{{ payment.doctorId ?? '' }}</p>
              </td>

              <!-- Amount -->
              <td class="td-amount">
                <span :class="{ refunded: payment.status === 'REFUNDED' }">
                  {{ formatCurrency(payment.amount) }}
                </span>
              </td>

              <!-- Method -->
              <td>
                <span v-if="payment.method" class="method-badge">{{ payment.method }}</span>
                <span v-else class="muted">—</span>
              </td>

              <!-- Status -->
              <td>
                <span
                  class="status-badge"
                  :style="{ background: statusMeta(payment.status).bg, color: statusMeta(payment.status).color }"
                >
                  {{ statusMeta(payment.status).label }}
                </span>
              </td>

              <!-- Date -->
              <td class="td-date">{{ formatDate(payment.createdAt) }}</td>

              <!-- Action -->
              <td class="td-action" @click.stop>
                <button
                  v-if="canRefund(payment.status)"
                  class="refund-btn"
                  @click="promptRefund(payment)"
                >
                  <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
                    <path d="M11 4H4.5L6.5 2M4.5 9H11L9 11" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M2 6.5H8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                  </svg>
                  Refund
                </button>
                <span v-else-if="payment.status === 'REFUNDED'" class="refunded-badge">
                  <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
                    <circle cx="6.5" cy="6.5" r="5.5" fill="#dbeafe"/>
                    <path d="M4 6.5L5.8 8.5L9.5 4.5" stroke="#1e40af" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  Refunded
                </span>
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
         Detail drawer
    ════════════════════════════════════ -->
    <Transition name="drawer">
      <div v-if="drawerPayment" class="drawer-overlay" @click.self="closeDrawer">
        <div class="drawer">
          <div class="drawer-header">
            <h3 class="drawer-title">Payment detail</h3>
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
                <span class="drawer-id">{{ drawerPayment.id }}</span>
                <span
                  class="status-badge"
                  :style="{ background: statusMeta(drawerPayment.status).bg, color: statusMeta(drawerPayment.status).color }"
                >{{ statusMeta(drawerPayment.status).label }}</span>
              </div>
            </div>

            <!-- Amount block -->
            <div class="drawer-section amount-section">
              <p class="amount-label">Amount</p>
              <p class="amount-value" :class="{ refunded: drawerPayment.status === 'REFUNDED' }">
                {{ formatCurrency(drawerPayment.amount) }}
              </p>
              <p v-if="drawerPayment.status === 'REFUNDED'" class="refunded-note">
                This payment has been refunded
              </p>
            </div>

            <!-- Transaction info -->
            <div class="drawer-section">
              <p class="drawer-section-title">Transaction</p>
              <div class="drawer-row">
                <span class="drawer-label">Method</span>
                <span class="drawer-value">{{ drawerPayment.method ?? '—' }}</span>
              </div>
              <div v-if="drawerPayment.transactionRef" class="drawer-row">
                <span class="drawer-label">Reference</span>
                <span class="drawer-value mono">{{ drawerPayment.transactionRef }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">Appointment</span>
                <span class="drawer-value mono small">{{ drawerPayment.appointmentId ?? '—' }}</span>
              </div>
            </div>

            <!-- Patient -->
            <div class="drawer-section">
              <p class="drawer-section-title">Patient</p>
              <div class="drawer-row">
                <span class="drawer-label">Name</span>
                <span class="drawer-value">{{ drawerPayment.patientName ?? '—' }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">ID</span>
                <span class="drawer-value mono">{{ drawerPayment.patientId ?? '—' }}</span>
              </div>
            </div>

            <!-- Doctor -->
            <div class="drawer-section">
              <p class="drawer-section-title">Doctor</p>
              <div class="drawer-row">
                <span class="drawer-label">Name</span>
                <span class="drawer-value">Dr. {{ drawerPayment.doctorName ?? '—' }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">ID</span>
                <span class="drawer-value mono">{{ drawerPayment.doctorId ?? '—' }}</span>
              </div>
            </div>

            <!-- Timestamps -->
            <div class="drawer-section no-border">
              <div class="drawer-row">
                <span class="drawer-label">Created</span>
                <span class="drawer-value">{{ formatDateTime(drawerPayment.createdAt) }}</span>
              </div>
              <div class="drawer-row">
                <span class="drawer-label">Updated</span>
                <span class="drawer-value">{{ formatDateTime(drawerPayment.updatedAt) }}</span>
              </div>
            </div>
          </div>

          <!-- Drawer footer -->
          <div class="drawer-footer">
            <button
              v-if="canRefund(drawerPayment.status)"
              class="drawer-refund-btn"
              @click="closeDrawer(); promptRefund(drawerPayment)"
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M12 4H5.5L7.5 2M5.5 10H12L10 12" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 7H8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              </svg>
              Issue refund
            </button>
            <div v-else-if="drawerPayment.status === 'REFUNDED'" class="refunded-footer-note">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <circle cx="7" cy="7" r="6" fill="#dbeafe"/>
                <path d="M4.5 7L6 8.5L9.5 5" stroke="#1e40af" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              Refund already processed
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ════════════════════════════════════
         Refund confirm modal
    ════════════════════════════════════ -->
    <Transition name="modal-fade">
      <div v-if="showRefundModal" class="modal-overlay" @click.self="closeRefundModal">
        <div class="modal">

          <div class="modal-icon">
            <svg width="26" height="26" viewBox="0 0 26 26" fill="none">
              <path d="M20 7H8.5L11.5 4M8.5 19H21L18 22" stroke="#4a9eff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M4 13H14" stroke="#4a9eff" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </div>

          <h3 class="modal-title">Confirm refund</h3>

          <div class="refund-amount-box">
            <p class="refund-amount-label">Refund amount</p>
            <p class="refund-amount-value">{{ formatCurrency(refundTarget?.amount ?? 0) }}</p>
          </div>

          <p class="modal-body">
            You are about to issue a refund for
            <strong>{{ refundTarget?.patientName ?? 'this patient' }}</strong>.
            <br/><br/>
            The payment will be marked as <strong>REFUNDED</strong> and the amount
            will be returned through the original payment gateway.
            This action cannot be undone.
          </p>

          <p v-if="refundError" class="modal-error">{{ refundError }}</p>

          <div class="modal-actions">
            <button class="modal-cancel" @click="closeRefundModal" :disabled="refundLoading">
              Cancel
            </button>
            <button class="modal-confirm-refund" :disabled="refundLoading" @click="confirmRefund">
              <span v-if="refundLoading" class="btn-spinner"></span>
              {{ refundLoading ? 'Processing…' : 'Confirm refund' }}
            </button>
          </div>

          <p class="modal-warning">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M6 1L11 10H1L6 1Z" stroke="#d97706" stroke-width="1.2" stroke-linejoin="round"/>
              <path d="M6 5V7M6 8.5V9" stroke="#d97706" stroke-width="1.2" stroke-linecap="round"/>
            </svg>
            This will trigger the payment gateway's refund API
          </p>
        </div>
      </div>
    </Transition>

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700&family=DM+Sans:wght@400;500&display=swap');

.payments-page {
  font-family: 'DM Sans', sans-serif;
  animation: fadeIn 0.3s ease;
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

/* ── Summary cards ───────────────────────────────────────── */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}
.summary-card {
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: box-shadow 0.2s;
}
.summary-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.summary-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.summary-label {
  font-size: 12px;
  color: #8a94a6;
  margin: 0 0 3px;
}
.summary-value {
  font-family: 'Syne', sans-serif;
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 2px;
  letter-spacing: -0.3px;
}
.summary-value.green  { color: #065f46; }
.summary-value.amber  { color: #92400e; }
.summary-value.blue   { color: #1e40af; }
.summary-value.purple { color: #3730a3; }
.summary-sub {
  font-size: 11px;
  color: #9aa3b0;
  margin: 0;
}

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

/* ── Filters ─────────────────────────────────────────────── */
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
  min-width: 240px;
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
  width: 38px; height: 38px;
  display: flex; align-items: center; justify-content: center;
  background: white; border: 1px solid #eaecf0; border-radius: 9px;
  cursor: pointer; color: #5a6578; flex-shrink: 0;
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
.th-action { width: 100px; }

.data-row {
  border-bottom: 1px solid #f4f5f7;
  cursor: pointer;
  transition: background 0.12s;
}
.data-row:last-child { border-bottom: none; }
.data-row:hover { background: #f8f9fc; }

td { padding: 12px 14px; color: #3d4a5c; vertical-align: middle; }

/* ID cell */
.id-cell { display: flex; flex-direction: column; gap: 2px; }
.id-text { font-family: monospace; font-size: 12px; color: #5a6578; font-weight: 500; }
.txn-ref { font-size: 11px; color: #9aa3b0; font-family: monospace; }

/* Person cells */
.td-person { min-width: 160px; }
.person-cell { display: flex; align-items: center; gap: 8px; }
.mini-avatar {
  width: 30px; height: 30px;
  border-radius: 50%;
  font-size: 12px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.patient-av { background: rgba(74,158,255,0.12); color: #1a6fc4; }
.person-name { font-size: 13px; font-weight: 500; color: #0f2744; margin: 0 0 2px; white-space: nowrap; }
.person-sub  { font-size: 11px; color: #9aa3b0; font-family: monospace; margin: 0; }

/* Amount */
.td-amount { font-size: 14px; font-weight: 600; color: #065f46; white-space: nowrap; }
.td-amount .refunded { color: #9aa3b0; text-decoration: line-through; }

/* Method badge */
.method-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 9px;
  border-radius: 20px;
  background: rgba(139,92,246,0.08);
  color: #4338ca;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

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

.td-date { white-space: nowrap; color: #8a94a6; }
.td-action { width: 100px; }
.muted { color: #c4cbd6; }

/* Refund button */
.refund-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #1e40af;
  background: rgba(74,158,255,0.08);
  border: 1px solid rgba(74,158,255,0.22);
  border-radius: 7px;
  padding: 5px 10px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}
.refund-btn:hover { background: rgba(74,158,255,0.15); }

/* Refunded indicator */
.refunded-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  color: #1e40af;
}

/* Empty */
.empty-cell { text-align: center; padding: 56px 16px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-state p { font-size: 14px; color: #9aa3b0; margin: 0; }
.link-btn {
  font-family: 'DM Sans', sans-serif; font-size: 13px;
  color: #4a9eff; background: none; border: none; cursor: pointer; text-decoration: underline;
}

/* Pagination */
.pagination {
  display: flex; align-items: center; gap: 4px;
  padding: 12px 14px; border-top: 1px solid #f0f2f5;
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
  background: white; font-family: 'DM Sans', sans-serif;
  font-size: 13px; color: #3d4a5c; cursor: pointer; padding: 0 6px;
  transition: background 0.12s;
}
.page-num:hover { background: #f4f5f7; }
.page-num.active { background: #0f2744; color: white; border-color: #0f2744; font-weight: 500; }
.page-ellipsis { font-size: 13px; color: #9aa3b0; padding: 0 4px; }
.page-info { margin-left: auto; font-size: 12px; color: #9aa3b0; }

/* ════════════════════
   Detail drawer
════════════════════ */
.drawer-overlay {
  position: fixed; inset: 0;
  background: rgba(15,39,68,0.3);
  z-index: 150; display: flex; justify-content: flex-end;
}
.drawer {
  width: 380px; max-width: 100vw; height: 100%;
  background: white; display: flex; flex-direction: column;
  box-shadow: -8px 0 32px rgba(0,0,0,0.1);
}
.drawer-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px; border-bottom: 1px solid #eaecf0; flex-shrink: 0;
}
.drawer-title {
  font-family: 'Syne', sans-serif; font-size: 16px;
  font-weight: 700; color: #0f2744; margin: 0;
}
.drawer-close {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none; cursor: pointer;
  color: #8a94a6; border-radius: 6px; transition: background 0.15s;
}
.drawer-close:hover { background: #f4f5f7; color: #3d4a5c; }
.drawer-body { flex: 1; overflow-y: auto; padding: 0 24px; }
.drawer-section { padding: 16px 0; border-bottom: 1px solid #f4f5f7; }
.drawer-section.no-border { border-bottom: none; }
.drawer-section-title {
  font-size: 11px; font-weight: 600; color: #9aa3b0;
  text-transform: uppercase; letter-spacing: 0.06em; margin: 0 0 10px;
}
.drawer-id-row {
  display: flex; align-items: flex-start;
  justify-content: space-between; gap: 10px;
}
.drawer-id { font-size: 11px; font-family: monospace; color: #5a6578; word-break: break-all; flex: 1; }
.drawer-row {
  display: flex; align-items: center;
  justify-content: space-between; padding: 5px 0; gap: 12px;
}
.drawer-label { font-size: 12px; color: #9aa3b0; flex-shrink: 0; }
.drawer-value { font-size: 13px; color: #0f2744; font-weight: 500; text-align: right; }
.drawer-value.mono { font-family: monospace; font-size: 12px; }
.drawer-value.small { font-size: 11px; }

/* Amount section */
.amount-section { text-align: center; padding: 24px 0; }
.amount-label { font-size: 12px; color: #9aa3b0; text-transform: uppercase; letter-spacing: 0.06em; margin: 0 0 6px; }
.amount-value {
  font-family: 'Syne', sans-serif;
  font-size: 28px; font-weight: 800;
  color: #065f46; margin: 0;
  letter-spacing: -0.5px;
}
.amount-value.refunded { color: #9aa3b0; text-decoration: line-through; }
.refunded-note { font-size: 12px; color: #1e40af; margin: 6px 0 0; }

.drawer-footer {
  padding: 16px 24px; border-top: 1px solid #eaecf0; flex-shrink: 0;
}
.drawer-refund-btn {
  width: 100%; height: 42px;
  display: flex; align-items: center; justify-content: center; gap: 7px;
  font-family: 'DM Sans', sans-serif; font-size: 14px; font-weight: 500;
  color: #1e40af;
  background: rgba(74,158,255,0.08);
  border: 1px solid rgba(74,158,255,0.25);
  border-radius: 10px; cursor: pointer;
  transition: background 0.15s;
}
.drawer-refund-btn:hover { background: rgba(74,158,255,0.15); }
.refunded-footer-note {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  font-size: 13px; color: #1e40af; padding: 6px 0;
}

/* ════════════════════
   Refund modal
════════════════════ */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(15,39,68,0.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 200; padding: 16px;
}
.modal {
  background: white; border-radius: 16px; padding: 32px;
  width: 100%; max-width: 420px;
  display: flex; flex-direction: column; align-items: center;
  text-align: center; gap: 14px;
}
.modal-icon {
  width: 56px; height: 56px; border-radius: 50%;
  background: rgba(74,158,255,0.1);
  display: flex; align-items: center; justify-content: center;
}
.modal-title {
  font-family: 'Syne', sans-serif;
  font-size: 18px; font-weight: 700; color: #0f2744; margin: 0;
}
.refund-amount-box {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
  padding: 14px 28px;
  width: 100%;
}
.refund-amount-label { font-size: 11px; color: #0369a1; text-transform: uppercase; letter-spacing: 0.06em; margin: 0 0 4px; }
.refund-amount-value {
  font-family: 'Syne', sans-serif;
  font-size: 24px; font-weight: 800;
  color: #0f2744; margin: 0; letter-spacing: -0.5px;
}
.modal-body {
  font-size: 14px; color: #5a6578; line-height: 1.65; margin: 0;
}
.modal-error {
  font-size: 13px; color: #c0392b;
  background: #fdf2f2; border: 1px solid #f5c6c6;
  border-radius: 8px; padding: 8px 14px;
  width: 100%; margin: 0; text-align: left;
}
.modal-warning {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: #d97706; margin: 0;
}
.modal-actions { display: flex; gap: 10px; width: 100%; }
.modal-cancel {
  flex: 1; height: 42px;
  font-family: 'DM Sans', sans-serif; font-size: 14px; font-weight: 500;
  color: #3d4a5c; background: white; border: 1px solid #eaecf0;
  border-radius: 10px; cursor: pointer; transition: background 0.15s;
}
.modal-cancel:hover:not(:disabled) { background: #f4f5f7; }
.modal-cancel:disabled { opacity: 0.5; cursor: not-allowed; }
.modal-confirm-refund {
  flex: 1; height: 42px;
  display: flex; align-items: center; justify-content: center; gap: 7px;
  font-family: 'DM Sans', sans-serif; font-size: 14px; font-weight: 500;
  color: white; background: #4a9eff;
  border: none; border-radius: 10px; cursor: pointer;
  transition: background 0.15s;
}
.modal-confirm-refund:hover:not(:disabled) { background: #2d87f0; }
.modal-confirm-refund:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-spinner {
  width: 13px; height: 13px;
  border: 2px solid rgba(255,255,255,0.35);
  border-top-color: white; border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ── Transitions ─────────────────────────────────────────── */
.toast-slide-enter-active, .toast-slide-leave-active { transition: all 0.25s ease; }
.toast-slide-enter-from, .toast-slide-leave-to { opacity: 0; transform: translateX(20px); }

.drawer-enter-active, .drawer-leave-active { transition: opacity 0.25s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-active .drawer, .drawer-leave-active .drawer { transition: transform 0.25s ease; }
.drawer-enter-from .drawer { transform: translateX(100%); }
.drawer-leave-to .drawer   { transform: translateX(100%); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .modal { transition: transform 0.2s ease; }
.modal-fade-enter-from .modal { transform: scale(0.96) translateY(8px); }

/* ── Responsive ──────────────────────────────────────────── */
@media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px)  { .summary-grid { grid-template-columns: 1fr; } }
</style>