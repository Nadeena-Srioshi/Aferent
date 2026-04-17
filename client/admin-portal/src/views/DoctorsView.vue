<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'

// ── Types ─────────────────────────────────────────────────
type VerificationAction = 'APPROVE' | 'REJECT'
type DoctorStatus = 'PENDING_PROFILE' | 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED'

interface Doctor {
  id: string
  doctorId: string
  authId: string
  email: string
  firstName: string
  lastName: string
  phone: string
  specialization: string
  qualifications: string[]
  hospitalAffiliations: string[]
  status: DoctorStatus
  consultationFee: number
  createdAt: string
  bio?: string
  gender?: string
}

// ── State ─────────────────────────────────────────────────
const doctors = ref<Doctor[]>([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const statusFilter = ref<'' | 'pending' | 'verified' | 'rejected'>('')
const sortKey = ref<'name' | 'specialization' | 'createdAt'>('createdAt')
const sortDir = ref<'asc' | 'desc'>('desc')
const page = ref(1)
const pageSize = 10

// Verify modal state
const verifyingId = ref<string | null>(null)
const verifySuccess = ref<string | null>(null)
const verifyError = ref<string | null>(null)
const showConfirm = ref(false)
const pendingVerifyDoctor = ref<Doctor | null>(null)
const pendingAction = ref<VerificationAction>('APPROVE')
const rejectionReason = ref('')

// ── Fetch ─────────────────────────────────────────────────
onMounted(fetchDoctors)

async function fetchDoctors() {
  loading.value = true
  error.value = ''
  verifyError.value = null

  try {
    const [activeResult, pendingResult] = await Promise.allSettled([
      api.get('/doctors'),
      api.get('/admin/doctors/pending'),
    ])

    const activeDoctors = activeResult.status === 'fulfilled'
      ? parseDoctorList(activeResult.value.data)
      : []
    const pendingDoctors = pendingResult.status === 'fulfilled'
      ? parseDoctorList(pendingResult.value.data)
      : []

    if (activeResult.status === 'rejected' && pendingResult.status === 'rejected') {
      throw new Error('Unable to load doctors from both endpoints')
    }

    const merged = mergeByDoctorId([...activeDoctors, ...pendingDoctors]).filter(isEligibleForAdminList)
    doctors.value = merged

    if (activeResult.status === 'rejected' || pendingResult.status === 'rejected') {
      verifyError.value = 'Loaded partial doctor data. Some status groups may be incomplete.'
    }
  } catch {
    error.value = 'Failed to load doctors.'
  } finally {
    loading.value = false
  }
}

// ── Filtering / sorting / pagination ──────────────────────
const filtered = computed(() => {
  let list = [...doctors.value]

  const q = search.value.trim().toLowerCase()
  if (q) {
    list = list.filter(d =>
      `${d.firstName} ${d.lastName}`.toLowerCase().includes(q) ||
      d.email?.toLowerCase().includes(q) ||
      d.specialization?.toLowerCase().includes(q) ||
      d.doctorId?.toLowerCase().includes(q)
    )
  }

  if (statusFilter.value === 'verified') list = list.filter(isVerified)
  if (statusFilter.value === 'pending') list = list.filter(isPendingVerification)
  if (statusFilter.value === 'rejected') list = list.filter(isRejected)

  list.sort((a, b) => {
    let av = '', bv = ''
    if (sortKey.value === 'name') {
      av = `${a.firstName} ${a.lastName}`
      bv = `${b.firstName} ${b.lastName}`
    } else if (sortKey.value === 'specialization') {
      av = a.specialization ?? ''
      bv = b.specialization ?? ''
    } else {
      av = a.createdAt ?? ''
      bv = b.createdAt ?? ''
    }
    return sortDir.value === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av)
  })

  return list
})

const pendingCount = computed(() => doctors.value.filter(isPendingVerification).length)
const totalPages   = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const paginated    = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

function onSearchInput() { page.value = 1 }

function toggleSort(key: typeof sortKey.value) {
  if (sortKey.value === key) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortKey.value = key; sortDir.value = 'asc' }
  page.value = 1
}

// ── Verify ────────────────────────────────────────────────
function promptVerify(doctor: Doctor, action: VerificationAction) {
  pendingVerifyDoctor.value = doctor
  pendingAction.value = action
  rejectionReason.value = ''
  showConfirm.value = true
  verifySuccess.value = null
  verifyError.value = null
}

