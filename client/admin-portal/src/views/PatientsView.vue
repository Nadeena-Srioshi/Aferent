<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
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
  address?: {
    city: string
    country: string
  }
}

// ── State ─────────────────────────────────────────────────
const router = useRouter()
const patients = ref<Patient[]>([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const genderFilter = ref('')
const sortKey = ref<'name' | 'email' | 'createdAt'>('createdAt')
const sortDir = ref<'asc' | 'desc'>('desc')
const page = ref(1)
const pageSize = 10

// ── Fetch ─────────────────────────────────────────────────
onMounted(fetchPatients)

async function fetchPatients() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get('/patients')
    const data = res.data
    patients.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch {
    error.value = 'Failed to load patients.'
  } finally {
    loading.value = false
  }
}

// ── Filtering / sorting / pagination ──────────────────────
const filtered = computed(() => {
  let list = [...patients.value]

  const q = search.value.trim().toLowerCase()
  if (q) {
    list = list.filter(p =>
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(q) ||
      p.email?.toLowerCase().includes(q) ||
      p.patientId?.toLowerCase().includes(q) ||
      p.phone?.includes(q)
    )
  }

  if (genderFilter.value) {
    list = list.filter(p => p.gender === genderFilter.value)
  }

  list.sort((a, b) => {
    let av = '', bv = ''
    if (sortKey.value === 'name') {
      av = `${a.firstName} ${a.lastName}`
      bv = `${b.firstName} ${b.lastName}`
    } else if (sortKey.value === 'email') {
      av = a.email ?? ''
      bv = b.email ?? ''
    } else {
      av = a.createdAt ?? ''
      bv = b.createdAt ?? ''
    }
    return sortDir.value === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av)
  })

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))

const paginated = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

// Reset to page 1 when search/filter changes
function onSearchInput() { page.value = 1 }

function toggleSort(key: typeof sortKey.value) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
  page.value = 1
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

