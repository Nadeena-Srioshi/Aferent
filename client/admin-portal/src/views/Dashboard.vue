<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Dashboard</h1>
      <p style="color: var(--text-secondary)">Overview of your admin portal</p>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm" style="color: var(--text-secondary)">{{ stat.label }}</p>
            <p class="text-3xl font-bold mt-2" style="color: var(--text-primary)">{{ stat.value }}</p>
            <p class="text-sm mt-2" :class="stat.trend >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ stat.trend >= 0 ? '↑' : '↓' }} {{ Math.abs(stat.trend) }}% from last month
            </p>
          </div>
          <div class="stat-icon" :style="{ backgroundColor: stat.color + '20', color: stat.color }">
            <component :is="stat.icon" />
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Appointments Chart -->
      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Appointments Overview</h3>
        <div class="chart-container">
          <canvas ref="appointmentsChart"></canvas>
        </div>
      </div>

      <!-- Revenue Chart -->
      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Revenue Trend</h3>
        <div class="chart-container">
          <canvas ref="revenueChart"></canvas>
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="card">
      <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Recent Activity</h3>
      <div class="space-y-3">
        <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
          <div class="flex items-start space-x-3">
            <div class="activity-icon" :style="{ backgroundColor: activity.color + '20', color: activity.color }">
              <component :is="activity.icon" />
            </div>
            <div class="flex-1">
              <p class="font-medium" style="color: var(--text-primary)">{{ activity.title }}</p>
              <p class="text-sm" style="color: var(--text-secondary)">{{ activity.description }}</p>
              <p class="text-xs mt-1" style="color: var(--text-muted)">{{ activity.time }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Users, Stethoscope, Calendar, DollarSign, UserPlus, CheckCircle, XCircle, Clock } from 'lucide-vue-next'
import Chart from 'chart.js/auto'

const stats = ref([
  { label: 'Total Users', value: '12,345', trend: 12.5, color: '#3b82f6', icon: Users },
  { label: 'Active Doctors', value: '856', trend: 8.2, color: '#10b981', icon: Stethoscope },
  { label: 'Appointments', value: '3,421', trend: -3.1, color: '#f59e0b', icon: Calendar },
  { label: 'Revenue', value: '$45,678', trend: 15.3, color: '#8b5cf6', icon: DollarSign }
])

const recentActivities = ref([
  {
    id: 1,
    title: 'New Doctor Registration',
    description: 'Dr. Sarah Johnson submitted verification documents',
    time: '5 minutes ago',
    icon: UserPlus,
    color: '#3b82f6'
  },
  {
    id: 2,
    title: 'Doctor Verified',
    description: 'Dr. Michael Chen has been verified and approved',
    time: '1 hour ago',
    icon: CheckCircle,
    color: '#10b981'
  },
  {
    id: 3,
    title: 'Payment Processed',
    description: 'Payment of $150 received for appointment #4521',
    time: '2 hours ago',
    icon: DollarSign,
    color: '#8b5cf6'
  },
  {
    id: 4,
    title: 'Refund Requested',
    description: 'User John Doe requested refund for appointment #4519',
    time: '3 hours ago',
    icon: XCircle,
    color: '#ef4444'
  },
  {
    id: 5,
    title: 'Appointment Scheduled',
    description: '15 new appointments scheduled today',
    time: '5 hours ago',
    icon: Clock,
    color: '#f59e0b'
  }
])

const appointmentsChart = ref<HTMLCanvasElement>()
const revenueChart = ref<HTMLCanvasElement>()

onMounted(() => {
  if (appointmentsChart.value) {
    new Chart(appointmentsChart.value, {
      type: 'line',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [
          {
            label: 'Completed',
            data: [65, 78, 85, 92, 88, 95],
            borderColor: '#10b981',
            backgroundColor: 'rgba(16, 185, 129, 0.1)',
            tension: 0.4
          },
          {
            label: 'Cancelled',
            data: [12, 15, 10, 8, 11, 9],
            borderColor: '#ef4444',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            tension: 0.4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
      }
    })
  }

  if (revenueChart.value) {
    new Chart(revenueChart.value, {
      type: 'bar',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
        datasets: [
          {
            label: 'Revenue ($)',
            data: [12000, 15000, 13500, 18000, 16500, 20000],
            backgroundColor: '#8b5cf6'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
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
  width: 48px;
  height: 48px;
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

.activity-item {
  padding: 1rem;
  background: var(--bg-secondary);
  border-radius: 8px;
  transition: background 0.2s;
}

.activity-item:hover {
  background: var(--bg-hover);
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>