function cancelVerify() {
  if (verifyingId.value) return
  showConfirm.value = false
  pendingVerifyDoctor.value = null
  rejectionReason.value = ''
}

async function confirmVerify() {
  if (!pendingVerifyDoctor.value) return
  if (pendingAction.value === 'REJECT' && !rejectionReason.value.trim()) {
    verifyError.value = 'Please provide a reason before rejecting this doctor.'
    return
  }

  const doctor = pendingVerifyDoctor.value
  verifyingId.value = doctor.id
  verifySuccess.value = null
  verifyError.value = null

  try {
    const payload = pendingAction.value === 'REJECT'
      ? { action: 'REJECT', reason: rejectionReason.value.trim() }
      : { action: 'APPROVE' }

    await api.patch(`/admin/doctors/${doctor.doctorId}/verify`, payload)

    const idx = doctors.value.findIndex(d => d.id === doctor.id)
    if (idx !== -1) {
      doctors.value[idx].status = pendingAction.value === 'REJECT' ? 'SUSPENDED' : 'ACTIVE'
    }

    verifySuccess.value = pendingAction.value === 'REJECT'
      ? `Dr. ${fullName(doctor)} has been rejected.`
      : `Dr. ${fullName(doctor)} has been verified successfully.`
    setTimeout(() => { verifySuccess.value = null }, 4000)
    showConfirm.value = false
  } catch (e: any) {
    verifyError.value = e.response?.data?.message ?? 'Verification failed. Please try again.'
  } finally {
    verifyingId.value = null
    pendingVerifyDoctor.value = null
    rejectionReason.value = ''
  }
}

// ── Helpers ────────────────────────────────────────────────
function parseDoctorList(data: unknown): Doctor[] {
  const rawList = Array.isArray(data)
    ? data
    : Array.isArray((data as { content?: unknown[] })?.content)
      ? (data as { content: unknown[] }).content
      : []

  return rawList.map(normalizeDoctor)
}

function normalizeDoctor(raw: any): Doctor {
  const mappedStatus = normalizeStatus(raw)
  return {
    id: raw.id ?? '',
    doctorId: raw.doctorId ?? '',
    authId: raw.authId ?? '',
    email: raw.email ?? '',
    firstName: raw.firstName ?? '',
    lastName: raw.lastName ?? '',
    phone: raw.phone ?? '',
    specialization: raw.specialization ?? '',
    qualifications: Array.isArray(raw.qualifications) ? raw.qualifications : [],
    hospitalAffiliations: Array.isArray(raw.hospitalAffiliations)
      ? raw.hospitalAffiliations
      : Array.isArray(raw.hospitals)
        ? raw.hospitals
        : [],
    status: mappedStatus,
    consultationFee: Number(raw.consultationFee ?? 0),
    createdAt: raw.createdAt ?? '',
    bio: raw.bio,
    gender: raw.gender,
  }
}

function normalizeStatus(raw: any): DoctorStatus {
  const status = String(raw?.status ?? '').toUpperCase()
  if (status === 'ACTIVE' || status === 'PENDING_VERIFICATION' || status === 'SUSPENDED' || status === 'PENDING_PROFILE') {
    return status
  }

  if (typeof raw?.verified === 'boolean') {
    return raw.verified ? 'ACTIVE' : 'PENDING_VERIFICATION'
  }

  return 'PENDING_VERIFICATION'
}

function mergeByDoctorId(list: Doctor[]): Doctor[] {
  const byId = new Map<string, Doctor>()

  for (const doctor of list) {
    const key = doctor.doctorId || doctor.id
    if (!key) continue

    const existing = byId.get(key)
    if (!existing) {
      byId.set(key, doctor)
      continue
    }

    if (doctor.status === 'PENDING_VERIFICATION' || existing.status !== 'PENDING_VERIFICATION') {
      byId.set(key, doctor)
    }
  }

  return Array.from(byId.values())
}

function isEligibleForAdminList(doctor: Doctor) {
  return doctor.status === 'ACTIVE' || doctor.status === 'PENDING_VERIFICATION' || doctor.status === 'SUSPENDED'
}

function isPendingVerification(doctor: Doctor) {
  return doctor.status === 'PENDING_VERIFICATION'
}

function isVerified(doctor: Doctor) {
  return doctor.status === 'ACTIVE'
}

function isRejected(doctor: Doctor) {
  return doctor.status === 'SUSPENDED'
}

function statusLabel(doctor: Doctor) {
  if (isVerified(doctor)) return 'Verified'
  if (isRejected(doctor)) return 'Rejected'
  return 'Pending'
}

