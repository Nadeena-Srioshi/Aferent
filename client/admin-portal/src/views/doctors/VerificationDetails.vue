<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex justify-between items-center">
      <div class="flex items-center space-x-4">
        <button @click="goBack" class="icon-btn">
          <ArrowLeft :size="20" />
        </button>
        <div>
          <h1 class="text-2xl font-semibold" style="color: var(--text-primary)">Verification Details</h1>
          <p style="color: var(--text-secondary)">Review doctor verification application</p>
        </div>
      </div>
      <span class="badge badge-large" :class="`badge-${verification.status}`">
        {{ formatStatus(verification.status) }}
      </span>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Main Content -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Doctor Information -->
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Doctor Information</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Full Name</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.doctorName }}</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Email</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.email }}</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Phone</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.phone }}</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Date of Birth</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ formatDate(verification.dob) }}</p>
            </div>
          </div>
        </div>

        <!-- Professional Information -->
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Professional Information</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Specialization</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.specialization }}</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">License Number</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.licenseNumber }}</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Years of Experience</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.experience }} years</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Medical School</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.medicalSchool }}</p>
            </div>
            <div class="md:col-span-2">
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Hospital/Clinic</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ verification.hospital }}</p>
            </div>
          </div>
        </div>

        <!-- Documents -->
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Submitted Documents</h3>
          <div class="space-y-3">
            <div v-for="doc in verification.documents" :key="doc.id" class="document-item">
              <div class="flex items-center justify-between">
                <div class="flex items-center space-x-3">
                  <FileText :size="20" style="color: var(--text-secondary)" />
                  <div>
                    <p class="font-medium" style="color: var(--text-primary)">{{ doc.name }}</p>
                    <p class="text-sm" style="color: var(--text-secondary)">{{ doc.size }}</p>
                  </div>
                </div>
                <div class="flex items-center space-x-2">
                  <button @click="viewDocument(doc)" class="btn-secondary-sm">
                    <Eye :size="16" class="mr-1" />
                    View
                  </button>
                  <button @click="downloadDocument(doc)" class="btn-secondary-sm">
                    <Download :size="16" class="mr-1" />
                    Download
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Review Notes -->
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Review Notes</h3>
          <textarea
            v-model="reviewNotes"
            rows="4"
            placeholder="Add your review notes here..."
            class="input-field"
          ></textarea>
        </div>
      </div>

      <!-- Sidebar -->
      <div class="space-y-6">
        <!-- Timeline -->
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Timeline</h3>
          <div class="timeline">
            <div v-for="event in timeline" :key="event.id" class="timeline-item">
              <div class="timeline-marker" :class="event.type"></div>
              <div class="timeline-content">
                <p class="font-medium text-sm" style="color: var(--text-primary)">{{ event.title }}</p>
                <p class="text-xs mt-1" style="color: var(--text-secondary)">{{ event.time }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Actions</h3>
          <div class="space-y-3">
            <button 
              v-if="verification.status === 'pending' || verification.status === 'under_review'"
              @click="markUnderReview" 
              class="btn-secondary w-full"
            >
              <Clock :size="16" class="mr-2" />
              Mark Under Review
            </button>
            <button 
              v-if="verification.status === 'pending' || verification.status === 'under_review'"
              @click="approveVerification" 
              class="btn-success w-full"
            >
              <CheckCircle :size="16" class="mr-2" />
              Approve Doctor
            </button>
            <button 
              v-if="verification.status === 'pending' || verification.status === 'under_review'"
              @click="rejectVerification" 
              class="btn-danger w-full"
            >
              <XCircle :size="16" class="mr-2" />
              Reject Application
            </button>
            <button @click="requestMoreInfo" class="btn-secondary w-full">
              <Mail :size="16" class="mr-2" />
              Request More Info
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  ArrowLeft, FileText, Eye, Download, CheckCircle, XCircle, 
  Clock, Mail 
} from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const { showToast } = useToast()

const reviewNotes = ref('')

const verification = ref({
  id: route.params.id,
  doctorName: 'Dr. Sarah Johnson',
  email: 'sarah.johnson@example.com',
  phone: '+1 (555) 123-4567',
  dob: '1985-05-15',
  specialization: 'Cardiology',
  licenseNumber: 'MD-123456',
  experience: 12,
  medicalSchool: 'Harvard Medical School',
  hospital: 'Massachusetts General Hospital',
  status: 'pending',
  submittedDate: '2024-04-01',
  documents: [
    { id: 1, name: 'Medical License', size: '2.5 MB', url: '/docs/license.pdf' },
    { id: 2, name: 'Medical Degree', size: '3.2 MB', url: '/docs/degree.pdf' },
    { id: 3, name: 'Board Certification', size: '1.8 MB', url: '/docs/board.pdf' },
    { id: 4, name: 'ID Proof', size: '1.2 MB', url: '/docs/id.pdf' }
  ]
})

