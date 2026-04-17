<template>
  <main id="main-content" class="min-h-screen bg-surface">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 py-10">
      <!-- Header -->
      <div class="flex items-start justify-between gap-4 mb-8 flex-wrap">
        <div>
          <h1 class="text-2xl font-bold text-ink mb-1">Medical Records</h1>
          <p class="text-muted max-w-2xl">
            Upload medical reports only. Classify each report by subcategory and manage who can access it.
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-border bg-card text-xs font-semibold text-ink">
            <ShieldCheck class="w-3.5 h-3.5 text-primary" />
            Medical reports only
          </span>
          <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-border bg-card text-xs font-semibold text-ink">
            <Users class="w-3.5 h-3.5 text-success" />
            Access controlled
          </span>
        </div>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div class="bg-card border border-border rounded-2xl p-5">
          <p class="text-xs uppercase tracking-wide text-muted mb-1">Total reports</p>
          <p class="text-2xl font-bold text-ink">{{ records.length }}</p>
        </div>
        <div class="bg-card border border-border rounded-2xl p-5">
          <p class="text-xs uppercase tracking-wide text-muted mb-1">Selected access grants</p>
          <p class="text-2xl font-bold text-ink">{{ selectedRecordAccess.length }}</p>
        </div>
        <div class="bg-card border border-border rounded-2xl p-5">
          <p class="text-xs uppercase tracking-wide text-muted mb-1">Active shared reports</p>
          <p class="text-2xl font-bold text-ink">{{ activeSharedRecords }}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <!-- Left: upload + list -->
        <section class="lg:col-span-8 space-y-6">
          <!-- Upload panel -->
          <section class="bg-card border border-border rounded-2xl p-5">
            <div class="flex items-start justify-between gap-3 mb-4 flex-wrap">
              <div>
                <h2 class="text-lg font-bold text-ink">Upload a medical report</h2>
                <p class="text-sm text-muted">Choose a subcategory so the report is easy to find later.</p>
              </div>
              <span class="inline-flex items-center px-2.5 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold">
                Medical documents
              </span>
            </div>

            <form class="grid grid-cols-1 md:grid-cols-2 gap-4" @submit.prevent="handleUpload">
              <label class="block">
                <span class="block text-sm font-semibold text-ink mb-2">File</span>
                <input
                  type="file"
                  class="block w-full text-sm text-muted border border-border rounded-xl bg-surface px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-primary"
                  @change="handleFileChange"
                />
              </label>

              <label class="block">
                <span class="block text-sm font-semibold text-ink mb-2">Record title</span>
                <input
                  v-model="uploadForm.displayName"
                  type="text"
                  placeholder="e.g. Blood test - April 2026"
                  class="w-full rounded-xl border border-border bg-surface px-3 py-2.5 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </label>

              <label class="block md:col-span-2">
                <span class="block text-sm font-semibold text-ink mb-2">Report subcategory</span>
                <select
                  v-model="uploadForm.documentSubType"
                  class="w-full rounded-xl border border-border bg-surface px-3 py-2.5 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary"
                >
                  <option v-for="option in documentSubTypeOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <div class="flex items-end gap-3">
                <button
                  class="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold hover:opacity-95 disabled:opacity-60 transition-colors"
                  type="submit"
                  :disabled="uploading || !uploadForm.file"
                >
                  <Upload class="w-4 h-4" aria-hidden="true" />
                  {{ uploading ? 'Uploading…' : 'Upload record' }}
                </button>
                <p class="text-xs text-muted leading-snug">
                  The file uploads directly to storage, then appears in your records list.
                </p>
              </div>
            </form>
          </section>

          <!-- Filters -->
          <div class="flex gap-1 border-b border-border overflow-x-auto" role="tablist" aria-label="Record filters">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              role="tab"
              :aria-selected="activeTab === tab.key"
              :class="[
                'px-4 py-3 text-sm font-semibold whitespace-nowrap border-b-2 -mb-px transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-t-lg',
                activeTab === tab.key ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-ink'
              ]"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <div v-if="isLoading" class="bg-card border border-border rounded-2xl p-8 text-center">
            <p class="text-sm text-muted">Loading your records…</p>
          </div>

          <div v-else-if="loadError" class="bg-card border border-danger/30 rounded-2xl p-6">
            <p class="text-sm text-danger font-semibold mb-1">Couldn’t load records</p>
            <p class="text-xs text-muted mb-4">{{ loadError }}</p>
            <button
              class="inline-flex items-center gap-2 px-4 py-2 rounded-lg border border-border text-sm font-semibold text-ink hover:text-primary hover:border-primary transition-colors"
              @click="fetchRecords"
            >
              <RefreshCw class="w-4 h-4" aria-hidden="true" />
              Retry
            </button>
          </div>

          <div v-else-if="filteredRecords.length" class="grid grid-cols-1 gap-3">
            <MedicalRecordCard
              v-for="record in filteredRecords"
              :key="record.id"
              :title="record.title"
              :subtitle="record.subtitle"
              :description="record.description"
              :status-label="record.statusLabel"
              :meta-line="record.metaLine"
              :uploaded-at-label="record.uploadedAtLabel"
              :content-type="record.contentType"
              :access-count="record.accessCount"
              :selected="selectedRecord?.id === record.id"
              :kind="record.kind"
              @select="selectRecord(record.id)"
            />
          </div>

          <div v-else class="bg-card border border-border rounded-2xl">
            <div class="flex flex-col items-center justify-center py-16 px-6 text-center">
              <div class="w-16 h-16 rounded-2xl bg-surface border border-border flex items-center justify-center mb-4" aria-hidden="true">
                <FileUp class="w-8 h-8 text-muted" aria-hidden="true" />
              </div>
              <h3 class="font-semibold text-ink mb-1">
                {{ records.length ? `No ${activeTabLabel.toLowerCase()} match this filter` : `No ${activeTabLabel.toLowerCase()} yet` }}
              </h3>
              <p class="text-sm text-muted max-w-md">
                {{ records.length
                  ? 'Try a different subcategory tab, or upload a report using a matching subcategory.'
                  : 'Upload your first medical report to keep everything organized in one place.' }}
              </p>
            </div>
          </div>
        </section>

        <!-- Right: detail + access -->
        <aside class="lg:col-span-4 space-y-6">
          <section class="bg-card border border-border rounded-2xl p-5 sticky top-6">
            <div class="flex items-start justify-between gap-3 mb-4">
              <div>
                <h2 class="text-lg font-bold text-ink">Record details</h2>
                <p class="text-xs text-muted">Quick actions for the selected medical report</p>
              </div>
              <span class="text-xs font-semibold px-2.5 py-1 rounded-full bg-surface border border-border">
                {{ selectedRecord ? selectedRecord.kind : 'None selected' }}
              </span>
            </div>

            <div v-if="!selectedRecord" class="rounded-xl border border-dashed border-border bg-surface p-5 text-sm text-muted">
              Select a record from the list to see details, access, and actions.
            </div>

            <div v-else class="space-y-4">
              <div class="rounded-xl border border-border bg-surface p-4">
                <p class="font-semibold text-ink">{{ selectedRecord.title }}</p>
                <p class="text-xs text-muted mt-1">{{ selectedRecord.subtitle }}</p>
                <p v-if="selectedRecord.description" class="text-sm text-muted mt-3 leading-relaxed">
                  {{ selectedRecord.description }}
                </p>
              </div>

              <div class="grid grid-cols-2 gap-3 text-xs">
                <div class="rounded-xl border border-border bg-white/50 p-3">
                  <p class="text-muted mb-1">Uploaded</p>
                  <p class="font-semibold text-ink">{{ selectedRecord.uploadedAtLabel }}</p>
                </div>
                <div class="rounded-xl border border-border bg-white/50 p-3">
                  <p class="text-muted mb-1">Subcategory</p>
                  <p class="font-semibold text-ink">{{ selectedRecord.kindLabel }}</p>
                </div>
              </div>

              <div class="flex flex-wrap gap-2">
                <button
                  class="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-primary text-white text-sm font-semibold hover:opacity-95 transition-colors"
                  @click="downloadSelectedRecord"
                >
                  <Download class="w-4 h-4" aria-hidden="true" />
                  Download
                </button>
                <button
                  class="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-border text-ink text-sm font-semibold hover:border-primary hover:text-primary transition-colors"
                  @click="refreshAll"
                >
                  <RotateCw class="w-4 h-4" aria-hidden="true" />
                  Refresh
                </button>
                <button
                  class="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-danger/30 text-danger text-sm font-semibold hover:bg-danger/5 transition-colors"
                  @click="deleteSelectedRecord"
                >
                  <Trash2 class="w-4 h-4" aria-hidden="true" />
                  Delete
                </button>
              </div>
            </div>
          </section>

          <RecordAccessPanel
            :selected-document="selectedRecordPayload"
            :grants="selectedRecordAccessEnriched"
          />
        </aside>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Download, FileUp, RefreshCw, ShieldCheck, Trash2, Upload, Users } from 'lucide-vue-next'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import recordsService from '@/services/recordsService'
