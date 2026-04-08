<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Payments</h1>
        <p style="color: var(--text-secondary)">Manage payment transactions</p>
      </div>
      <button @click="exportPayments" class="btn-primary">
        <Download :size="20" class="mr-2" />
        Export
      </button>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Total Revenue</p>
        <p class="text-2xl font-bold mt-1" style="color: var(--text-primary)">$45,678</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Completed</p>
        <p class="text-2xl font-bold mt-1 text-green-600">1,234</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Pending</p>
        <p class="text-2xl font-bold mt-1 text-yellow-600">45</p>
      </div>
      <div class="stat-card">
        <p class="text-sm" style="color: var(--text-secondary)">Refunded</p>
        <p class="text-2xl font-bold mt-1 text-red-600">23</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <input v-model="filters.search" type="text" placeholder="Search..." class="input-field" />
        </div>
        <div>
          <select v-model="filters.status" class="input-field">
            <option value="">All Status</option>
            <option value="completed">Completed</option>
            <option value="pending">Pending</option>
            <option value="failed">Failed</option>
            <option value="refunded">Refunded</option>
          </select>
        </div>
        <div>
          <input v-model="filters.dateFrom" type="date" class="input-field" />
        </div>
        <div>
          <input v-model="filters.dateTo" type="date" class="input-field" />
        </div>
      </div>
    </div>

    <!-- Payments Table -->
    <div class="card">
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>Transaction ID</th>
              <th>Patient</th>
              <th>Doctor</th>
              <th>Amount</th>
              <th>Payment Method</th>
              <th>Status</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="payment in filteredPayments" :key="payment.id">
              <td class="font-mono text-sm" style="color: var(--text-secondary)">{{ payment.transactionId }}</td>
              <td style="color: var(--text-primary)">{{ payment.patient }}</td>
              <td style="color: var(--text-primary)">{{ payment.doctor }}</td>
              <td class="font-semibold" style="color: var(--text-primary)">${{ payment.amount }}</td>
              <td style="color: var(--text-secondary)">{{ payment.method }}</td>
              <td>
                <span class="badge" :class="`badge-${payment.status}`">{{ payment.status }}</span>
              </td>
              <td style="color: var(--text-secondary)">{{ formatDate(payment.date) }}</td>
              <td>
                <div class="flex items-center space-x-2">
                  <button @click="viewPayment(payment)" class="icon-btn" title="View">
                    <Eye :size="18" />
                  </button>
                  <button v-if="payment.status === 'completed'" @click="initiateRefund(payment)" class="icon-btn text-red-600" title="Refund">
                    <RefreshCcw :size="18" />
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
import { Download, Eye, RefreshCcw } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

const filters = ref({
  search: '',
  status: '',
  dateFrom: '',
  dateTo: ''
})

const payments = ref([
  {
    id: 1,
    transactionId: 'TXN-2024-001234',
    patient: 'John Doe',
    doctor: 'Dr. Sarah Johnson',
    amount: 150,
    method: 'Credit Card',
    status: 'completed',
    date: '2024-04-08'
  },
  {
    id: 2,
    transactionId: 'TXN-2024-001235',
    patient: 'Jane Smith',
    doctor: 'Dr. Michael Chen',
    amount: 200,
    method: 'Debit Card',
    status: 'completed',
    date: '2024-04-07'
  },
  {
    id: 3,
    transactionId: 'TXN-2024-001236',
    patient: 'Bob Wilson',
    doctor: 'Dr. Emily Williams',
    amount: 175,
    method: 'Credit Card',
    status: 'pending',
    date: '2024-04-08'
  },
  {
    id: 4,
    transactionId: 'TXN-2024-001237',
    patient: 'Alice Brown',
    doctor: 'Dr. James Brown',
    amount: 150,
    method: 'PayPal',
    status: 'refunded',
    date: '2024-04-06'
  }
])

const filteredPayments = computed(() => {
  return payments.value.filter(payment => {
    const matchesSearch = !filters.value.search || 
      payment.transactionId.toLowerCase().includes(filters.value.search.toLowerCase()) ||
      payment.patient.toLowerCase().includes(filters.value.search.toLowerCase())
    
    const matchesStatus = !filters.value.status || payment.status === filters.value.status
    
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

const viewPayment = (payment: any) => {
  showToast(`Viewing payment ${payment.transactionId}`, 'info')
}

const initiateRefund = (payment: any) => {
  if (confirm(`Initiate refund for ${payment.transactionId}?`)) {
    showToast('Refund initiated', 'success')
  }
}

const exportPayments = () => {
  showToast('Exporting payments...', 'info')
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

.badge-completed { background: #dcfce7; color: #166534; }
.badge-pending { background: #fef3c7; color: #92400e; }
.badge-failed { background: #fee2e2; color: #991b1b; }
.badge-refunded { background: #dbeafe; color: #1e40af; }

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

.btn-primary {
  display: flex;
  align-items: center;
  padding: 0.75rem 1.5rem;
  background: var(--primary);
  color: white;
  border-radius: 8px;
  font-weight: 500;
  transition: opacity 0.2s;
}

.btn-primary:hover {
  opacity: 0.9;
}
</style>