const timeline = ref([
  { id: 1, title: 'Application Submitted', time: '2 days ago', type: 'success' },
  { id: 2, title: 'Documents Uploaded', time: '2 days ago', type: 'success' },
  { id: 3, title: 'Pending Review', time: '2 days ago', type: 'warning' }
])

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const formatStatus = (status: string) => {
  return status.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase())
}

const goBack = () => {
  router.push('/doctors/verification')
}

const viewDocument = (doc: any) => {
  showToast(`Viewing ${doc.name}`, 'info')
}

const downloadDocument = (doc: any) => {
  showToast(`Downloading ${doc.name}`, 'info')
}

const markUnderReview = () => {
  verification.value.status = 'under_review'
  timeline.value.push({ 
    id: timeline.value.length + 1, 
    title: 'Under Review', 
    time: 'Just now', 
    type: 'info' 
  })
  showToast('Status updated to Under Review', 'info')
}

const approveVerification = () => {
  if (confirm('Are you sure you want to approve this doctor?')) {
    verification.value.status = 'approved'
    timeline.value.push({ 
      id: timeline.value.length + 1, 
      title: 'Application Approved', 
      time: 'Just now', 
      type: 'success' 
    })
    showToast('Doctor approved successfully!', 'success')
    setTimeout(() => router.push('/doctors/verification'), 2000)
  }
}

const rejectVerification = () => {
  if (confirm('Are you sure you want to reject this application?')) {
    if (!reviewNotes.value) {
      showToast('Please add review notes before rejecting', 'error')
      return
    }
    verification.value.status = 'rejected'
    timeline.value.push({ 
      id: timeline.value.length + 1, 
      title: 'Application Rejected', 
      time: 'Just now', 
      type: 'danger' 
    })
    showToast('Application rejected', 'error')
    setTimeout(() => router.push('/doctors/verification'), 2000)
  }
}

const requestMoreInfo = () => {
  showToast('Request sent to doctor', 'info')
}
</script>

<style scoped>
.card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.5rem;
}

.icon-btn {
  padding: 0.5rem;
  border-radius: 8px;
  transition: background 0.2s;
  color: var(--text-secondary);
}

.icon-btn:hover {
  background: var(--bg-hover);
}

.badge-large {
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 600;
}

.badge-pending { background: #fef3c7; color: #92400e; }
.badge-under_review { background: #dbeafe; color: #1e40af; }
.badge-approved { background: #dcfce7; color: #166534; }
.badge-rejected { background: #fee2e2; color: #991b1b; }

.input-field {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  resize: vertical;
}

.document-item {
  padding: 1rem;
  background: var(--bg-secondary);
  border-radius: 8px;
  transition: background 0.2s;
}

.document-item:hover {
  background: var(--bg-hover);
}

.btn-secondary-sm {
  display: flex;
  align-items: center;
  padding: 0.375rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 0.875rem;
  transition: background 0.2s;
}

.btn-secondary-sm:hover {
  background: var(--bg-hover);
}

.btn-secondary, .btn-success, .btn-danger {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-weight: 500;
  transition: opacity 0.2s;
}

.btn-secondary {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn-success {
  background: #10b981;
  color: white;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-secondary:hover, .btn-success:hover, .btn-danger:hover {
  opacity: 0.9;
}

.timeline {
  position: relative;
}

.timeline-item {
  position: relative;
  padding-left: 2rem;
  padding-bottom: 1.5rem;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: 0.5rem;
  top: 1.5rem;
  bottom: -0.5rem;
  width: 2px;
  background: var(--border-color);
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-marker {
  position: absolute;
  left: 0;
  top: 0.25rem;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  background: var(--card-bg);
}

.timeline-marker.success {
  background: #10b981;
  border-color: #10b981;
}

.timeline-marker.warning {
  background: #f59e0b;
  border-color: #f59e0b;
}

.timeline-marker.info {
  background: #3b82f6;
  border-color: #3b82f6;
}

.timeline-marker.danger {
  background: #ef4444;
  border-color: #ef4444;
}
</style>