import MedicalRecordCard from '@/components/shared/MedicalRecordCard.vue'
import RecordAccessPanel from '@/components/shared/RecordAccessPanel.vue'

const authStore = useAuth()
const notificationStore = useNotificationStore()

const isLoading = ref(false)
const loadError = ref('')
const uploading = ref(false)
const activeTab = ref('all')
const records = ref([])
const accessGrants = ref([])
const doctorProfilesById = reactive({})
const selectedRecordId = ref('')

const uploadForm = reactive({
  file: null,
  displayName: '',
  documentSubType: 'LAB_REPORT',
})

const tabs = [
  { key: 'all', label: 'All records' },
  { key: 'LAB_REPORT', label: 'Lab reports' },
  { key: 'IMAGING', label: 'Imaging' },
  { key: 'REFERRAL', label: 'Referrals' },
  { key: 'DISCHARGE_SUMMARY', label: 'Discharge summaries' },
  { key: 'OTHER', label: 'Other' },
]

const documentSubTypeOptions = [
  { value: 'LAB_REPORT', label: 'Lab report' },
  { value: 'IMAGING', label: 'Imaging / scan' },
  { value: 'REFERRAL', label: 'Referral' },
  { value: 'DISCHARGE_SUMMARY', label: 'Discharge summary' },
  { value: 'OTHER', label: 'Other' },
]

