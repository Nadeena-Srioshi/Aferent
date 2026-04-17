<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '@/api/axios'

// ── Types ────────────────────────────────────────────────
interface StatsState {
  totalPatients: number
  totalDoctors: number
  appointmentsToday: number
  totalRevenue: number
  pendingVerifications: number
  activeAppointments: number
}

interface RecentAppointment {
  id: string
  patientName: string
  doctorName: string
  type: 'PHYSICAL' | 'VIDEO'
  status: string
  appointmentDate: string
  consultationFee: number
}

interface RecentPayment {
  id: string
  patientName?: string
  amount: number
  status: string
  createdAt: string
}

// ── State ────────────────────────────────────────────────
const loading = ref(true)
const error = ref('')

const stats = ref<StatsState>({
  totalPatients: 0,
  totalDoctors: 0,
  appointmentsToday: 0,
  totalRevenue: 0,
  pendingVerifications: 0,
  activeAppointments: 0,
})

const recentAppointments = ref<RecentAppointment[]>([])
const recentPayments = ref<RecentPayment[]>([])

// Appointment status breakdown for mini bar chart
const statusBreakdown = ref<Record<string, number>>({})

// ── Fetch ────────────────────────────────────────────────
onMounted(async () => {
  await loadDashboard()
})

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    // Parallel fetches
    const [patientsRes, doctorsRes, appointmentsRes, paymentsRes] = await Promise.allSettled([
      api.get('/patients'),
      api.get('http://localhost:8080/doctors'),
      api.get('/appointments'),
      api.get('/payments'),
    ])

    // Patients
    if (patientsRes.status === 'fulfilled') {
      const data = patientsRes.value.data
      stats.value.totalPatients = Array.isArray(data) ? data.length : (data?.totalElements ?? 0)
    }

    // Doctors
    if (doctorsRes.status === 'fulfilled') {
      const data = doctorsRes.value.data
      const doctors = Array.isArray(data) ? data : (data?.content ?? [])
      stats.value.totalDoctors = doctors.length
      stats.value.pendingVerifications = doctors.filter((d: any) => !d.verified).length
    }

    // Appointments
    if (appointmentsRes.status === 'fulfilled' || appointmentsRes.status === 'rejected') {
      const data = appointmentsRes.value.data
      const appts = Array.isArray(data) ? data : (data?.content ?? [])

      const today = new Date().toISOString().slice(0, 10)
      stats.value.appointmentsToday = appts.filter(
        (a: any) => a.appointmentDate?.slice(0, 10) === today
      ).length

      stats.value.activeAppointments = appts.filter((a: any) =>
        ['PENDING', 'CONFIRMED', 'ACCEPTED_PENDING_PAYMENT'].includes(a.status)
      ).length

      // Status breakdown
      const breakdown: Record<string, number> = {}
      for (const a of appts) {
        breakdown[a.status] = (breakdown[a.status] ?? 0) + 1
      }
      statusBreakdown.value = breakdown

      // Recent 6 appointments (newest first)
      recentAppointments.value = [...appts]
        .sort((a: any, b: any) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
        .slice(0, 6)
    }

    // Payments
    if (paymentsRes.status === 'fulfilled') {
      const data = paymentsRes.value.data
      const payments = Array.isArray(data) ? data : (data?.content ?? [])

      stats.value.totalRevenue = payments
        .filter((p: any) => p.status === 'PAID' || p.status === 'COMPLETED')
        .reduce((sum: number, p: any) => sum + (p.amount ?? 0), 0)

      recentPayments.value = [...payments]
        .sort((a: any, b: any) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
        .slice(0, 5)
    }
  } catch (e) {
    error.value = 'Failed to load dashboard data.'
  } finally {
    loading.value = false
  }
}

// ── Computed ─────────────────────────────────────────────
const statCards = computed(() => [
  {
    label: 'Total patients',
    value: stats.value.totalPatients,
    icon: 'patient',
    color: '#4a9eff',
    bg: 'rgba(74,158,255,0.08)',
    suffix: '',
  },
  {
    label: 'Total doctors',
    value: stats.value.totalDoctors,
    icon: 'doctor',
    color: '#10b981',
    bg: 'rgba(16,185,129,0.08)',
    suffix: '',
  },
  {
    label: 'Appointments today',
    value: stats.value.appointmentsToday,
    icon: 'calendar',
    color: '#f59e0b',
    bg: 'rgba(245,158,11,0.08)',
    suffix: '',
  },
  {
    label: 'Total revenue',
    value: stats.value.totalRevenue,
    icon: 'payment',
    color: '#8b5cf6',
    bg: 'rgba(139,92,246,0.08)',
    prefix: 'LKR ',
    suffix: '',
  },
])

