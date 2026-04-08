<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="goBack" class="icon-btn">
        <ArrowLeft :size="20" />
      </button>
      <div>
        <h1 class="text-2xl font-semibold" style="color: var(--text-primary)">{{ hospital.name }}</h1>
        <p style="color: var(--text-secondary)">Hospital details and information</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-6">
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Basic Information</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Hospital Name</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ hospital.name }}</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Status</label>
              <p class="mt-1"><span class="badge" :class="`badge-${hospital.status}`">{{ hospital.status }}</span></p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Phone</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ hospital.phone }}</p>
            </div>
            <div>
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Email</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ hospital.email }}</p>
            </div>
            <div class="md:col-span-2">
              <label class="text-sm font-medium" style="color: var(--text-secondary)">Address</label>
              <p class="mt-1 font-medium" style="color: var(--text-primary)">{{ hospital.address }}, {{ hospital.city }}, {{ hospital.state }} {{ hospital.zip }}</p>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Facilities & Services</h3>
          <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
            <div v-for="facility in hospital.facilities" :key="facility" class="facility-item">
              <CheckCircle :size="16" class="text-green-600" />
              <span style="color: var(--text-primary)">{{ facility }}</span>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Doctors ({{ hospital.doctorsList.length }})</h3>
          <div class="space-y-3">
            <div v-for="doctor in hospital.doctorsList" :key="doctor.id" class="doctor-item">
              <div class="flex items-center space-x-3">
                <div class="avatar">{{ doctor.name.charAt(0) }}</div>
                <div>
                  <p class="font-medium" style="color: var(--text-primary)">{{ doctor.name }}</p>
                  <p class="text-sm" style="color: var(--text-secondary)">{{ doctor.specialization }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-6">
        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Quick Stats</h3>
          <div class="space-y-3">
            <div class="stat-item">
              <Users :size="20" style="color: var(--text-secondary)" />
              <div>
                <p class="text-sm" style="color: var(--text-secondary)">Total Doctors</p>
                <p class="text-xl font-bold" style="color: var(--text-primary)">{{ hospital.doctorsList.length }}</p>
              </div>
            </div>
            <div class="stat-item">
              <Calendar :size="20" style="color: var(--text-secondary)" />
              <div>
                <p class="text-sm" style="color: var(--text-secondary)">Total Appointments</p>
                <p class="text-xl font-bold" style="color: var(--text-primary)">1,234</p>
              </div>
            </div>
            <div class="stat-item">
              <Star :size="20" style="color: var(--text-secondary)" />
              <div>
                <p class="text-sm" style="color: var(--text-secondary)">Rating</p>
                <p class="text-xl font-bold" style="color: var(--text-primary)">4.8</p>
              </div>
            </div>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Actions</h3>
          <div class="space-y-3">
            <button @click="editHospital" class="btn-secondary w-full">
              <Edit :size="16" class="mr-2" />
              Edit Hospital
            </button>
            <button @click="toggleStatus" class="btn-secondary w-full">
              <component :is="hospital.status === 'active' ? Lock : Unlock" :size="16" class="mr-2" />
              {{ hospital.status === 'active' ? 'Deactivate' : 'Activate' }}
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
import { ArrowLeft, CheckCircle, Users, Calendar, Star, Edit, Lock, Unlock } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const { showToast } = useToast()

const hospital = ref({
  id: route.params.id,
  name: 'City General Hospital',
  status: 'active',
  phone: '+1 (555) 123-4567',
  email: 'info@citygeneralhospital.com',
  address: '123 Main St',
  city: 'New York',
  state: 'NY',
  zip: '10001',
  facilities: ['Emergency Care', 'ICU', 'Surgery', 'Radiology', 'Laboratory', 'Pharmacy'],
  doctorsList: [
    { id: 1, name: 'Dr. John Smith', specialization: 'Cardiology' },
    { id: 2, name: 'Dr. Sarah Johnson', specialization: 'Neurology' },
    { id: 3, name: 'Dr. Michael Chen', specialization: 'Pediatrics' }
  ]
})

const goBack = () => {
  router.push('/hospitals')
}

const editHospital = () => {
  router.push(`/hospitals/edit/${hospital.value.id}`)
}

const toggleStatus = () => {
  hospital.value.status = hospital.value.status === 'active' ? 'inactive' : 'active'
  showToast(`Hospital ${hospital.value.status === 'active' ? 'activated' : 'deactivated'}`, 'success')
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

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: capitalize;
}

.badge-active { background: #dcfce7; color: #166534; }
.badge-inactive { background: #fee2e2; color: #991b1b; }

.facility-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: var(--bg-secondary);
  border-radius: 6px;
}

.doctor-item {
  padding: 0.75rem;
  background: var(--bg-secondary);
  border-radius: 8px;
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

.stat-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  background: var(--bg-secondary);
  border-radius: 8px;
}

.btn-secondary {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.75rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: var(--bg-hover);
}
</style>
