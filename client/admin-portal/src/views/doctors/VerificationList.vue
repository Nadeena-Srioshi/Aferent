<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Doctor Verifications</h1>
        <p style="color: var(--text-secondary)">Review and verify doctor applications</p>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Pending</p>
        <p class="text-2xl font-bold mt-1" style="color: var(--text-primary)">{{ pendingCount }}</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Approved</p>
        <p class="text-2xl font-bold mt-1 text-green-600">{{ approvedCount }}</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Rejected</p>
        <p class="text-2xl font-bold mt-1 text-red-600">{{ rejectedCount }}</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Under Review</p>
        <p class="text-2xl font-bold mt-1 text-yellow-600">{{ reviewCount }}</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label class="block text-sm font-medium mb-2" style="color: var(--text-primary)">Search</label>
          <input
            v-model="filters.search"
            type="text"
            placeholder="Search by name or email..."
            class="input-field"
          />
        </div>
        <div>
          <label class="block text-sm font-medium mb-2" style="color: var(--text-primary)">Status</label>
          <select v-model="filters.status" class="input-field">
            <option value="">All Status</option>
            <option value="pending">Pending</option>
            <option value="under_review">Under Review</option>
            <option value="approved">Approved</option>
            <option value="rejected">Rejected</option>
          </select>
        </div>
        <div class="flex items-end">
          <button @click="resetFilters" class="btn-secondary w-full">Reset Filters</button>
        </div>
      </div>
    </div>

    <!-- Verifications Table -->
    <div class="card">
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>Doctor</th>
              <th>Specialization</th>
              <th>License Number</th>
              <th>Submitted Date</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="verification in filteredVerifications" :key="verification.id">
              <td>
                <div class="flex items-center space-x-3">
                  <div class="avatar">{{ verification.doctorName.charAt(0) }}</div>
                  <div>
                    <p class="font-medium" style="color: var(--text-primary)">{{ verification.doctorName }}</p>
                    <p class="text-sm" style="color: var(--text-secondary)">{{ verification.email }}</p>
                  </div>
                </div>
              </td>
              <td style="color: var(--text-secondary)">{{ verification.specialization }}</td>
              <td style="color: var(--text-secondary)">{{ verification.licenseNumber }}</td>
              <td style="color: var(--text-secondary)">{{ formatDate(verification.submittedDate) }}</td>
              <td>
                <span class="badge" :class="`badge-${verification.status}`">
                  {{ formatStatus(verification.status) }}
                </span>
              </td>
              <td>
                <div class="flex items-center space-x-2">
                  <button @click="viewDetails(verification)" class="btn-primary-sm">
                    <Eye :size="16" class="mr-1" />
                    Review
                  </button>
                  <button 
                    v-if="verification.status === 'pending' || verification.status === 'under_review'"
                    @click="quickApprove(verification)" 
                    class="icon-btn text-green-600"
                    title="Quick Approve"
                  >
                    <CheckCircle :size="18" />
                  </button>
                  <button 
                    v-if="verification.status === 'pending' || verification.status === 'under_review'"
                    @click="quickReject(verification)" 
                    class="icon-btn text-red-600"
                    title="Quick Reject"
                  >
                    <XCircle :size="18" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex justify-between items-center mt-4 pt-4 border-t" style="border-color: var(--border-color)">
        <p class="text-sm" style="color: var(--text-secondary)">
          Showing {{ filteredVerifications.length }} verifications
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Eye, CheckCircle, XCircle } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { showToast } = useToast()

const filters = ref({
  search: '',
  status: ''
})

const verifications = ref([
  {
    id: 1,
    doctorName: 'Dr. Sarah Johnson',
    email: 'sarah.johnson@example.com',
    specialization: 'Cardiology',
    licenseNumber: 'MD-123456',
    submittedDate: '2024-04-01',
    status: 'pending'
  },
  {
    id: 2,
    doctorName: 'Dr. Michael Chen',
    email: 'michael.chen@example.com',
    specialization: 'Neurology',
    licenseNumber: 'MD-234567',
    submittedDate: '2024-04-02',
    status: 'under_review'
  },
  {
    id: 3,
    doctorName: 'Dr. Emily Williams',
    email: 'emily.williams@example.com',
    specialization: 'Pediatrics',
    licenseNumber: 'MD-345678',
    submittedDate: '2024-03-28',
    status: 'approved'
  },
  {
    id: 4,
    doctorName: 'Dr. James Brown',
    email: 'james.brown@example.com',
    specialization: 'Orthopedics',
    licenseNumber: 'MD-456789',
    submittedDate: '2024-03-25',
    status: 'rejected'
  },
  {
    id: 5,
    doctorName: 'Dr. Lisa Anderson',
    email: 'lisa.anderson@example.com',
    specialization: 'Dermatology',
    licenseNumber: 'MD-567890',
    submittedDate: '2024-04-03',
    status: 'pending'
  }
])

const filteredVerifications = computed(() => {
  return verifications.value.filter(verification => {
    const matchesSearch = !filters.value.search || 
      verification.doctorName.toLowerCase().includes(filters.value.search.toLowerCase()) ||
      verification.email.toLowerCase().includes(filters.value.search.toLowerCase())
    
    const matchesStatus = !filters.value.status || verification.status === filters.value.status
    
    return matchesSearch && matchesStatus
  })
})

const pendingCount = computed(() => verifications.value.filter(v => v.status === 'pending').length)
const approvedCount = computed(() => verifications.value.filter(v => v.status === 'approved').length)
const rejectedCount = computed(() => verifications.value.filter(v => v.status === 'rejected').length)
const reviewCount = computed(() => verifications.value.filter(v => v.status === 'under_review').length)

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const formatStatus = (status: string) => {
  return status.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase())
}

const resetFilters = () => {
  filters.value = { search: '', status: '' }
}

const viewDetails = (verification: any) => {
  router.push(`/doctors/verification/${verification.id}`)
}

const quickApprove = (verification: any) => {
  if (confirm(`Are you sure you want to approve ${verification.doctorName}?`)) {
    verification.status = 'approved'
    showToast('Doctor approved successfully', 'success')
  }
}

const quickReject = (verification: any) => {
  if (confirm(`Are you sure you want to reject ${verification.doctorName}?`)) {
    verification.status = 'rejected'
    showToast('Doctor rejected', 'error')
  }
}
</script>

<style scoped>
.card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.5rem;
}

.stat-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1rem;
}

.input-field {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 0.75rem;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 2px solid var(--border-color);
}

.data-table td {
  padding: 0.75rem;
  border-bottom: 1px solid var(--border-color);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-pending { background: #fef3c7; color: #92400e; }
.badge-under_review { background: #dbeafe; color: #1e40af; }
.badge-approved { background: #dcfce7; color: #166534; }
.badge-rejected { background: #fee2e2; color: #991b1b; }

.btn-primary-sm {
  display: flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  background: var(--primary);
  color: white;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: opacity 0.2s;
}

.btn-primary-sm:hover {
  opacity: 0.9;
}

.icon-btn {
  padding: 0.5rem;
  border-radius: 6px;
  transition: background 0.2s;
}

.icon-btn:hover {
  background: var(--bg-hover);
}

.btn-secondary {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
}
</style>