const activeTabLabel = computed(() => tabs.find((tab) => tab.key === activeTab.value)?.label || 'Records')

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

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

function formatBytes(bytes) {
  if (!bytes && bytes !== 0) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function kindLabel(kind) {
  return {
    MEDICAL_REPORT: 'Medical report',
    LAB_REPORT: 'Lab report',
    IMAGING: 'Imaging',
    REFERRAL: 'Referral',
    DISCHARGE_SUMMARY: 'Discharge summary',
    OTHER: 'Other',
  }[kind] || kind || 'Record'
}

function enrichDoctor(grant) {
  const profile = doctorProfilesById[grant.doctorId]
  const fullName = profile
    ? [profile.firstName, profile.lastName].filter(Boolean).join(' ').trim()
    : ''

  return {
    ...grant,
    doctorName: fullName || profile?.doctorId || grant.doctorId,
    doctorSpecialization: profile?.specialization || grant.doctorAuthId,
  }
}

function buildRecordView(doc) {
  const normalized = normalizeDocument(doc)
  const sharedCount = grantsForDocument(normalized.id).length
  const title = normalized.displayName || normalized.originalFileName || 'Untitled record'
  const subtitle = `${kindLabel(normalized.documentSubType || normalized.documentType)} · ${formatDate(normalized.uploadedAt || normalized.requestedAt)}`
  const description = normalized.fileName && normalized.fileName !== title ? normalized.fileName : ''

  return {
    id: normalized.id,
    kind: normalized.documentSubType || normalized.documentType || 'OTHER',
    kindLabel: kindLabel(normalized.documentSubType || normalized.documentType),
    title,
    subtitle,
    description,
    statusLabel: normalized.uploadStatus || 'UPLOADED',
    metaLine: `${formatBytes(normalized.fileSize)} · ${normalized.visibility || 'private'}`,
    uploadedAtLabel: formatDate(normalized.uploadedAt || normalized.requestedAt),
    contentType: normalized.contentType || '—',
    accessCount: sharedCount,
    original: normalized,
  }
}

function normalizeDocument(doc) {
  if (!doc || typeof doc !== 'object') {
    return { id: '', documentType: 'OTHER' }
  }

  return {
    ...doc,
    id: doc.id || doc.documentId || doc._id || '',
    documentType: doc.documentType || 'OTHER',
    documentSubType: doc.documentSubType || '',
    displayName: doc.displayName || '',
    originalFileName: doc.originalFileName || doc.fileName || '',
    fileName: doc.fileName || doc.originalFileName || '',
  }
}

function grantsForDocument(documentId) {
  return accessGrants.value
    .filter((grant) => Array.isArray(grant.allowedDocumentIds) && grant.allowedDocumentIds.includes(documentId))
    .map(enrichDoctor)
}

const filteredRecords = computed(() => {
  const docs = records.value.map(buildRecordView)
  if (activeTab.value === 'all') return docs
  return docs.filter((record) => record.kind === activeTab.value)
})

const selectedRecord = computed(() => {
  if (!filteredRecords.value.length) return null
  return filteredRecords.value.find((record) => record.id === selectedRecordId.value) || filteredRecords.value[0]
})

const selectedRecordPayload = computed(() => selectedRecord.value?.original || null)
const selectedRecordAccess = computed(() => {
  if (!selectedRecord.value) return []
  return grantsForDocument(selectedRecord.value.id)
})
const selectedRecordAccessEnriched = computed(() => selectedRecordAccess.value)
const activeSharedRecords = computed(() => {
  const uniqueIds = new Set()
  accessGrants.value.forEach((grant) => {
    grant.allowedDocumentIds?.forEach((id) => uniqueIds.add(id))
  })
  return uniqueIds.size
})

function selectRecord(id) {
  selectedRecordId.value = id
}

async function fetchRecords() {
  isLoading.value = true
  loadError.value = ''

  try {
    const token = authStore.token
    if (!token) {
      throw new Error('You must be logged in to view records')
    }

    const [documentsResult, grantsResult, currentPatientResult] = await Promise.allSettled([
      recordsService.getMyDocuments({ token }),
      recordsService.getMyDocumentAccess({ token }),
      authStore.fetchMe(),
    ])

    records.value = documentsResult.status === 'fulfilled' && Array.isArray(documentsResult.value)
      ? documentsResult.value
      : []

    if (documentsResult.status === 'rejected') {
      throw documentsResult.reason
    }

    accessGrants.value = grantsResult.status === 'fulfilled' && Array.isArray(grantsResult.value)
      ? grantsResult.value
      : []

    const uniqueDoctorIds = [...new Set(accessGrants.value.map((grant) => grant.doctorId).filter(Boolean))]
    await Promise.all(uniqueDoctorIds.map(async (doctorId) => {
      if (doctorProfilesById[doctorId]) return
      try {
        doctorProfilesById[doctorId] = await recordsService.getDoctorProfile(doctorId)
      } catch {
        doctorProfilesById[doctorId] = { doctorId }
      }
    }))

    const currentPatient = currentPatientResult.status === 'fulfilled' ? currentPatientResult.value : null
    if (currentPatient?.role !== 'PATIENT') {
      notificationStore.push('This page is designed for patient access.', 'warning')
    }

    if (grantsResult.status === 'rejected') {
      notificationStore.push('Documents loaded, but access details could not be loaded.', 'warning')
    }

    if (currentPatientResult.status === 'rejected') {
      notificationStore.push('Documents loaded, but your profile could not be refreshed.', 'warning')
    }

    if (!selectedRecordId.value && records.value.length) {
      selectedRecordId.value = records.value[0].id
    }
  } catch (error) {
    const message = error?.message || 'Failed to load records'
    loadError.value = message
    notificationStore.push(message, 'error')
  } finally {
    isLoading.value = false
  }
}

async function handleUpload() {
  const token = authStore.token
  if (!token) {
    notificationStore.push('Please log in to upload records', 'warning')
    return
  }

  if (!uploadForm.file) {
    notificationStore.push('Please choose a file first', 'warning')
    return
  }

  uploading.value = true
  try {
    const uploadInit = await recordsService.initializeDocumentUpload({
      token,
      fileName: uploadForm.file.name,
      contentType: uploadForm.file.type || 'application/octet-stream',
      documentType: 'MEDICAL_REPORT',
      documentSubType: uploadForm.documentSubType,
      displayName: uploadForm.displayName || uploadForm.file.name,
    })

    await recordsService.uploadDocumentToPresignedUrl({
      uploadUrl: uploadInit.uploadUrl,
      file: uploadForm.file,
      contentType: uploadForm.file.type,
    })

    notificationStore.push('Record uploaded successfully', 'success')

    uploadForm.file = null
    uploadForm.displayName = ''
    uploadForm.documentSubType = 'LAB_REPORT'

    await fetchRecords()
  } catch (error) {
    notificationStore.push(error?.message || 'Upload failed', 'error')
  } finally {
    uploading.value = false
  }
}

function handleFileChange(event) {
  uploadForm.file = event.target.files?.[0] || null
}

async function downloadSelectedRecord() {
  if (!selectedRecord.value) return

  try {
    const { downloadUrl } = await recordsService.getMyDocumentDownloadUrl({
      token: authStore.token,
      documentId: selectedRecord.value.id,
    })
    window.open(downloadUrl, '_blank', 'noopener,noreferrer')
  } catch (error) {
    notificationStore.push(error?.message || 'Unable to generate download URL', 'error')
  }
}

async function deleteSelectedRecord() {
  if (!selectedRecord.value) return

  try {
    await recordsService.deleteMyDocument({
      token: authStore.token,
      documentId: selectedRecord.value.id,
    })
    notificationStore.push('Record deleted', 'success')
    await fetchRecords()
  } catch (error) {
    notificationStore.push(error?.message || 'Unable to delete record', 'error')
  }
}

async function refreshAll() {
  await fetchRecords()
}

onMounted(() => {
  fetchRecords()
})
</script>
