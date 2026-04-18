<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'

// ── Types ─────────────────────────────────────────────────
interface Patient {
  id: string
  patientId: string
  authId: string
  email: string
  firstName: string
  lastName: string
  phone: string
  dateOfBirth: string
  gender: string
  bloodGroup: string
  createdAt: string
  address?: { street: string; city: string; country: string }
}

interface PatientDocument {
  id: string
  documentId: string
  fileName: string
  contentType: string
  uploadedAt: string
  fileSize?: number
}

interface Appointment {
  id: string
  patientId: string
  doctorName: string
  type: 'PHYSICAL' | 'VIDEO'
  status: string
  appointmentDate: string
  consultationFee: number | { video?: number; physical?: number } | null
  hospitalName?: string
}

// ── State ─────────────────────────────────────────────────
const route = useRoute()
const router = useRouter()

const patientId = route.params.patientId as string

const patient = ref<Patient | null>(null)
const documents = ref<PatientDocument[]>([])
const appointments = ref<Appointment[]>([])

const loadingPatient = ref(true)
const loadingDocs = ref(true)
const loadingAppts = ref(true)
const error = ref('')

// ── Fetch ─────────────────────────────────────────────────
onMounted(async () => {
  await Promise.allSettled([fetchPatient(), fetchDocuments(), fetchAppointments()])
})

async function fetchPatient() {
  loadingPatient.value = true
  try {
    const res = await api.get(`/patients/${patientId}`)
    patient.value = res.data
  } catch (e: any) {
    if (e.response?.status === 404) error.value = 'Patient not found.'
    else error.value = 'Failed to load patient.'
  } finally {
    loadingPatient.value = false
  }
}

async function fetchDocuments() {
  loadingDocs.value = true
  try {
    const res = await api.get(`/patients/${patientId}/documents`)
    documents.value = Array.isArray(res.data) ? res.data : []
  } catch {
    documents.value = []
  } finally {
    loadingDocs.value = false
  }
}

async function fetchAppointments() {
  loadingAppts.value = true
  try {
    // Fetch all appointments and filter by patientId client-side
    const res = await api.get('/appointments')
    const all = Array.isArray(res.data) ? res.data : (res.data?.content ?? [])
    appointments.value = all
      .filter((a: any) => a.patientId === patientId)
      .sort((a: any, b: any) =>
        new Date(b.appointmentDate ?? 0).getTime() - new Date(a.appointmentDate ?? 0).getTime()
      )
  } catch {
    appointments.value = []
  } finally {
    loadingAppts.value = false
  }
}

// ── Helpers ────────────────────────────────────────────────
function fullName(p: Patient) {
  return [p.firstName, p.lastName].filter(Boolean).join(' ') || '—'
}

function initials(p: Patient) {
  return [(p.firstName ?? '')[0], (p.lastName ?? '')[0]].filter(Boolean).join('').toUpperCase() || '?'
}

function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