const statusColors: Record<string, string> = {
  PENDING: '#f59e0b',
  CONFIRMED: '#10b981',
  ACCEPTED_PENDING_PAYMENT: '#4a9eff',
  COMPLETED: '#6366f1',
  CANCELLED: '#ef4444',
  REJECTED: '#9ca3af',
}

const statusBreakdownList = computed(() => {
  const total = Object.values(statusBreakdown.value).reduce((a, b) => a + b, 0)
  return Object.entries(statusBreakdown.value)
    .sort((a, b) => b[1] - a[1])
    .map(([status, count]) => ({
      status,
      count,
      pct: total > 0 ? Math.round((count / total) * 100) : 0,
      color: statusColors[status] ?? '#9ca3af',
    }))
})

// ── Helpers ───────────────────────────────────────────────
function formatCurrency(val: number) {
  return 'LKR ' + val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(dateStr: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

function statusLabel(s: string) {
  return s.replace(/_/g, ' ')
}

function typeLabel(t: string) {
  return t === 'VIDEO' ? 'Video' : 'In-person'
}
</script>

<template>
  <div class="dashboard">

    <!-- Loading skeleton -->
    <div v-if="loading" class="skeleton-grid">
      <div v-for="i in 4" :key="i" class="skeleton-card"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-banner">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <circle cx="8" cy="8" r="7" stroke="#c0392b" stroke-width="1.4"/>
        <path d="M8 5v3.5M8 10.5v.5" stroke="#c0392b" stroke-width="1.4" stroke-linecap="round"/>
      </svg>
      {{ error }}
      <button @click="loadDashboard" class="retry-btn">Retry</button>
    </div>

    <!-- Content -->
    <template v-else>

      <!-- ── Stat cards ── -->
      <div class="stat-grid">
        <div v-for="card in statCards" :key="card.label" class="stat-card">
          <div class="stat-icon-wrap" :style="{ background: card.bg }">
            <!-- Patient icon -->
            <svg v-if="card.icon === 'patient'" width="20" height="20" viewBox="0 0 20 20" fill="none" :style="{ color: card.color }">
              <circle cx="10" cy="6" r="3.5" stroke="currentColor" stroke-width="1.5"/>
              <path d="M2 19C2 15.134 5.582 12 10 12C14.418 12 18 15.134 18 19" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <!-- Doctor icon -->
            <svg v-if="card.icon === 'doctor'" width="20" height="20" viewBox="0 0 20 20" fill="none" :style="{ color: card.color }">
              <rect x="4" y="3" width="12" height="15" rx="2" stroke="currentColor" stroke-width="1.5"/>
              <path d="M8 10H12M10 8V12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <!-- Calendar icon -->
            <svg v-if="card.icon === 'calendar'" width="20" height="20" viewBox="0 0 20 20" fill="none" :style="{ color: card.color }">
              <rect x="3" y="4" width="14" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/>
              <path d="M7 2V5M13 2V5M3 9H17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <!-- Payment icon -->
            <svg v-if="card.icon === 'payment'" width="20" height="20" viewBox="0 0 20 20" fill="none" :style="{ color: card.color }">
              <rect x="2" y="5" width="16" height="11" rx="2" stroke="currentColor" stroke-width="1.5"/>
              <path d="M2 9H18" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="6" cy="13" r="1" fill="currentColor"/>
            </svg>
          </div>
          <div class="stat-body">
            <p class="stat-label">{{ card.label }}</p>
            <p class="stat-value">
              <span v-if="card.prefix" class="stat-prefix">{{ card.prefix }}</span>
              {{ card.icon === 'payment'
                  ? stats.totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
                  : card.value }}
            </p>
          </div>
        </div>
      </div>

      <!-- ── Alert pills (pending verifications / active appointments) ── -->
      <div class="alert-row" v-if="stats.pendingVerifications > 0 || stats.activeAppointments > 0">
        <RouterLink v-if="stats.pendingVerifications > 0" to="/doctors" class="alert-pill alert-warn">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <circle cx="7" cy="7" r="6" stroke="currentColor" stroke-width="1.3"/>
            <path d="M7 4.5V7.5M7 9.5V10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
          </svg>
          {{ stats.pendingVerifications }} doctor{{ stats.pendingVerifications > 1 ? 's' : '' }} awaiting verification
          <span class="pill-arrow">→</span>
        </RouterLink>
        <RouterLink v-if="stats.activeAppointments > 0" to="/appointments" class="alert-pill alert-info">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <rect x="2" y="2.5" width="10" height="10" rx="1.5" stroke="currentColor" stroke-width="1.3"/>
            <path d="M5 1.5V3.5M9 1.5V3.5M2 6H12" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
          </svg>
          {{ stats.activeAppointments }} active appointment{{ stats.activeAppointments > 1 ? 's' : '' }}
          <span class="pill-arrow">→</span>
        </RouterLink>
      </div>

      <!-- ── Two-column lower section ── -->
      <div class="lower-grid">

        <!-- Recent appointments -->
        <div class="panel">
          <div class="panel-header">
            <h2 class="panel-title">Recent appointments</h2>
            <RouterLink to="/appointments" class="panel-link">View all →</RouterLink>
          </div>

          <div v-if="recentAppointments.length === 0" class="empty-state">
            No appointments yet.
          </div>

          <div v-else class="appt-list">
            <div v-for="appt in recentAppointments" :key="appt.id" class="appt-row">
              <div class="appt-avatar">
                {{ appt.patientName?.charAt(0) ?? '?' }}
              </div>
              <div class="appt-body">
                <p class="appt-names">
                  {{ appt.patientName ?? '—' }}
                  <span class="appt-with">with</span>
                  {{ appt.doctorName ?? '—' }}
                </p>
                <p class="appt-meta">
                  {{ formatDate(appt.appointmentDate) }} · {{ typeLabel(appt.type) }}
                </p>
              </div>
              <div class="appt-right">
                <span
                  class="status-badge"
                  :style="{ background: (statusColors[appt.status] ?? '#9ca3af') + '18', color: statusColors[appt.status] ?? '#9ca3af' }"
                >
                  {{ statusLabel(appt.status) }}
                </span>
                <p class="appt-fee">LKR {{ (appt.consultationFee ?? 0).toLocaleString() }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Right column: status breakdown + recent payments -->
        <div class="right-col">

          <!-- Appointment status breakdown -->
          <div class="panel" style="margin-bottom: 20px">
            <div class="panel-header">
              <h2 class="panel-title">Appointment status</h2>
            </div>
            <div v-if="statusBreakdownList.length === 0" class="empty-state">No data.</div>
            <div v-else class="breakdown-list">
              <div v-for="item in statusBreakdownList" :key="item.status" class="breakdown-row">
                <div class="breakdown-label">
                  <span class="breakdown-dot" :style="{ background: item.color }"></span>
                  <span class="breakdown-name">{{ statusLabel(item.status) }}</span>
                </div>
                <div class="breakdown-bar-wrap">
                  <div class="breakdown-bar" :style="{ width: item.pct + '%', background: item.color }"></div>
                </div>
                <span class="breakdown-count">{{ item.count }}</span>
              </div>
            </div>
          </div>

          <!-- Recent payments -->
          <div class="panel">
            <div class="panel-header">
              <h2 class="panel-title">Recent payments</h2>
              <RouterLink to="/payments" class="panel-link">View all →</RouterLink>
            </div>
            <div v-if="recentPayments.length === 0" class="empty-state">No payments yet.</div>
            <div v-else class="payment-list">
              <div v-for="pmt in recentPayments" :key="pmt.id" class="payment-row">
                <div class="payment-id">
                  <span class="id-label">{{ pmt.id?.slice(0, 8) ?? '—' }}</span>
                  <span class="payment-date">{{ formatDate(pmt.createdAt) }}</span>
                </div>
                <div class="payment-right">
                  <span
                    class="status-badge"
                    :style="{ background: (pmt.status === 'PAID' || pmt.status === 'COMPLETED' ? '#10b981' : '#f59e0b') + '18',
                              color: pmt.status === 'PAID' || pmt.status === 'COMPLETED' ? '#10b981' : '#f59e0b' }"
                  >{{ pmt.status }}</span>
                  <span class="payment-amount">{{ formatCurrency(pmt.amount ?? 0) }}</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>

    </template>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700;800&family=DM+Sans:wght@300;400;500&display=swap');

.dashboard {
  font-family: 'DM Sans', sans-serif;
  animation: fadeIn 0.35s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Skeleton ────────────────────────────────────────────── */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.skeleton-card {
  height: 100px;
  background: linear-gradient(90deg, #eaecf0 25%, #f4f5f7 50%, #eaecf0 75%);
  background-size: 200% 100%;
  border-radius: 12px;
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
.retry-btn:hover { background: #fdf2f2; }

/* ── Stat cards ──────────────────────────────────────────── */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: box-shadow 0.2s;
}
.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.stat-icon-wrap {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-body {}
.stat-label {
  font-size: 12px;
  color: #8a94a6;
  font-weight: 400;
  margin: 0 0 4px;
}
.stat-value {
  font-family: 'Syne', sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: #0f2744;
  margin: 0;
  letter-spacing: -0.5px;
  white-space: nowrap;
}
.stat-prefix {
  font-size: 13px;
  font-weight: 600;
  color: #8a94a6;
}

/* ── Alert pills ─────────────────────────────────────────── */
.alert-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.alert-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: opacity 0.15s;
}
.alert-pill:hover { opacity: 0.82; }
.alert-warn {
  background: rgba(245,158,11,0.1);
  color: #b45309;
  border: 1px solid rgba(245,158,11,0.25);
}
.alert-info {
  background: rgba(74,158,255,0.08);
  color: #1a6fc4;
  border: 1px solid rgba(74,158,255,0.2);
}
.pill-arrow { margin-left: 2px; }

/* ── Lower grid ──────────────────────────────────────────── */
.lower-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}

/* ── Panel ────────────────────────────────────────────────── */
.panel {
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  padding: 20px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.panel-title {
  font-family: 'Syne', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #0f2744;
  margin: 0;
}
.panel-link {
  font-size: 13px;
  color: #4a9eff;
  text-decoration: none;
  font-weight: 500;
}
.panel-link:hover { text-decoration: underline; }

.empty-state {
  font-size: 13px;
  color: #9aa3b0;
  padding: 16px 0;
  text-align: center;
}

/* ── Appointment list ────────────────────────────────────── */
.appt-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.appt-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f4f5f7;
}
.appt-row:last-child { border-bottom: none; }

.appt-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(74,158,255,0.1);
  color: #4a9eff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  text-transform: uppercase;
}
.appt-body { flex: 1; min-width: 0; }
.appt-names {
  font-size: 13px;
  font-weight: 500;
  color: #0f2744;
  margin: 0 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.appt-with {
  font-weight: 400;
  color: #8a94a6;
  margin: 0 3px;
}
.appt-meta {
  font-size: 12px;
  color: #9aa3b0;
  margin: 0;
}
.appt-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}
.appt-fee {
  font-size: 12px;
  color: #8a94a6;
  margin: 0;
}

/* Status badge */
.status-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 20px;
  white-space: nowrap;
  text-transform: capitalize;
}

/* ── Status breakdown ────────────────────────────────────── */
.breakdown-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.breakdown-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.breakdown-label {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 175px;
  flex-shrink: 0;
}
.breakdown-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.breakdown-name {
  font-size: 12px;
  color: #3d4a5c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-transform: capitalize;
}
.breakdown-bar-wrap {
  flex: 1;
  height: 5px;
  background: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
}
.breakdown-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
  min-width: 4px;
}
.breakdown-count {
  font-size: 12px;
  font-weight: 500;
  color: #0f2744;
  width: 24px;
  text-align: right;
  flex-shrink: 0;
}

/* ── Recent payments ─────────────────────────────────────── */
.payment-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.payment-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px solid #f4f5f7;
}
.payment-row:last-child { border-bottom: none; }
.payment-id {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.id-label {
  font-size: 12px;
  font-family: monospace;
  color: #3d4a5c;
  font-weight: 500;
}
.payment-date {
  font-size: 11px;
  color: #9aa3b0;
}
.payment-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}
.payment-amount {
  font-size: 13px;
  font-weight: 600;
  color: #0f2744;
}

/* ── Right column ─────────────────────────────────────────── */
.right-col {
  display: flex;
  flex-direction: column;
}

/* ── Responsive ──────────────────────────────────────────── */
@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 860px) {
  .lower-grid { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .stat-grid { grid-template-columns: 1fr; }
}
</style>