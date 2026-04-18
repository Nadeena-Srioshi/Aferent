<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { referenceApi, type Specialization } from '@/api/reference'

const specializations = ref<Specialization[]>([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const sortDir = ref<'asc' | 'desc'>('asc')

const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const editingId = ref<string | null>(null)
const formName = ref('')
const formMaxVideoFee = ref<number | null>(null)
const formMaxPhysicalFee = ref<number | null>(null)
const formError = ref('')
const formSubmitting = ref(false)

const showDeleteModal = ref(false)
const deletingSpecialization = ref<Specialization | null>(null)
const deleteSubmitting = ref(false)

onMounted(fetchSpecializations)

async function fetchSpecializations() {
  loading.value = true
  error.value = ''
  try {
    const res = await referenceApi.getAllSpecializations(true)
    specializations.value = res.data
  } catch {
    error.value = 'Failed to load specializations.'
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  let list = [...specializations.value]
  const q = search.value.trim().toLowerCase()
  if (q) {
    list = list.filter(s => s.name.toLowerCase().includes(q))
  }
  list.sort((a, b) => {
    const compare = a.name.localeCompare(b.name)
    return sortDir.value === 'asc' ? compare : -compare
  })
  return list
})

function openAddModal() {
  modalMode.value = 'add'
  editingId.value = null
  formName.value = ''
  formMaxVideoFee.value = null
  formMaxPhysicalFee.value = null
  formError.value = ''
  showModal.value = true
}

function openEditModal(specialization: Specialization) {
  modalMode.value = 'edit'
  editingId.value = specialization.id
  formName.value = specialization.name
  formMaxVideoFee.value = specialization.maxVideoConsultationFee ?? null
  formMaxPhysicalFee.value = specialization.maxPhysicalConsultationFee ?? null
  formError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  formName.value = ''
  formMaxVideoFee.value = null
  formMaxPhysicalFee.value = null
  formError.value = ''
  editingId.value = null
}

async function submitForm() {
  formError.value = ''
  const name = formName.value.trim()
  if (!name) {
    formError.value = 'Name is required'
    return
  }

  if (formMaxVideoFee.value === null || Number.isNaN(formMaxVideoFee.value) || formMaxVideoFee.value < 0) {
    formError.value = 'Max video consultation fee is required and must be 0 or greater.'
    return
  }

  if (formMaxPhysicalFee.value === null || Number.isNaN(formMaxPhysicalFee.value) || formMaxPhysicalFee.value < 0) {
    formError.value = 'Max physical consultation fee is required and must be 0 or greater.'
    return
  }

  formSubmitting.value = true
  try {
    const payload = {
      name,
      maxVideoConsultationFee: Number(formMaxVideoFee.value),
      maxPhysicalConsultationFee: Number(formMaxPhysicalFee.value),
    }

    if (modalMode.value === 'add') {
      await referenceApi.addSpecialization(payload)
    } else if (editingId.value) {
      await referenceApi.updateSpecialization(editingId.value, payload)
    }
    await fetchSpecializations()
    closeModal()
  } catch (err: any) {
    formError.value = err.response?.data?.message || 'Operation failed'
  } finally {
    formSubmitting.value = false
  }
}

function openDeleteModal(specialization: Specialization) {
  deletingSpecialization.value = specialization
  showDeleteModal.value = true
}

function closeDeleteModal() {
  showDeleteModal.value = false
  deletingSpecialization.value = null
}

async function confirmDelete() {
  if (!deletingSpecialization.value) return
  deleteSubmitting.value = true
  try {
    await referenceApi.deleteSpecialization(deletingSpecialization.value.id)
    await fetchSpecializations()
    closeDeleteModal()
  } catch {
    alert('Failed to delete specialization')
  } finally {
    deleteSubmitting.value = false
  }
}

function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

function formatFee(fee?: number) {
  if (fee == null) return '—'
  return `LKR ${fee.toLocaleString()}`
}
</script>

<template>
  <div class="specializations-page">

    <!-- ── Header ── -->
    <div class="page-header">
      <div>
        <p class="page-meta">{{ filtered.length }} specialization{{ filtered.length !== 1 ? 's' : '' }} found</p>
      </div>
      <button @click="openAddModal" class="add-btn">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path d="M7 1V13M1 7H13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
        </svg>
        Add Specialization
      </button>
    </div>

    <!-- ── Error ── -->
    <div v-if="error" class="error-banner">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <circle cx="8" cy="8" r="7" stroke="#c0392b" stroke-width="1.4"/>
        <path d="M8 5v3.5M8 10.5v.5" stroke="#c0392b" stroke-width="1.4" stroke-linecap="round"/>
      </svg>
      {{ error }}
    </div>

    <!-- ── Filters ── -->
    <div class="filters-bar">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none">
          <circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.4"/>
          <path d="M10.5 10.5L13.5 13.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
        <input v-model="search" type="text" placeholder="Search specializations…" class="search-input" />
        <button v-if="search" class="clear-btn" @click="search = ''">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M2 2L10 10M10 2L2 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <button @click="sortDir = sortDir === 'asc' ? 'desc' : 'asc'" class="sort-btn">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path d="M3 5L7 1L11 5M3 9L7 13L11 9" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        {{ sortDir === 'asc' ? 'A→Z' : 'Z→A' }}
      </button>
    </div>

    <!-- ── Loading skeleton ── -->
    <div v-if="loading" class="skeleton-table">
      <div class="skeleton-header"></div>
      <div v-for="i in 6" :key="i" class="skeleton-row"></div>
    </div>

    <!-- ── Table ── -->
    <div v-else class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Specialization Name</th>
              <th>Max Video Fee (15 min)</th>
              <th>Max Physical Fee (15 min)</th>
              <th>Created Date</th>
              <th class="th-action">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filtered.length === 0">
              <td colspan="5" class="empty-cell">
                <div class="empty-state">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                    <path d="M16 4L19 13H28L21 19L23 28L16 22L9 28L11 19L4 13H13L16 4Z" stroke="#d0d5dd" stroke-width="1.5" stroke-linejoin="round"/>
                  </svg>
                  <p>{{ search ? 'No specializations match your search.' : 'No specializations yet.' }}</p>
                </div>
              </td>
            </tr>
            <tr v-for="spec in filtered" :key="spec.id" class="data-row">
              <td class="td-name">{{ spec.name }}</td>
              <td class="td-fee">{{ formatFee(spec.maxVideoConsultationFee) }}</td>
              <td class="td-fee">{{ formatFee(spec.maxPhysicalConsultationFee) }}</td>
              <td class="td-date">{{ spec.createdAt ? formatDate(spec.createdAt) : '—' }}</td>
              <td class="td-action">
                <button @click="openEditModal(spec)" class="action-btn edit-btn">Edit</button>
                <button @click="openDeleteModal(spec)" class="action-btn delete-btn">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── Add/Edit Modal ── -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-card">
        <div class="modal-header">
          <h2>{{ modalMode === 'add' ? 'Add Specialization' : 'Edit Specialization' }}</h2>
          <button @click="closeModal" class="modal-close">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M2 2L12 12M12 2L2 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="formError" class="form-error">{{ formError }}</div>
          <div class="form-group">
            <label>Specialization Name</label>
            <input v-model="formName" type="text" placeholder="Enter specialization name" @keyup.enter="submitForm" />
          </div>
          <div class="form-group">
            <label>Max Video Consultation Fee (15 min)</label>
            <input v-model.number="formMaxVideoFee" type="number" min="0" step="0.01" placeholder="e.g. 3500" />
          </div>
          <div class="form-group">
            <label>Max Physical Consultation Fee (15 min)</label>
            <input v-model.number="formMaxPhysicalFee" type="number" min="0" step="0.01" placeholder="e.g. 4500" />
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeModal" class="btn-secondary" :disabled="formSubmitting">Cancel</button>
          <button @click="submitForm" class="btn-primary" :disabled="formSubmitting">
            {{ formSubmitting ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ── Delete Modal ── -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-card">
        <div class="modal-header">
          <h2>Delete Specialization</h2>
          <button @click="closeDeleteModal" class="modal-close">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M2 2L12 12M12 2L2 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <p class="delete-warning">Are you sure you want to delete <strong>{{ deletingSpecialization?.name }}</strong>? This action cannot be undone.</p>
        </div>
        <div class="modal-footer">
          <button @click="closeDeleteModal" class="btn-secondary" :disabled="deleteSubmitting">Cancel</button>
          <button @click="confirmDelete" class="btn-danger" :disabled="deleteSubmitting">
            {{ deleteSubmitting ? 'Deleting…' : 'Delete' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@700&family=DM+Sans:wght@400;500&display=swap');

.specializations-page {
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
.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: white;
  background: #4a9eff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.add-btn:hover { background: #3a8eef; }

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
  margin-bottom: 16px;
}

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

.sort-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  color: #3d4a5c;
  background: white;
  cursor: pointer;
  transition: background 0.15s;
}
.sort-btn:hover { background: #f4f5f7; }

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
}

.data-row {
  border-bottom: 1px solid #f4f5f7;
  transition: background 0.12s;
}
.data-row:last-child { border-bottom: none; }
.data-row:hover { background: #f8f9fc; }

td {
  padding: 13px 16px;
  color: #3d4a5c;
  vertical-align: middle;
}

.td-name {
  font-weight: 500;
  color: #0f2744;
}
.td-date {
  color: #8a94a6;
  white-space: nowrap;
}
.td-fee {
  white-space: nowrap;
  color: #3d4a5c;
}
.td-action {
  width: 140px;
  text-align: right;
}

.action-btn {
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 500;
  border: none;
  border-radius: 6px;
  padding: 5px 12px;
  cursor: pointer;
  transition: background 0.15s;
  margin-left: 6px;
}
.edit-btn {
  color: #4a9eff;
  background: rgba(74,158,255,0.08);
}
.edit-btn:hover { background: rgba(74,158,255,0.15); }
.delete-btn {
  color: #ef4444;
  background: rgba(239,68,68,0.08);
}
.delete-btn:hover { background: rgba(239,68,68,0.15); }

.empty-cell {
  text-align: center;
  padding: 48px 16px;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.empty-state p {
  font-size: 14px;
  color: #9aa3b0;
  margin: 0;
}

/* ── Modal ────────────────────────────────────────────────── */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 39, 68, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeInOverlay 0.2s ease;
}
@keyframes fadeInOverlay {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.modal-card {
  background: white;
  border-radius: 14px;
  width: 90%;
  max-width: 460px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25);
  animation: slideUp 0.25s ease;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #eaecf0;
}
.modal-header h2 {
  font-size: 17px;
  font-weight: 600;
  color: #0f2744;
  margin: 0;
}
.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #9aa3b0;
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.15s;
}
.modal-close:hover { color: #3d4a5c; }

.modal-body {
  padding: 24px;
}

.form-error {
  background: #fdf2f2;
  border: 1px solid #f5c6c6;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: #c0392b;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 16px;
}
.form-group:last-child { margin-bottom: 0; }

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #3d4a5c;
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #eaecf0;
  border-radius: 9px;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  color: #0f2744;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.form-group input::placeholder { color: #bbc4d0; }
.form-group input:focus {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.1);
}

.delete-warning {
  font-size: 14px;
  color: #3d4a5c;
  line-height: 1.6;
  margin: 0;
}
.delete-warning strong {
  color: #0f2744;
  font-weight: 600;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid #eaecf0;
}

.btn-secondary, .btn-primary, .btn-danger {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  padding: 9px 18px;
  cursor: pointer;
  transition: all 0.15s;
  border: none;
}

.btn-secondary {
  background: white;
  border: 1px solid #eaecf0;
  color: #3d4a5c;
}
.btn-secondary:hover:not(:disabled) { background: #f4f5f7; }

.btn-primary {
  background: #4a9eff;
  color: white;
}
.btn-primary:hover:not(:disabled) { background: #3a8eef; }

.btn-danger {
  background: #ef4444;
  color: white;
}
.btn-danger:hover:not(:disabled) { background: #dc2626; }

.btn-secondary:disabled, .btn-primary:disabled, .btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
