<template>
  <main id="main-content" class="min-h-screen bg-surface">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 py-10">

      <!-- Header -->
      <div class="flex items-center justify-between gap-4 mb-8 flex-wrap">
        <div>
          <h1 class="text-2xl font-bold text-ink mb-1">Medical History</h1>
          <p class="text-muted">Your consultation timeline and prescriptions from completed visits.</p>
        </div>
        <div class="text-xs text-muted">
          <span class="inline-flex items-center px-2 py-1 rounded-md bg-surface border border-border">
            Synced from your account
          </span>
        </div>
      </div>

      <div v-if="isLoading" class="bg-card border border-border rounded-2xl p-8 text-center">
        <p class="text-sm text-muted">Loading your medical history…</p>
      </div>

      <div v-else-if="loadError" class="bg-card border border-danger/30 rounded-2xl p-6">
        <p class="text-sm text-danger font-semibold mb-1">Couldn’t load medical history</p>
        <p class="text-xs text-muted mb-4">{{ loadError }}</p>
        <button
          class="inline-flex items-center gap-2 px-4 py-2 rounded-lg border border-border text-sm font-semibold text-ink hover:text-primary hover:border-primary transition-colors"
          @click="fetchMedicalHistory"
        >
          <RotateCw class="w-4 h-4" aria-hidden="true" />
          Retry
        </button>
      </div>

      <div v-else-if="showEmptyState" class="bg-card border border-border rounded-2xl">
        <div class="flex flex-col items-center justify-center py-16 px-6 text-center">
          <div class="w-16 h-16 rounded-2xl bg-surface border border-border flex items-center justify-center mb-4" aria-hidden="true">
            <FileText class="w-8 h-8 text-muted" aria-hidden="true" />
          </div>
          <h3 class="font-semibold text-ink mb-1">You don’t have a medical history yet</h3>
          <p class="text-sm text-muted max-w-md">Try booking an appointment to start building your medical history and view prescriptions here later.</p>
          <button
            class="mt-6 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-white text-sm font-semibold hover:bg-action transition-colors"
            @click="goToFindDoctor"
          >
            Book an appointment
          </button>
        </div>
      </div>

      <!-- Records list -->
      <div v-else class="space-y-3">
        <MedicalHistoryRecordCard
          v-for="record in visibleItems"
          :key="record.id"
          :title="record.title"
          :subtitle="record.subtitle"
          :description="record.description"
          :meta-line="record.metaLine"
          :status-label="record.status"
          :prescription-id="record.prescriptionId"
          :medications="record.medications"
          :symptoms="record.symptoms"
          :follow-up-line="record.followUpLine"
          :action-label="record.actionLabel"
          :action-aria-label="record.actionAriaLabel"
          :action-loading="qrLoadingByPrescriptionId[record.prescriptionId] === true"
          action-loading-label="Fetching QR…"
          @action="handleCardAction(record)"
        />
      </div>

    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { FileText, RotateCw } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import medicalHistoryService from '@/services/medicalHistoryService'
import MedicalHistoryRecordCard from '@/components/shared/MedicalHistoryRecordCard.vue'

const isLoading = ref(false)
const loadError = ref('')
const showEmptyState = ref(false)
const records = ref([])
const currentPatientId = ref('')

const authStore = useAuth()
const notificationStore = useNotificationStore()
const router = useRouter()
const qrLoadingByPrescriptionId = reactive({})

function formatDate(value) {
  if (!value) return 'Unknown date'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function toUiRecord(record, index) {
  const doctorName = record?.doctorName || 'Doctor'
  const issuedDate = formatDate(record?.issuedAt || record?.recordedAt)
  const diagnosis = record?.diagnosis || 'Consultation note'
  const prescriptionId = record?.prescriptionId || ''
  const medications = Array.isArray(record?.medications)
    ? record.medications
        .map((item) => item?.name || item?.medication || item?.drugName || item?.title || '')
        .filter(Boolean)
    : []
  const symptoms = Array.isArray(record?.symptoms)
    ? record.symptoms.map((item) => String(item).trim()).filter(Boolean)
    : []

  return {
    id: `${prescriptionId}-${index}`,
    prescriptionId,
    title: diagnosis,
    subtitle: `${doctorName} · ${issuedDate}`,
    description: record?.notes || '',
    status: 'Completed',
    metaLine: [
      record?.consultationType ? `Consultation: ${record.consultationType}` : '',
      record?.doctorSpecialization ? `Specialization: ${record.doctorSpecialization}` : '',
    ].filter(Boolean).join(' · '),
    medications,
    symptoms,
    followUpLine: record?.followUpDate ? `Follow-up: ${formatDate(record.followUpDate)}` : '',
    actionLabel: 'Get QR',
    actionAriaLabel: `Get QR code for prescription ${prescriptionId}`,
  }
}

const visitItems = computed(() => records.value.map(toUiRecord))
const visibleItems = computed(() => visitItems.value)

async function fetchMedicalHistory() {
  isLoading.value = true
  loadError.value = ''
  showEmptyState.value = false

  try {
    const token = authStore.token
    if (!token) {
      throw new Error('You must be logged in to view medical history')
    }

    const history = await medicalHistoryService.getMyMedicalHistory({ token })
    currentPatientId.value = history?.patientId || ''
    records.value = Array.isArray(history?.records) ? history.records : []
    showEmptyState.value = records.value.length === 0
  } catch (error) {
    if (error?.status === 404) {
      records.value = []
      showEmptyState.value = true
      return
    }

    const message = error?.message || 'Failed to load medical history'
    loadError.value = message
    notificationStore.push(message, 'error')
  } finally {
    isLoading.value = false
  }
}

async function requestQrSignedUrl(prescriptionId) {
  const token = authStore.token
  const authId = authStore.user?.authId || ''
  const role = authStore.user?.role || ''

  if (!token) {
    notificationStore.push('You must be logged in to fetch QR links', 'warning')
    return
  }

  if (!authId || !role) {
    notificationStore.push('Missing auth context. Please login again.', 'warning')
    return
  }

  qrLoadingByPrescriptionId[prescriptionId] = true
  try {
    const { signedUrl } = await medicalHistoryService.getPrescriptionQrSignedUrl({
      token,
      prescriptionId,
      authId,
      role,
      patientId: currentPatientId.value || undefined,
    })

    window.open(signedUrl, '_blank', 'noopener,noreferrer')
  } catch (error) {
    notificationStore.push(error?.message || 'Could not fetch QR signed URL', 'error')
  } finally {
    qrLoadingByPrescriptionId[prescriptionId] = false
  }
}

function handleCardAction(record) {
  if (!record?.prescriptionId) return
  requestQrSignedUrl(record.prescriptionId)
}

function goToFindDoctor() {
  router.push({ name: 'find-doctor' })
}

onMounted(() => {
  fetchMedicalHistory()
})
</script>