function statusClass(doctor: Doctor) {
  if (isVerified(doctor)) return 'verified'
  if (isRejected(doctor)) return 'rejected'
  return 'pending'
}

function fullName(d: Doctor) {
  return [d.firstName, d.lastName].filter(Boolean).join(' ') || '—'
}
function initials(d: Doctor) {
  return [(d.firstName ?? '')[0], (d.lastName ?? '')[0]].filter(Boolean).join('').toUpperCase() || '?'
}
function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}
function avatarColor(id: string) {
  const colors = ['#10b981', '#4a9eff', '#8b5cf6', '#f59e0b', '#06b6d4', '#ec4899']
  let hash = 0
  for (const c of (id ?? '')) hash = c.charCodeAt(0) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}
function formatFee(fee: number) {
  if (!fee) return '—'
  return 'LKR ' + fee.toLocaleString()
}
</script>

<template>
  <div class="doctors-page">

    <!-- ── Pending verification banner ── -->
    <Transition name="slide-down">
      <div v-if="pendingCount > 0 && !loading" class="pending-banner">
        <div class="pending-banner-left">
          <div class="pending-dot"></div>
          <span>
            <strong>{{ pendingCount }}</strong>
            doctor{{ pendingCount > 1 ? 's' : '' }} awaiting verification
          </span>
        </div>
        <button
          v-if="statusFilter !== 'pending'"
          class="banner-filter-btn"
          @click="statusFilter = 'pending'; onSearchInput()"
        >
          Show pending only
        </button>
        <button
          v-else
          class="banner-filter-btn"
          @click="statusFilter = ''; onSearchInput()"
        >
          Show all
        </button>
      </div>
    </Transition>

    <!-- ── Success / error toasts ── -->
    <Transition name="fade-down">
      <div v-if="verifySuccess" class="toast toast-success">
        <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="7.5" cy="7.5" r="6.5" stroke="currentColor" stroke-width="1.3"/>
          <path d="M4.5 7.5L6.5 9.5L10.5 5.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        {{ verifySuccess }}
      </div>
    </Transition>
    <Transition name="fade-down">
      <div v-if="verifyError" class="toast toast-error">
        <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="7.5" cy="7.5" r="6.5" stroke="currentColor" stroke-width="1.3"/>
          <path d="M7.5 4.5V8M7.5 10v.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
        </svg>
        {{ verifyError }}
        <button class="toast-close" @click="verifyError = null">×</button>
      </div>
    </Transition>

    <!-- ── Page header ── -->
    <div class="page-header">
      <p class="page-meta">{{ filtered.length }} doctor{{ filtered.length !== 1 ? 's' : '' }} found</p>
      <button class="refresh-btn" @click="fetchDoctors" :disabled="loading">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" :class="{ spinning: loading }">
          <path d="M13 7A6 6 0 112 4M2 1v3h3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        Refresh
      </button>
    </div>

    <!-- ── Filters ── -->
    <div class="filters-bar">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.4"/>
          <path d="M10.5 10.5L13.5 13.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
        <input
          v-model="search"
          @input="onSearchInput"
          class="search-input"
          type="text"
          placeholder="Search by name, email, specialization or ID…"
        />
        <button v-if="search" class="clear-btn" @click="search = ''; onSearchInput()">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M2 2L10 10M10 2L2 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <select v-model="statusFilter" @change="onSearchInput" class="filter-select">
        <option value="">All doctors</option>
        <option value="pending">Pending verification</option>
        <option value="verified">Verified only</option>
        <option value="rejected">Rejected only</option>
      </select>
    </div>

    <!-- ── Skeleton ── -->
    <div v-if="loading" class="skeleton-table">
      <div class="skeleton-header"></div>
      <div v-for="i in 7" :key="i" class="skeleton-row"></div>
    </div>

    <!-- ── Error ── -->
    <div v-else-if="error" class="error-banner">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <circle cx="8" cy="8" r="7" stroke="#c0392b" stroke-width="1.4"/>
        <path d="M8 5v3.5M8 10.5v.5" stroke="#c0392b" stroke-width="1.4" stroke-linecap="round"/>
      </svg>
      {{ error }}
      <button @click="fetchDoctors" class="retry-btn">Retry</button>
    </div>

    <!-- ── Table ── -->
    <div v-else class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th class="sortable" @click="toggleSort('name')">
                Doctor
                <span class="sort-icon" :class="{ active: sortKey === 'name' }">
                  {{ sortKey === 'name' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th class="sortable" @click="toggleSort('specialization')">
                Specialization
                <span class="sort-icon" :class="{ active: sortKey === 'specialization' }">
                  {{ sortKey === 'specialization' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th>Email</th>
              <th>Fee</th>
              <th class="sortable" @click="toggleSort('createdAt')">
                Registered
                <span class="sort-icon" :class="{ active: sortKey === 'createdAt' }">
                  {{ sortKey === 'createdAt' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th>Status</th>
              <th class="th-action">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginated.length === 0">
              <td colspan="7" class="empty-cell">
                <div class="empty-state">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                    <rect x="6" y="4" width="20" height="24" rx="3" stroke="#d0d5dd" stroke-width="1.5"/>
                    <path d="M11 12H21M11 16H21M11 20H16" stroke="#d0d5dd" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                  <p>No doctors match your search.</p>
                </div>
              </td>
            </tr>

            <tr v-for="doctor in paginated" :key="doctor.id" class="data-row" :class="{ pending: isPendingVerification(doctor) }">
              <!-- Doctor name + ID -->
              <td class="td-doctor">
                <div class="doctor-cell">
                  <div class="avatar" :style="{ background: avatarColor(doctor.doctorId) + '20', color: avatarColor(doctor.doctorId) }">
                    {{ initials(doctor) }}
                  </div>
                  <div>
                    <p class="doctor-name">Dr. {{ fullName(doctor) }}</p>
                    <p class="doctor-id">{{ doctor.doctorId ?? '—' }}</p>
                  </div>
                </div>
              </td>

              <!-- Specialization -->
              <td>
                <span v-if="doctor.specialization" class="spec-pill">
                  {{ doctor.specialization }}
                </span>
                <span v-else class="muted">—</span>
              </td>

              <!-- Email -->
              <td class="td-email">{{ doctor.email ?? '—' }}</td>

              <!-- Fee -->
              <td class="td-fee">{{ formatFee(doctor.consultationFee) }}</td>

              <!-- Registered date -->
              <td class="td-date">{{ formatDate(doctor.createdAt) }}</td>

              <!-- Status -->
              <td>
                <span class="verify-status" :class="statusClass(doctor)">
                  <span class="status-dot"></span>
                  {{ statusLabel(doctor) }}
                </span>
              </td>

              <!-- Action -->
              <td class="td-action" @click.stop>
                <div v-if="isPendingVerification(doctor)" class="action-group">
                  <button
                    class="action-btn approve"
                    :disabled="verifyingId === doctor.id"
                    @click="promptVerify(doctor, 'APPROVE')"
                  >
                    <span v-if="verifyingId === doctor.id" class="spinner"></span>
                    <template v-else>Approve</template>
                  </button>
                  <button
                    class="action-btn reject"
                    :disabled="verifyingId === doctor.id"
                    @click="promptVerify(doctor, 'REJECT')"
                  >
                    Reject
                  </button>
                </div>
                <span v-else-if="isVerified(doctor)" class="verified-tick">
                  <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
                    <circle cx="7.5" cy="7.5" r="6.5" fill="#d1fae5"/>
                    <path d="M4.5 7.5L6.5 9.5L10.5 5.5" stroke="#065f46" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </span>
                <span v-else class="rejected-label">Rejected</span>
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

    <!-- ── Confirm modal ── -->
    <Transition name="modal-fade">
      <div v-if="showConfirm" class="modal-overlay" @click.self="cancelVerify">
        <div class="modal">
          <div class="modal-icon" :class="{ danger: pendingAction === 'REJECT' }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" :stroke="pendingAction === 'REJECT' ? '#ef4444' : '#10b981'" stroke-width="1.5"/>
              <path
                v-if="pendingAction !== 'REJECT'"
                d="M7 12L10 15L17 8"
                stroke="#10b981"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                v-else
                d="M8 8L16 16M16 8L8 16"
                stroke="#ef4444"
                stroke-width="1.8"
                stroke-linecap="round"
              />
            </svg>
          </div>
          <h3 class="modal-title">
            {{ pendingAction === 'REJECT' ? 'Reject doctor registration' : 'Verify doctor registration' }}
          </h3>
          <p class="modal-body">
            You are about to {{ pendingAction === 'REJECT' ? 'reject' : 'verify' }}
            <strong>Dr. {{ pendingVerifyDoctor ? fullName(pendingVerifyDoctor) : '' }}</strong>
            ({{ pendingVerifyDoctor?.specialization ?? 'Unknown specialty' }}).
            <br/><br/>
            {{ pendingAction === 'REJECT'
              ? 'They will not be allowed to use the platform until re-approved by an admin.'
              : 'This will allow them to accept appointments and conduct consultations on the platform.' }}
          </p>
          <div v-if="pendingAction === 'REJECT'" class="reason-block">
            <label class="reason-label" for="reject-reason">Rejection reason</label>
            <textarea
              id="reject-reason"
              v-model="rejectionReason"
              class="reason-input"
              rows="3"
              placeholder="Provide a short reason for rejection"
            ></textarea>
          </div>
          <div class="modal-actions">
            <button class="modal-cancel" :disabled="!!verifyingId" @click="cancelVerify">Cancel</button>
            <button class="modal-confirm" :class="{ danger: pendingAction === 'REJECT' }" :disabled="!!verifyingId" @click="confirmVerify">
              <span v-if="verifyingId" class="spinner modal-spinner"></span>
              <template v-else>
                {{ pendingAction === 'REJECT' ? 'Confirm rejection' : 'Confirm verification' }}
              </template>
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700&family=DM+Sans:wght@400;500&display=swap');

.doctors-page {
  font-family: 'DM Sans', sans-serif;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Pending banner ──────────────────────────────────────── */
.pending-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #92400e;
  gap: 12px;
}
.pending-banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pending-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f59e0b;
  animation: pulse 2s infinite;
  flex-shrink: 0;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.6; transform: scale(1.3); }
}
.banner-filter-btn {
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #92400e;
  background: rgba(245,158,11,0.12);
  border: 1px solid rgba(245,158,11,0.3);
  border-radius: 6px;
  padding: 4px 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}
.banner-filter-btn:hover { background: rgba(245,158,11,0.2); }

/* ── Toasts ──────────────────────────────────────────────── */
.toast {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 14px;
}
.toast-success {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}
.toast-error {
  background: #fdf2f2;
  border: 1px solid #f5c6c6;
  color: #c0392b;
}
.toast-close {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: inherit;
  line-height: 1;
  padding: 0 2px;
}

/* ── Page header ─────────────────────────────────────────── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-meta { font-size: 13px; color: #8a94a6; margin: 0; }
.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #3d4a5c;
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 8px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.refresh-btn:hover:not(:disabled) { background: #f4f5f7; }
.refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.spinning { animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Filters ─────────────────────────────────────────────── */
.filters-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
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
  height: 40px;
  padding: 0 36px;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  color: #0f2744;
  background: white;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input::placeholder { color: #bbc4d0; }
.search-input:focus {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.1);
}
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
  border-radius: 4px;
}
.clear-btn:hover { color: #3d4a5c; }
.filter-select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #3d4a5c;
  background: white;
  outline: none;
  cursor: pointer;
  min-width: 180px;
}
.filter-select:focus {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.1);
}

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
.retry-btn:hover { background: #fdf2f2; }

/* ── Table card ──────────────────────────────────────────── */
.table-card { background: white; border: 1px solid #eaecf0; border-radius: 12px; overflow: hidden; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }

thead tr { background: #f8f9fc; border-bottom: 1px solid #eaecf0; }
th {
  padding: 11px 16px;
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
  transition: background 0.12s;
}
.data-row:last-child { border-bottom: none; }
.data-row:hover { background: #f8f9fc; }
.data-row.pending { background: #fffdf7; }
.data-row.pending:hover { background: #fffbeb; }

td { padding: 13px 16px; color: #3d4a5c; vertical-align: middle; }

/* Doctor cell */
.td-doctor { min-width: 200px; }
.doctor-cell { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.doctor-name { font-size: 13px; font-weight: 500; color: #0f2744; margin: 0 0 2px; white-space: nowrap; }
.doctor-id { font-size: 11px; color: #9aa3b0; margin: 0; font-family: monospace; }

/* Specialization pill */
.spec-pill {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  background: rgba(99,102,241,0.08);
  color: #4338ca;
  white-space: nowrap;
}

.td-email { color: #4a9eff; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.td-fee { font-weight: 500; color: #0f2744; white-space: nowrap; }
.td-date { white-space: nowrap; color: #8a94a6; }
.td-action { width: 120px; }
.muted { color: #c4cbd6; }

/* Verify status badge */
.verify-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 20px;
}
.verify-status.verified {
  background: rgba(16,185,129,0.1);
  color: #065f46;
}
.verify-status.pending {
  background: rgba(245,158,11,0.1);
  color: #92400e;
}
.verify-status.rejected {
  background: rgba(239,68,68,0.1);
  color: #991b1b;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.verify-status.verified .status-dot { background: #10b981; }
.verify-status.pending .status-dot  { background: #f59e0b; animation: pulse 2s infinite; }
.verify-status.rejected .status-dot { background: #ef4444; }

/* Action buttons */
.action-group {
  display: inline-flex;
  gap: 6px;
}
.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 500;
  border-radius: 7px;
  padding: 5px 10px;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.action-btn.approve {
  color: #065f46;
  background: rgba(16,185,129,0.1);
  border: 1px solid rgba(16,185,129,0.25);
}
.action-btn.approve:hover:not(:disabled) { background: rgba(16,185,129,0.18); }
.action-btn.reject {
  color: #991b1b;
  background: rgba(239,68,68,0.1);
  border: 1px solid rgba(239,68,68,0.25);
}
.action-btn.reject:hover:not(:disabled) { background: rgba(239,68,68,0.18); }
.action-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.verified-tick {
  display: inline-flex;
  align-items: center;
}
.rejected-label {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  color: #991b1b;
}

.spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(6,95,70,0.2);
  border-top-color: #065f46;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ── Empty state ─────────────────────────────────────────── */
.empty-cell { text-align: center; padding: 48px 16px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-state p { font-size: 14px; color: #9aa3b0; margin: 0; }

/* ── Pagination ──────────────────────────────────────────── */
.pagination {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 16px;
  border-top: 1px solid #f0f2f5;
}
.page-btn {
  width: 32px; height: 32px;
  border: 1px solid #eaecf0;
  border-radius: 7px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3d4a5c;
  transition: background 0.12s;
}
.page-btn:hover:not(:disabled) { background: #f4f5f7; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-numbers { display: flex; align-items: center; gap: 2px; }
.page-num {
  min-width: 32px; height: 32px;
  border: 1px solid #eaecf0;
  border-radius: 7px;
  background: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #3d4a5c;
  cursor: pointer;
  transition: background 0.12s;
  padding: 0 6px;
}
.page-num:hover { background: #f4f5f7; }
.page-num.active { background: #0f2744; color: white; border-color: #0f2744; font-weight: 500; }
.page-ellipsis { font-size: 13px; color: #9aa3b0; padding: 0 4px; }
.page-info { margin-left: auto; font-size: 12px; color: #9aa3b0; }

/* ── Confirm modal ───────────────────────────────────────── */
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
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
}
.modal-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(16,185,129,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}
.modal-icon.danger { background: rgba(239,68,68,0.1); }
.modal-title {
  font-family: 'Syne', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #0f2744;
  margin: 0;
}
.modal-body {
  font-size: 14px;
  color: #5a6578;
  line-height: 1.65;
  margin: 0;
}
.modal-actions {
  display: flex;
  gap: 10px;
  width: 100%;
  margin-top: 8px;
}
.reason-block {
  width: 100%;
  text-align: left;
}
.reason-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #3d4a5c;
  margin-bottom: 6px;
}
.reason-input {
  width: 100%;
  resize: vertical;
  min-height: 78px;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  padding: 9px 10px;
  color: #0f2744;
  outline: none;
}
.reason-input:focus {
  border-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239,68,68,0.12);
}
.modal-cancel {
  flex: 1;
  height: 42px;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #3d4a5c;
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.modal-cancel:hover { background: #f4f5f7; }
.modal-confirm {
  flex: 1;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: white;
  background: #10b981;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.modal-confirm:hover { background: #059669; }
.modal-confirm.danger { background: #ef4444; }
.modal-confirm.danger:hover { background: #dc2626; }
.modal-cancel:disabled,
.modal-confirm:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.modal-spinner {
  border: 2px solid rgba(255,255,255,0.35);
  border-top-color: white;
}

/* ── Transitions ─────────────────────────────────────────── */
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.25s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-8px); }

.fade-down-enter-active, .fade-down-leave-active { transition: all 0.2s ease; }
.fade-down-enter-from, .fade-down-leave-to { opacity: 0; transform: translateY(-5px); }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .modal, .modal-fade-leave-active .modal { transition: transform 0.2s ease; }
.modal-fade-enter-from .modal { transform: scale(0.95) translateY(8px); }
.modal-fade-leave-to .modal { transform: scale(0.95); }
</style>