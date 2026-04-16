<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'

interface Notification {
  id: string
  recipientEmail?: string
  recipientPhone?: string
  type: 'EMAIL' | 'SMS'
  event: string
  status: string
  message?: string
  createdAt: string
}

const notifications = ref<Notification[]>([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const dateFilter = ref('')
const page = ref(1)
const pageSize = 15
const sortDir = ref<'asc' | 'desc'>('desc')

onMounted(fetchNotifications)

async function fetchNotifications() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get('/notifications')
    const data = res.data
    notifications.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch {
    error.value = 'Failed to load notifications.'
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  let list = [...notifications.value]
  const q = search.value.trim().toLowerCase()
  if (q) {
    list = list.filter(n =>
      n.recipientEmail?.toLowerCase().includes(q) ||
      n.recipientPhone?.includes(q) ||
      n.event?.toLowerCase().includes(q) ||
      n.id?.toLowerCase().includes(q)
    )
  }
  if (typeFilter.value)   list = list.filter(n => n.type === typeFilter.value)
  if (statusFilter.value) list = list.filter(n => n.status === statusFilter.value)
  if (dateFilter.value)   list = list.filter(n => n.createdAt?.slice(0, 10) === dateFilter.value)
  list.sort((a, b) => {
    const av = a.createdAt ?? '', bv = b.createdAt ?? ''
    return sortDir.value === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av)
  })
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const paginated  = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))

function onFilterChange() { page.value = 1 }
function clearFilters() {
  search.value = ''; typeFilter.value = ''; statusFilter.value = ''; dateFilter.value = ''
  page.value = 1
}

const STATUS_META: Record<string, { color: string; bg: string }> = {
  SENT:    { color: '#065f46', bg: 'rgba(16,185,129,0.10)' },
  FAILED:  { color: '#991b1b', bg: 'rgba(239,68,68,0.10)'  },
  PENDING: { color: '#92400e', bg: 'rgba(245,158,11,0.10)' },
}
function statusMeta(s: string) {
  return STATUS_META[s] ?? { color: '#6b7280', bg: 'rgba(156,163,175,0.15)' }
}