function avatarColor(patientId: string) {
  const colors = ['#4a9eff', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4']
  let hash = 0
  for (const c of (patientId ?? '')) hash = c.charCodeAt(0) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function goToDetail(p: Patient) {
  router.push(`/patients/${p.patientId}`)
}
</script>

<template>
  <div class="patients-page">

    <!-- ── Header ── -->
    <div class="page-header">
      <div>
        <p class="page-meta">{{ filtered.length }} patient{{ filtered.length !== 1 ? 's' : '' }} found</p>
      </div>
      <button class="refresh-btn" @click="fetchPatients" :disabled="loading" title="Refresh">
        <svg width="15" height="15" viewBox="0 0 15 15" fill="none" :class="{ spinning: loading }">
          <path d="M13 7.5A5.5 5.5 0 112.5 4.5M2.5 1.5V4.5H5.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
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
          placeholder="Search by name, email, ID or phone…"
        />
        <button v-if="search" class="clear-btn" @click="search = ''; onSearchInput()">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M2 2L10 10M10 2L2 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <select v-model="genderFilter" @change="onSearchInput" class="filter-select">
        <option value="">All genders</option>
        <option value="MALE">Male</option>
        <option value="FEMALE">Female</option>
        <option value="OTHER">Other</option>
      </select>
    </div>

    <!-- ── Loading skeleton ── -->
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
      <button @click="fetchPatients" class="retry-btn">Retry</button>
    </div>

    <!-- ── Table ── -->
    <div v-else class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th class="th-patient sortable" @click="toggleSort('name')">
                Patient
                <span class="sort-icon" :class="{ active: sortKey === 'name' }">
                  {{ sortKey === 'name' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th class="th-email sortable" @click="toggleSort('email')">
                Email
                <span class="sort-icon" :class="{ active: sortKey === 'email' }">
                  {{ sortKey === 'email' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th>Phone</th>
              <th>Gender</th>
              <th>Blood group</th>
              <th class="sortable" @click="toggleSort('createdAt')">
                Registered
                <span class="sort-icon" :class="{ active: sortKey === 'createdAt' }">
                  {{ sortKey === 'createdAt' ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
                </span>
              </th>
              <th class="th-action">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginated.length === 0">
              <td colspan="7" class="empty-cell">
                <div class="empty-state">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                    <circle cx="16" cy="10" r="5" stroke="#d0d5dd" stroke-width="1.5"/>
                    <path d="M4 29C4 23.477 9.373 19 16 19C22.627 19 28 23.477 28 29" stroke="#d0d5dd" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                  <p>No patients match your search.</p>
                </div>
              </td>
            </tr>
            <tr
              v-for="patient in paginated"
              :key="patient.id"
              class="data-row"
              @click="goToDetail(patient)"
            >
              <!-- Patient name + ID -->
              <td class="td-patient">
                <div class="patient-cell">
                  <div class="avatar" :style="{ background: avatarColor(patient.patientId) + '20', color: avatarColor(patient.patientId) }">
                    {{ initials(patient) }}
                  </div>
                  <div>
                    <p class="patient-name">{{ fullName(patient) }}</p>
                    <p class="patient-id">{{ patient.patientId ?? '—' }}</p>
                  </div>
                </div>
              </td>

              <td class="td-email">{{ patient.email ?? '—' }}</td>
              <td class="td-mono">{{ patient.phone ?? '—' }}</td>

              <!-- Gender pill -->
              <td>
                <span v-if="patient.gender" class="gender-pill" :class="patient.gender?.toLowerCase()">
                  {{ patient.gender }}
                </span>
                <span v-else class="muted">—</span>
              </td>

              <!-- Blood group -->
              <td>
                <span v-if="patient.bloodGroup" class="blood-badge">{{ patient.bloodGroup }}</span>
                <span v-else class="muted">—</span>
              </td>

              <td class="td-date">{{ formatDate(patient.createdAt) }}</td>

              <!-- Action -->
              <td class="td-action" @click.stop>
                <button class="view-btn" @click="goToDetail(patient)">
                  View
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <path d="M2.5 6H9.5M7 3.5L9.5 6L7 8.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ── Pagination ── -->
      <div class="pagination" v-if="totalPages > 1">
        <button
          class="page-btn"
          :disabled="page === 1"
          @click="page--"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M8.5 3L5 7L8.5 11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>

        <div class="page-numbers">
          <template v-for="p in totalPages" :key="p">
            <button
              v-if="p === 1 || p === totalPages || Math.abs(p - page) <= 1"
              class="page-num"
              :class="{ active: p === page }"
              @click="page = p"
            >{{ p }}</button>
            <span
              v-else-if="Math.abs(p - page) === 2"
              class="page-ellipsis"
            >…</span>
          </template>
        </div>

        <button
          class="page-btn"
          :disabled="page === totalPages"
          @click="page++"
        >
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

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700&family=DM+Sans:wght@400;500&display=swap');

.patients-page {
  font-family: 'DM Sans', sans-serif;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Page header ─────────────────────────────────────────── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-meta {
  font-size: 13px;
  color: #8a94a6;
  margin: 0;
}
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
  padding: 0 36px 0 36px;
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
  min-width: 140px;
}
.filter-select:focus {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.1);
}

/* ── Skeleton ─────────────────────────────────────────────── */
.skeleton-table {
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  overflow: hidden;
}
.skeleton-header {
  height: 44px;
  background: #f4f5f7;
}
.skeleton-row {
  height: 56px;
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
.table-card {
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  overflow: hidden;
}
.table-wrap {
  overflow-x: auto;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

/* Head */
thead tr {
  background: #f8f9fc;
  border-bottom: 1px solid #eaecf0;
}
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
.sort-icon {
  margin-left: 4px;
  font-size: 10px;
  color: #bbc4d0;
}
.sort-icon.active { color: #4a9eff; }

/* Body rows */
.data-row {
  border-bottom: 1px solid #f4f5f7;
  cursor: pointer;
  transition: background 0.12s;
}
.data-row:last-child { border-bottom: none; }
.data-row:hover { background: #f8f9fc; }

td {
  padding: 13px 16px;
  color: #3d4a5c;
  vertical-align: middle;
}

/* Patient cell */
.td-patient { min-width: 200px; }
.patient-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.patient-name {
  font-size: 13px;
  font-weight: 500;
  color: #0f2744;
  margin: 0 0 2px;
  white-space: nowrap;
}
.patient-id {
  font-size: 11px;
  color: #9aa3b0;
  margin: 0;
  font-family: monospace;
}

.td-email {
  color: #4a9eff;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.td-mono { font-family: monospace; font-size: 12px; }
.td-date { white-space: nowrap; color: #8a94a6; }
.muted { color: #c4cbd6; }

/* Gender pill */
.gender-pill {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 9px;
  border-radius: 20px;
}
.gender-pill.male   { background: rgba(74,158,255,0.1);  color: #1a6fc4; }
.gender-pill.female { background: rgba(236,72,153,0.1);  color: #be185d; }
.gender-pill.other  { background: rgba(139,92,246,0.1);  color: #6d28d9; }

/* Blood badge */
.blood-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 5px;
  background: rgba(239,68,68,0.08);
  color: #b91c1c;
  font-family: monospace;
}

/* Action */
.td-action { width: 80px; }
.view-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #4a9eff;
  background: rgba(74,158,255,0.08);
  border: 1px solid rgba(74,158,255,0.2);
  border-radius: 7px;
  padding: 5px 10px;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.view-btn:hover { background: rgba(74,158,255,0.15); }

/* Empty state */
.empty-cell { text-align: center; padding: 48px 16px; }
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
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
  width: 32px;
  height: 32px;
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

.page-numbers {
  display: flex;
  align-items: center;
  gap: 2px;
}
.page-num {
  min-width: 32px;
  height: 32px;
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
.page-num.active {
  background: #0f2744;
  color: white;
  border-color: #0f2744;
  font-weight: 500;
}
.page-ellipsis {
  font-size: 13px;
  color: #9aa3b0;
  padding: 0 4px;
}
.page-info {
  margin-left: auto;
  font-size: 12px;
  color: #9aa3b0;
}
</style>