function formatSize(bytes?: number) {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function fileIcon(contentType: string) {
  if (contentType?.includes('pdf')) return 'pdf'
  if (contentType?.includes('image')) return 'img'
  return 'file'
}

const statusColors: Record<string, string> = {
  PENDING: '#f59e0b',
  CONFIRMED: '#10b981',
  ACCEPTED_PENDING_PAYMENT: '#4a9eff',
  COMPLETED: '#6366f1',
  CANCELLED: '#ef4444',
  REJECTED: '#9ca3af',
}

function statusColor(s: string) {
  return statusColors[s] ?? '#9ca3af'
}

function statusLabel(s: string) {
  return s?.replace(/_/g, ' ') ?? '—'
}

function age(dob: string) {
  if (!dob) return null
  const diff = Date.now() - new Date(dob).getTime()
  return Math.floor(diff / (365.25 * 24 * 60 * 60 * 1000))
}

function appointmentFee(appt: Appointment) {
  if (typeof appt.consultationFee === 'number' && Number.isFinite(appt.consultationFee)) {
    return appt.consultationFee
  }

  const fee = appt.consultationFee
  if (!fee || typeof fee !== 'object') return 0

  const preferred = appt.type === 'VIDEO' ? fee.video : fee.physical
  if (typeof preferred === 'number' && Number.isFinite(preferred)) return preferred

  const fallback = typeof fee.physical === 'number' ? fee.physical : fee.video
  return Number.isFinite(fallback) ? fallback : 0
}
</script>

<template>
  <div class="detail-page">

    <!-- Back -->
    <button class="back-btn" @click="router.push('/patients')">
      <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
        <path d="M8.5 3L5 7L8.5 11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      Back to patients
    </button>

    <!-- Loading -->
    <div v-if="loadingPatient" class="skeleton-profile"></div>

    <!-- Error -->
    <div v-else-if="error" class="error-banner">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <circle cx="8" cy="8" r="7" stroke="#c0392b" stroke-width="1.4"/>
        <path d="M8 5v3.5M8 10.5v.5" stroke="#c0392b" stroke-width="1.4" stroke-linecap="round"/>
      </svg>
      {{ error }}
    </div>

    <template v-else-if="patient">

      <!-- ── Profile card ── -->
      <div class="profile-card">
        <div class="profile-left">
          <div class="profile-avatar">{{ initials(patient) }}</div>
          <div class="profile-info">
            <h2 class="profile-name">{{ fullName(patient) }}</h2>
            <p class="profile-id">{{ patient.patientId }}</p>
            <div class="profile-tags">
              <span v-if="patient.gender" class="tag tag-gender">{{ patient.gender }}</span>
              <span v-if="patient.bloodGroup" class="tag tag-blood">{{ patient.bloodGroup }}</span>
              <span v-if="patient.dateOfBirth" class="tag tag-age">
                {{ age(patient.dateOfBirth) }} yrs
              </span>
            </div>
          </div>
        </div>

        <div class="profile-meta-grid">
          <div class="meta-item">
            <p class="meta-label">Email</p>
            <p class="meta-value">{{ patient.email ?? '—' }}</p>
          </div>
          <div class="meta-item">
            <p class="meta-label">Phone</p>
            <p class="meta-value mono">{{ patient.phone ?? '—' }}</p>
          </div>
          <div class="meta-item">
            <p class="meta-label">Date of birth</p>
            <p class="meta-value">{{ formatDate(patient.dateOfBirth) }}</p>
          </div>
          <div class="meta-item">
            <p class="meta-label">Location</p>
            <p class="meta-value">
              {{ [patient.address?.city, patient.address?.country].filter(Boolean).join(', ') || '—' }}
            </p>
          </div>
          <div class="meta-item">
            <p class="meta-label">Registered</p>
            <p class="meta-value">{{ formatDate(patient.createdAt) }}</p>
          </div>
          <div class="meta-item">
            <p class="meta-label">Auth ID</p>
            <p class="meta-value mono small">{{ patient.authId ?? '—' }}</p>
          </div>
        </div>
      </div>

      <!-- ── Two-column lower ── -->
      <div class="lower-grid">

        <!-- Documents -->
        <div class="panel">
          <div class="panel-header">
            <h3 class="panel-title">
              Medical documents
              <span class="count-badge">{{ documents.length }}</span>
            </h3>
          </div>

          <div v-if="loadingDocs" class="loading-rows">
            <div v-for="i in 3" :key="i" class="loading-row"></div>
          </div>
          <div v-else-if="documents.length === 0" class="empty-state">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect x="5" y="3" width="18" height="22" rx="2" stroke="#d0d5dd" stroke-width="1.4"/>
              <path d="M9 9H19M9 13H19M9 17H15" stroke="#d0d5dd" stroke-width="1.4" stroke-linecap="round"/>
            </svg>
            <p>No documents uploaded.</p>
          </div>
          <div v-else class="doc-list">
            <div v-for="doc in documents" :key="doc.id" class="doc-row">
              <div class="doc-icon-wrap" :class="fileIcon(doc.contentType)">
                <!-- PDF -->
                <svg v-if="fileIcon(doc.contentType) === 'pdf'" width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <rect x="2" y="1" width="12" height="14" rx="1.5" stroke="currentColor" stroke-width="1.3"/>
                  <path d="M5 6H11M5 9H11M5 12H8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                </svg>
                <!-- Image -->
                <svg v-else-if="fileIcon(doc.contentType) === 'img'" width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <rect x="1" y="2" width="14" height="12" rx="2" stroke="currentColor" stroke-width="1.3"/>
                  <circle cx="5.5" cy="6.5" r="1.5" stroke="currentColor" stroke-width="1.3"/>
                  <path d="M1 10.5L5 7.5L8 10L11 7.5L15 11.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <!-- Generic -->
                <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <rect x="2" y="1" width="12" height="14" rx="1.5" stroke="currentColor" stroke-width="1.3"/>
                  <path d="M5 6H11M5 9H11M5 12H8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                </svg>
              </div>
              <div class="doc-body">
                <p class="doc-name">{{ doc.fileName ?? 'Unknown file' }}</p>
                <p class="doc-meta">
                  {{ formatDate(doc.uploadedAt) }}
                  <span v-if="doc.fileSize"> · {{ formatSize(doc.fileSize) }}</span>
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Appointment history -->
        <div class="panel">
          <div class="panel-header">
            <h3 class="panel-title">
              Appointment history
              <span class="count-badge">{{ appointments.length }}</span>
            </h3>
          </div>

          <div v-if="loadingAppts" class="loading-rows">
            <div v-for="i in 4" :key="i" class="loading-row"></div>
          </div>
          <div v-else-if="appointments.length === 0" class="empty-state">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect x="4" y="5" width="20" height="20" rx="2" stroke="#d0d5dd" stroke-width="1.4"/>
              <path d="M9 3V7M19 3V7M4 11H24" stroke="#d0d5dd" stroke-width="1.4" stroke-linecap="round"/>
            </svg>
            <p>No appointments found.</p>
          </div>
          <div v-else class="appt-list">
            <div v-for="appt in appointments" :key="appt.id" class="appt-row">
              <div class="appt-type-badge" :class="appt.type === 'VIDEO' ? 'video' : 'physical'">
                <svg v-if="appt.type === 'VIDEO'" width="12" height="12" viewBox="0 0 12 12" fill="none">
                  <rect x="1" y="2.5" width="7" height="7" rx="1" stroke="currentColor" stroke-width="1.2"/>
                  <path d="M8 5L11 3.5V8.5L8 7V5Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
                </svg>
                <svg v-else width="12" height="12" viewBox="0 0 12 12" fill="none">
                  <path d="M6 1C6 1 2 3.5 2 6.5C2 8.43 3.79 10 6 10C8.21 10 10 8.43 10 6.5C10 3.5 6 1 6 1Z" stroke="currentColor" stroke-width="1.2"/>
                </svg>
                {{ appt.type === 'VIDEO' ? 'Video' : 'In-person' }}
              </div>
              <div class="appt-body">
                <p class="appt-doctor">Dr. {{ appt.doctorName ?? '—' }}</p>
                <p class="appt-meta">
                  {{ formatDate(appt.appointmentDate) }}
                  <span v-if="appt.hospitalName"> · {{ appt.hospitalName }}</span>
                </p>
              </div>
              <div class="appt-right">
                <span
                  class="status-badge"
                  :style="{ background: statusColor(appt.status) + '18', color: statusColor(appt.status) }"
                >{{ statusLabel(appt.status) }}</span>
                <p class="appt-fee">LKR {{ appointmentFee(appt).toLocaleString() }}</p>
              </div>
            </div>
          </div>
        </div>

      </div>
    </template>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700&family=DM+Sans:wght@400;500&display=swap');

.detail-page {
  font-family: 'DM Sans', sans-serif;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Back btn ────────────────────────────────────────────── */
.back-btn {
  display: inline-flex;
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
  margin-bottom: 20px;
  transition: background 0.15s;
}
.back-btn:hover { background: #f4f5f7; }

/* ── Skeleton ─────────────────────────────────────────────── */
.skeleton-profile {
  height: 160px;
  background: linear-gradient(90deg, #eaecf0 25%, #f4f5f7 50%, #eaecf0 75%);
  background-size: 200% 100%;
  border-radius: 12px;
  margin-bottom: 20px;
  animation: shimmer 1.4s infinite;
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Error ─────────────────────────────────────────────────── */
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

/* ── Profile card ────────────────────────────────────────── */
.profile-card {
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
  align-items: flex-start;
}
.profile-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(74,158,255,0.12);
  color: #4a9eff;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Syne', sans-serif;
}
.profile-name {
  font-family: 'Syne', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #0f2744;
  margin: 0 0 4px;
}
.profile-id {
  font-size: 12px;
  color: #9aa3b0;
  font-family: monospace;
  margin: 0 0 10px;
}
.profile-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.tag {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 9px;
  border-radius: 20px;
}
.tag-gender { background: rgba(74,158,255,0.1); color: #1a6fc4; }
.tag-blood  { background: rgba(239,68,68,0.08); color: #b91c1c; font-family: monospace; }
.tag-age    { background: rgba(139,92,246,0.08); color: #6d28d9; }

/* Meta grid */
.profile-meta-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px 24px;
  min-width: 0;
}
.meta-item {}
.meta-label {
  font-size: 11px;
  color: #9aa3b0;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 3px;
}
.meta-value {
  font-size: 14px;
  color: #0f2744;
  font-weight: 500;
  margin: 0;
  word-break: break-all;
}
.meta-value.mono { font-family: monospace; font-size: 13px; }
.meta-value.small { font-size: 11px; color: #9aa3b0; }

/* ── Lower grid ──────────────────────────────────────────── */
.lower-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
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
  margin-bottom: 16px;
}
.panel-title {
  font-family: 'Syne', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #0f2744;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.count-badge {
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  font-weight: 600;
  background: #f0f2f5;
  color: #8a94a6;
  padding: 2px 7px;
  border-radius: 20px;
}

/* Loading rows */
.loading-rows { display: flex; flex-direction: column; gap: 8px; }
.loading-row {
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f8f9fb 25%, #eef0f3 50%, #f8f9fb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 0;
  text-align: center;
}
.empty-state p { font-size: 13px; color: #9aa3b0; margin: 0; }

/* ── Documents ───────────────────────────────────────────── */
.doc-list { display: flex; flex-direction: column; gap: 4px; }
.doc-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background 0.12s;
}
.doc-row:hover { background: #f8f9fc; }
.doc-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.doc-icon-wrap.pdf  { background: rgba(239,68,68,0.1);  color: #b91c1c; }
.doc-icon-wrap.img  { background: rgba(74,158,255,0.1); color: #1a6fc4; }
.doc-icon-wrap.file { background: rgba(139,92,246,0.1); color: #6d28d9; }

.doc-body { flex: 1; min-width: 0; }
.doc-name {
  font-size: 13px;
  font-weight: 500;
  color: #0f2744;
  margin: 0 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.doc-meta {
  font-size: 11px;
  color: #9aa3b0;
  margin: 0;
}

/* ── Appointments ────────────────────────────────────────── */
.appt-list { display: flex; flex-direction: column; gap: 4px; }
.appt-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f4f5f7;
}
.appt-row:last-child { border-bottom: none; }

.appt-type-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  padding: 4px 9px;
  border-radius: 20px;
  white-space: nowrap;
  flex-shrink: 0;
}
.appt-type-badge.video    { background: rgba(99,102,241,0.1); color: #4338ca; }
.appt-type-badge.physical { background: rgba(16,185,129,0.1); color: #065f46; }

.appt-body { flex: 1; min-width: 0; }
.appt-doctor {
  font-size: 13px;
  font-weight: 500;
  color: #0f2744;
  margin: 0 0 2px;
}
.appt-meta {
  font-size: 11px;
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
.status-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 20px;
  white-space: nowrap;
  text-transform: capitalize;
}
.appt-fee {
  font-size: 11px;
  color: #9aa3b0;
  margin: 0;
}

/* ── Responsive ──────────────────────────────────────────── */
@media (max-width: 900px) {
  .lower-grid { grid-template-columns: 1fr; }
  .profile-meta-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .profile-card { flex-direction: column; }
  .profile-meta-grid { grid-template-columns: 1fr 1fr; }
}
</style>