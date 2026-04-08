<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Hospitals</h1>
        <p style="color: var(--text-secondary)">Manage hospital listings</p>
      </div>
      <button @click="addHospital" class="btn-primary">
        <Plus :size="20" class="mr-2" />
        Add Hospital
      </button>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <input
            v-model="filters.search"
            type="text"
            placeholder="Search hospitals..."
            class="input-field"
          />
        </div>
        <div>
          <select v-model="filters.city" class="input-field">
            <option value="">All Cities</option>
            <option value="New York">New York</option>
            <option value="Los Angeles">Los Angeles</option>
            <option value="Chicago">Chicago</option>
          </select>
        </div>
        <div>
          <select v-model="filters.status" class="input-field">
            <option value="">All Status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Hospitals Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="hospital in filteredHospitals" :key="hospital.id" class="hospital-card">
        <div class="hospital-image">
          <Building2 :size="48" style="color: var(--text-secondary)" />
        </div>
        <div class="p-4">
          <h3 class="text-lg font-semibold mb-2" style="color: var(--text-primary)">{{ hospital.name }}</h3>
          <div class="space-y-2 text-sm">
            <div class="flex items-center" style="color: var(--text-secondary)">
              <MapPin :size="16" class="mr-2" />
              {{ hospital.address }}, {{ hospital.city }}
            </div>
            <div class="flex items-center" style="color: var(--text-secondary)">
              <Phone :size="16" class="mr-2" />
              {{ hospital.phone }}
            </div>
            <div class="flex items-center justify-between mt-3">
              <span class="badge" :class="`badge-${hospital.status}`">{{ hospital.status }}</span>
              <span style="color: var(--text-secondary)">{{ hospital.doctors }} doctors</span>
            </div>
          </div>
          <div class="flex items-center space-x-2 mt-4 pt-4 border-t" style="border-color: var(--border-color)">
            <button @click="viewHospital(hospital)" class="btn-secondary flex-1">
              <Eye :size="16" class="mr-1" />
              View
            </button>
            <button @click="editHospital(hospital)" class="btn-secondary flex-1">
              <Edit :size="16" class="mr-1" />
              Edit
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Building2, MapPin, Phone, Eye, Edit } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

const router = useRouter()

const filters = ref({
  search: '',
  city: '',
  status: ''
})

const hospitals = ref([
  {
    id: 1,
    name: 'City General Hospital',
    address: '123 Main St',
    city: 'New York',
    phone: '+1 (555) 123-4567',
    status: 'active',
    doctors: 45
  },
  {
    id: 2,
    name: 'Metro Medical Center',
    address: '456 Oak Ave',
    city: 'Los Angeles',
    phone: '+1 (555) 234-5678',
    status: 'active',
    doctors: 38
  },
  {
    id: 3,
    name: 'Riverside Hospital',
    address: '789 River Rd',
    city: 'Chicago',
    phone: '+1 (555) 345-6789',
    status: 'inactive',
    doctors: 22
  }
])

const filteredHospitals = computed(() => {
  return hospitals.value.filter(hospital => {
    const matchesSearch = !filters.value.search || 
      hospital.name.toLowerCase().includes(filters.value.search.toLowerCase())
    const matchesCity = !filters.value.city || hospital.city === filters.value.city
    const matchesStatus = !filters.value.status || hospital.status === filters.value.status
    
    return matchesSearch && matchesCity && matchesStatus
  })
})

const addHospital = () => {
  router.push('/hospitals/add')
}

const viewHospital = (hospital: any) => {
  router.push(`/hospitals/${hospital.id}`)
}

const editHospital = (hospital: any) => {
  router.push(`/hospitals/edit/${hospital.id}`)
}
</script>

<style scoped>
.card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.5rem;
}

.input-field {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.hospital-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.hospital-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.hospital-image {
  height: 150px;
  background: var(--bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
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

.btn-secondary {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 0.875rem;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: var(--bg-hover);
}
</style>