function formatDateTime(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleString('en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

// Summary counts
const summary = computed(() => ({
  total:   notifications.value.length,
  sent:    notifications.value.filter(n => n.status === 'SENT').length,
  failed:  notifications.value.filter(n => n.status === 'FAILED').length,
  email:   notifications.value.filter(n => n.type === 'EMAIL').length,
  sms:     notifications.value.filter(n => n.type === 'SMS').length,
}))
</script>

<template>
  <div class="notif-page">

    <!-- Summary cards -->
    <div v-if="!loading && !error" class="summary-grid">
      <div class="summary-card">
        <div class="s-icon" style="background:rgba(74,158,255,0.1);color:#4a9eff">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <rect x="1" y="3" width="16" height="12" rx="2" stroke="currentColor" stroke-width="1.4"/>
            <path d="M1 6L9 11L17 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
          </svg>
        </div>
        <div><p class="s-label">Total sent</p><p class="s-val blue">{{ summary.total }}</p></div>
      </div>
      <div class="summary-card">
        <div class="s-icon" style="background:rgba(16,185,129,0.1);color:#10b981">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <circle cx="9" cy="9" r="7" stroke="currentColor" stroke-width="1.4"/>
            <path d="M5.5 9L7.5 11L12.5 6.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div><p class="s-label">Delivered</p><p class="s-val green">{{ summary.sent }}</p></div>
      </div>
      <div class="summary-card">
        <div class="s-icon" style="background:rgba(239,68,68,0.1);color:#ef4444">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <circle cx="9" cy="9" r="7" stroke="currentColor" stroke-width="1.4"/>
            <path d="M6 6L12 12M12 6L6 12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
          </svg>
        </div>
        <div><p class="s-label">Failed</p><p class="s-val red">{{ summary.failed }}</p></div>
      </div>
      <div class="summary-card">
        <div class="s-icon" style="background:rgba(139,92,246,0.1);color:#8b5cf6">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <rect x="3" y="2" width="12" height="15" rx="2" stroke="currentColor" stroke-width="1.4"/>
            <path d="M6 7H12M6 10H10" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
          </svg>
        </div>
        <div>
          <p class="s-label">By channel</p>
          <p class="s-val purple">{{ summary.email }} email · {{ summary.sms }} SMS</p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.4"/>
          <path d="M10.5 10.5L13.5 13.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
        <input v-model="search" @input="onFilterChange" class="search-input" type="text" placeholder="Search recipient, event or ID…"/>
        <button v-if="search" class="clear-btn" @click="search = ''; onFilterChange()">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M2 2L10 10M10 2L2 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <select v-model="typeFilter" @change="onFilterChange" class="filter-select">
        <option value="">All types</option>
        <option value="EMAIL">Email</option>
        <option value="SMS">SMS</option>
      </select>
      <select v-model="statusFilter" @change="onFilterChange" class="filter-select">
        <option value="">All statuses</option>
        <option value="SENT">Sent</option>
        <option value="FAILED">Failed</option>
        <option value="PENDING">Pending</option>
      </select>
      <input v-model="dateFilter" @change="onFilterChange" type="date" class="filter-select date-input"/>
      <button v-if="search || typeFilter || statusFilter || dateFilter" class="clear-all-btn" @click="clearFilters">Clear</button>
      <div class="spacer"></div>
      <button class="sort-toggle" @click="sortDir = sortDir === 'desc' ? 'asc' : 'desc'" :title="sortDir === 'desc' ? 'Newest first' : 'Oldest first'">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path d="M3 4H11M5 7H9M7 10H7" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
        {{ sortDir === 'desc' ? 'Newest first' : 'Oldest first' }}
      </button>
      <p class="result-count">{{ filtered.length }} result{{ filtered.length !== 1 ? 's' : '' }}</p>
      <button class="refresh-btn" @click="fetchNotifications" :disabled="loading" title="Refresh">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" :class="{ spinning: loading }">
          <path d="M13 7A6 6 0 112 4M2 1v3h3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="skeleton-table">
      <div class="skeleton-header"></div>
      <div v-for="i in 10" :key="i" class="skeleton-row"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-banner">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <circle cx="8" cy="8" r="7" stroke="#c0392b" stroke-width="1.4"/>
        <path d="M8 5v3.5M8 10.5v.5" stroke="#c0392b" stroke-width="1.4" stroke-linecap="round"/>
      </svg>
      {{ error }}
      <button @click="fetchNotifications" class="retry-btn">Retry</button>
    </div>

    <!-- Table -->
    <div v-else class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Recipient</th>
              <th>Event</th>
              <th>Status</th>
              <th>Sent at</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginated.length === 0">
              <td colspan="5" class="empty-cell">
                <div class="empty-state">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                    <path d="M16 3C16 3 7 8 7 16C7 20.418 11.134 24 16 24C20.866 24 25 20.418 25 16C25 8 16 3 16 3Z" stroke="#d0d5dd" stroke-width="1.5"/>
                    <path d="M16 24V16M12 18H20" stroke="#d0d5dd" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                  <p>No notifications match your filters.</p>
                  <button class="link-btn" @click="clearFilters">Clear filters</button>
                </div>
              </td>
            </tr>
            <tr v-for="n in paginated" :key="n.id" class="data-row">

              <!-- Type badge -->
              <td>
                <span class="type-badge" :class="n.type === 'EMAIL' ? 'email' : 'sms'">
                  <svg v-if="n.type === 'EMAIL'" width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <rect x="1" y="2" width="10" height="8" rx="1.5" stroke="currentColor" stroke-width="1.2"/>
                    <path d="M1 4L6 7L11 4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/>
                  </svg>
                  <svg v-else width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <rect x="3" y="1" width="6" height="10" rx="1.5" stroke="currentColor" stroke-width="1.2"/>
                    <circle cx="6" cy="9" r="0.8" fill="currentColor"/>
                  </svg>
                  {{ n.type }}
                </span>
              </td>

              <!-- Recipient -->
              <td class="td-recipient">
                <span class="recipient-val">
                  {{ n.type === 'EMAIL' ? (n.recipientEmail ?? '—') : (n.recipientPhone ?? '—') }}
                </span>
              </td>

              <!-- Event -->
              <td>
                <span class="event-tag">{{ n.event?.replace(/_/g, ' ') ?? '—' }}</span>
              </td>

              <!-- Status -->
              <td>
                <span
                  class="status-badge"
                  :style="{ background: statusMeta(n.status).bg, color: statusMeta(n.status).color }"
                >
                  {{ n.status }}
                </span>
              </td>

              <!-- Sent at -->
              <td class="td-date">{{ formatDateTime(n.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination" v-if="totalPages > 1">
        <button class="page-btn" :disabled="page === 1" @click="page--">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M8.5 3L5 7L8.5 11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="page-numbers">
          <template v-for="p in totalPages" :key="p">
            <button v-if="p === 1 || p === totalPages || Math.abs(p - page) <= 1"
              class="page-num" :class="{ active: p === page }" @click="page = p">{{ p }}</button>
            <span v-else-if="Math.abs(p - page) === 2" class="page-ellipsis">…</span>
          </template>
        </div>
        <button class="page-btn" :disabled="page === totalPages" @click="page++">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M5.5 3L9 7L5.5 11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <span class="page-info">
          {{ (page - 1) * pageSize + 1 }}–{{ Math.min(page * pageSize, filtered.length) }} of {{ filtered.length }}
        </span>
      </div>
    </div>

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700&family=DM+Sans:wght@400;500&display=swap');

.notif-page {
  font-family: 'DM Sans', sans-serif;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* Summary cards */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}
.summary-card {
  background: white;
  border: 1px solid #eaecf0;
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 13px;
}
.s-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.s-label { font-size: 12px; color: #8a94a6; margin: 0 0 3px; }
.s-val {
  font-family: 'Syne', sans-serif;
  font-size: 17px; font-weight: 700; margin: 0;
  letter-spacing: -0.3px;
}
.s-val.blue   { color: #1e40af; }
.s-val.green  { color: #065f46; }
.s-val.red    { color: #991b1b; }
.s-val.purple { color: #3730a3; font-size: 13px; font-family: 'DM Sans', sans-serif; font-weight: 500; }

/* Filters */
.filters-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  align-items: center;
  flex-wrap: wrap;
}
.search-wrap { position: relative; flex: 1; min-width: 220px; }
.search-icon {
  position: absolute; left: 12px; top: 50%;
  transform: translateY(-50%); color: #a0aab8; pointer-events: none;
}
.search-input {
  width: 100%; height: 38px;
  padding: 0 34px;
  border: 1px solid #eaecf0; border-radius: 9px;
  font-family: 'DM Sans', sans-serif; font-size: 13px;
  color: #0f2744; background: white; outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input::placeholder { color: #bbc4d0; }
.search-input:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.1); }
.clear-btn {
  position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
  background: none; border: none; cursor: pointer;
  color: #a0aab8; display: flex; align-items: center; padding: 4px;
}
.clear-btn:hover { color: #3d4a5c; }
.filter-select {
  height: 38px; padding: 0 10px;
  border: 1px solid #eaecf0; border-radius: 9px;
  font-family: 'DM Sans', sans-serif; font-size: 13px;
  color: #3d4a5c; background: white; outline: none; cursor: pointer;
}
.filter-select:focus { border-color: #4a9eff; box-shadow: 0 0 0 3px rgba(74,158,255,0.1); }
.date-input { min-width: 145px; }
.clear-all-btn {
  font-family: 'DM Sans', sans-serif; font-size: 12px;
  color: #ef4444; background: rgba(239,68,68,0.06);
  border: 1px solid rgba(239,68,68,0.2); border-radius: 8px;
  padding: 5px 12px; cursor: pointer; white-space: nowrap;
}
.clear-all-btn:hover { background: rgba(239,68,68,0.12); }
.spacer { flex: 1; }
.sort-toggle {
  display: flex; align-items: center; gap: 5px;
  font-family: 'DM Sans', sans-serif; font-size: 12px; font-weight: 500;
  color: #5a6578; background: white;
  border: 1px solid #eaecf0; border-radius: 8px;
  padding: 5px 12px; cursor: pointer; white-space: nowrap;
  transition: background 0.15s;
}
.sort-toggle:hover { background: #f4f5f7; }
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

/* Skeleton */
.skeleton-table { background: white; border: 1px solid #eaecf0; border-radius: 12px; overflow: hidden; }
.skeleton-header { height: 44px; background: #f4f5f7; }
.skeleton-row {
  height: 52px; border-top: 1px solid #f0f2f5;
  background: linear-gradient(90deg, #f8f9fb 25%, #eef0f3 50%, #f8f9fb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Error */
.error-banner {
  display: flex; align-items: center; gap: 10px;
  background: #fdf2f2; border: 1px solid #f5c6c6;
  border-radius: 10px; padding: 14px 18px;
  font-size: 14px; color: #c0392b;
}
.retry-btn {
  margin-left: auto; background: none;
  border: 1px solid #c0392b; color: #c0392b;
  border-radius: 6px; padding: 4px 12px;
  font-size: 13px; cursor: pointer; font-family: 'DM Sans', sans-serif;
}

/* Table */
.table-card { background: white; border: 1px solid #eaecf0; border-radius: 12px; overflow: hidden; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }

thead tr { background: #f8f9fc; border-bottom: 1px solid #eaecf0; }
th {
  padding: 11px 16px;
  text-align: left; font-size: 11px; font-weight: 600;
  color: #8a94a6; text-transform: uppercase;
  letter-spacing: 0.05em; white-space: nowrap;
}

.data-row { border-bottom: 1px solid #f4f5f7; transition: background 0.12s; }
.data-row:last-child { border-bottom: none; }
.data-row:hover { background: #f8f9fc; }

td { padding: 12px 16px; color: #3d4a5c; vertical-align: middle; }

/* Type badge */
.type-badge {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 600;
  padding: 4px 10px; border-radius: 20px;
  letter-spacing: 0.04em;
}
.type-badge.email { background: rgba(74,158,255,0.1);  color: #1e40af; }
.type-badge.sms   { background: rgba(139,92,246,0.1);  color: #4338ca; }

/* Recipient */
.td-recipient { max-width: 220px; }
.recipient-val { font-size: 13px; color: #0f2744; font-weight: 500; }

/* Event tag */
.event-tag {
  font-size: 12px; font-weight: 500;
  color: #5a6578; background: #f4f5f7;
  padding: 3px 9px; border-radius: 5px;
  text-transform: capitalize;
}

/* Status badge */
.status-badge {
  display: inline-flex; align-items: center;
  font-size: 11px; font-weight: 500;
  padding: 4px 9px; border-radius: 20px; white-space: nowrap;
}

.td-date { white-space: nowrap; color: #8a94a6; font-size: 12px; }

/* Empty */
.empty-cell { text-align: center; padding: 56px 16px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.empty-state p { font-size: 14px; color: #9aa3b0; margin: 0; }
.link-btn {
  font-family: 'DM Sans', sans-serif; font-size: 13px;
  color: #4a9eff; background: none; border: none;
  cursor: pointer; text-decoration: underline;
}

/* Pagination */
.pagination {
  display: flex; align-items: center; gap: 4px;
  padding: 12px 16px; border-top: 1px solid #f0f2f5;
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

@media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 600px)  { .summary-grid { grid-template-columns: 1fr; } }
</style>