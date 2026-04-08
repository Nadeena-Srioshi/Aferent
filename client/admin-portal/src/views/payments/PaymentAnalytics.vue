<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Payment Analytics</h1>
      <p style="color: var(--text-secondary)">Revenue insights and payment trends</p>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm" style="color: var(--text-secondary)">Total Revenue</p>
            <p class="text-3xl font-bold mt-2" style="color: var(--text-primary)">$245,678</p>
            <p class="text-sm mt-2 text-green-600">↑ 12.5% vs last month</p>
          </div>
          <div class="stat-icon" style="background-color: rgba(139, 92, 246, 0.2); color: #8b5cf6;">
            <DollarSign :size="32" />
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm" style="color: var(--text-secondary)">Avg Transaction</p>
            <p class="text-3xl font-bold mt-2" style="color: var(--text-primary)">$165</p>
            <p class="text-sm mt-2 text-green-600">↑ 3.2% vs last month</p>
          </div>
          <div class="stat-icon" style="background-color: rgba(16, 185, 129, 0.2); color: #10b981;">
            <TrendingUp :size="32" />
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm" style="color: var(--text-secondary)">Success Rate</p>
            <p class="text-3xl font-bold mt-2" style="color: var(--text-primary)">96.8%</p>
            <p class="text-sm mt-2 text-green-600">↑ 1.2% vs last month</p>
          </div>
          <div class="stat-icon" style="background-color: rgba(34, 197, 94, 0.2); color: #22c55e;">
            <CheckCircle :size="32" />
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm" style="color: var(--text-secondary)">Refund Rate</p>
            <p class="text-3xl font-bold mt-2" style="color: var(--text-primary)">2.1%</p>
            <p class="text-sm mt-2 text-red-600">↓ 0.5% vs last month</p>
          </div>
          <div class="stat-icon" style="background-color: rgba(239, 68, 68, 0.2); color: #ef4444;">
            <RefreshCcw :size="32" />
          </div>
        </div>
      </div>
    </div>

    <!-- Charts -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Revenue Trend</h3>
        <div class="chart-container">
          <canvas ref="revenueChart"></canvas>
        </div>
      </div>

      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Payment Methods</h3>
        <div class="chart-container">
          <canvas ref="methodsChart"></canvas>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Transaction Status</h3>
        <div class="chart-container">
          <canvas ref="statusChart"></canvas>
        </div>
      </div>

      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Top Earning Doctors</h3>
        <div class="space-y-3">
          <div v-for="doctor in topDoctors" :key="doctor.id" class="doctor-item">
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-3">
                <div class="avatar">{{ doctor.name.charAt(0) }}</div>
                <div>
                  <p class="font-medium" style="color: var(--text-primary)">{{ doctor.name }}</p>
                  <p class="text-sm" style="color: var(--text-secondary)">{{ doctor.appointments }} appointments</p>
                </div>
              </div>
              <div class="text-right">
                <p class="font-bold" style="color: var(--text-primary)">${{ doctor.revenue.toLocaleString() }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { DollarSign, TrendingUp, CheckCircle, RefreshCcw } from 'lucide-vue-next'
import Chart from 'chart.js/auto'

const revenueChart = ref<HTMLCanvasElement>()
const methodsChart = ref<HTMLCanvasElement>()
const statusChart = ref<HTMLCanvasElement>()

const topDoctors = ref([
  { id: 1, name: 'Dr. Sarah Johnson', appointments: 145, revenue: 21750 },
  { id: 2, name: 'Dr. Michael Chen', appointments: 132, revenue: 19800 },
  { id: 3, name: 'Dr. Emily Williams', appointments: 128, revenue: 19200 },
  { id: 4, name: 'Dr. James Brown', appointments: 115, revenue: 17250 },
  { id: 5, name: 'Dr. Lisa Anderson', appointments: 108, revenue: 16200 }
])

onMounted(() => {
  if (revenueChart.value) {
    new Chart(revenueChart.value, {
      type: 'line',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [{
          label: 'Revenue ($)',
          data: [35000, 42000, 38000, 45000, 48000, 52000],
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.1)',
          tension: 0.4,
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } }
      }
    })
  }

  if (methodsChart.value) {
    new Chart(methodsChart.value, {
      type: 'doughnut',
      data: {
        labels: ['Credit Card', 'Debit Card', 'PayPal', 'Other'],
        datasets: [{
          data: [45, 30, 20, 5],
          backgroundColor: ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' } }
      }
    })
  }

  if (statusChart.value) {
    new Chart(statusChart.value, {
      type: 'bar',
      data: {
        labels: ['Completed', 'Pending', 'Failed', 'Refunded'],
        datasets: [{
          label: 'Transactions',
          data: [1234, 45, 12, 23],
          backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#3b82f6']
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } }
      }
    })
  }
})
</script>

<style scoped>
.stat-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.5rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.5rem;
}

.chart-container {
  height: 300px;
}

.doctor-item {
  padding: 1rem;
  background: var(--bg-secondary);
  border-radius: 8px;
  transition: background 0.2s;
}

.doctor-item:hover {
  background: var(--bg-hover);
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
</style>
