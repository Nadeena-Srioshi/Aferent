<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Refund Management</h1>
      <p style="color: var(--text-secondary)">Handle refund requests and processing</p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Pending Requests</p>
        <p class="text-2xl font-bold mt-1" style="color: var(--text-primary)">12</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Approved</p>
        <p class="text-2xl font-bold mt-1 text-green-600">89</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Rejected</p>
        <p class="text-2xl font-bold mt-1 text-red-600">15</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Total Refunded</p>
        <p class="text-2xl font-bold mt-1" style="color: var(--text-primary)">$12,450</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <input v-model="filters.search" type="text" placeholder="Search..." class="input-field" />
        </div>
        <div>
          <select v-model="filters.status" class="input-field">
            <option value="">All Status</option>
            <option value="pending">Pending</option>
            <option value="approved">Approved</option>
            <option value="rejected">Rejected</option>
            <option value="completed">Completed</option>
          </select>
        </div>
        <div>
          <button @click="resetFilters" class="btn-secondary w-full">Reset Filters</button>
        </div>
      </div>
    </div>

    <!-- Refunds Table -->
    <div class="card">
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>Request ID</th>
              <th>Transaction ID</th>
              <th>Patient</th>
              <th>Amount</th>
              <th>Reason</th>
              <th>Status</th>
              <th>Requested Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="refund in filteredRefunds" :key="refund.id">
              <td class="font-mono text-sm" style="color: var(--text-secondary)">{{ refund.requestId }}</td>
              <td class="font-mono text-sm" style="color: var(--text-secondary)">{{ refund.transactionId }}</td>
              <td style="color: var(--text-primary)">{{ refund.patient }}</td>
              <td class="font-semibold" style="color: var(--text-primary)">${{ refund.amount }}</td>
              <td style="color: var(--text-secondary)">{{ refund.reason }}</td>
              <td>
                <span class="badge" :class="`badge-${refund.status}`">{{ refund.status }}</span>
              </td>
              <td style="color: var(--text-secondary)">{{ formatDate(refund.requestedDate) }}</td>
              <td>
                <div class="flex items-center space-x-2">
                  <button v-if="refund.status === 'pending'" @click="approveRefund(refund)" class="btn-success-sm">
                    <CheckCircle :size="16" class="mr-1" />
                    Approve
                  </button>
                  <button v-if="refund.status === 'pending'" @click="rejectRefund(refund)" class="btn-danger-sm">
                    <XCircle :size="16" class="mr-1" />
                    Reject
                  </button>
                  <button @click="viewDetails(refund)" class="icon-btn" title="View">
                    <Eye :size="18" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { CheckCircle, XCircle, Eye } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

const filters = ref({
  search: '',
  status: ''
})

const refunds = ref([
  {
    id: 1,
    requestId: 'REF-2024-001',
    transactionId: 'TXN-2024-001234',
    patient: 'John Doe',
    amount: 150,
    reason: 'Appointment cancelled by doctor',
    status: 'pending',
    requestedDate: '2024-04-08'
  },
  {
    id: 2,
    requestId: 'REF-2024-002',
    transactionId: 'TXN-2024-001235',
    patient: 'Jane Smith',
    amount: 200,
    reason: 'Service not rendered',
    status: 'approved',
    requestedDate: '2024-04-07'
  },
  {
    id: 3,
    requestId: 'REF-2024-003',
    transactionId: 'TXN-2024-001236',
    patient: 'Bob Wilson',
    amount: 175,
    reason: 'Duplicate payment',
    status: 'completed',
    requestedDate: '2024-04-06'
  },
  {
    id: 4,
    requestId: 'REF-2024-004',
    transactionId: 'TXN-2024-001237',
    patient: 'Alice Brown',
    amount: 150,
    reason: 'Patient request',
    status: 'rejected',
    requestedDate: '2024-04-05'
  }
])

const filteredRefunds = computed(() => {
  return refunds.value.filter(refund => {
    const matchesSearch = !filters.value.search || 
      refund.requestId.toLowerCase().includes(filters.value.search.toLowerCase()) ||
      refund.patient.toLowerCase().includes(filters.value.search.toLowerCase())
    
    const matchesStatus = !filters.value.status || refund.status === filters.value.status
    
    return matchesSearch && matchesStatus
  })
})

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const resetFilters = () => {
  filters.value = { search: '', status: '' }
}

const approveRefund = (refund: any) => {
  if (confirm(`Approve refund ${refund.requestId}?`)) {
    refund.status = 'approved'
    showToast('Refund approved', 'success')
  }
}

const rejectRefund = (refund: any) => {
  if (confirm(`Reject refund ${refund.requestId}?`)) {
    refund.status = 'rejected'
    showToast('Refund rejected', 'error')
  }
}

const viewDetails = (refund: any) => {
  showToast(`Viewing ${refund.requestId}`, 'info')
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

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: capitalize;
}

.badge-pending { background: #fef3c7; color: #92400e; }
.badge-approved { background: #dcfce7; color: #166534; }
.badge-rejected { background: #fee2e2; color: #991b1b; }
.badge-completed { background: #dbeafe; color: #1e40af; }

.btn-success-sm, .btn-danger-sm {
  display: flex;
  align-items: center;
  padding: 0.375rem 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  transition: opacity 0.2s;
}

.btn-success-sm {
  background: #10b981;
  color: white;
}

.btn-danger-sm {
  background: #ef4444;
  color: white;
}

.btn-success-sm:hover, .btn-danger-sm:hover {
  opacity: 0.9;
}

.icon-btn {
  padding: 0.5rem;
  border-radius: 6px;
  transition: background 0.2s;
  color: var(--text-secondary);
}

.icon-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.btn-secondary {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
}
</style>
