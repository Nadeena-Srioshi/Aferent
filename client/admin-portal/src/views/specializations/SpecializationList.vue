<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Specializations</h1>
        <p style="color: var(--text-secondary)">Manage medical specializations</p>
      </div>
      <button @click="addSpecialization" class="btn-primary">
        <Plus :size="20" class="mr-2" />
        Add Specialization
      </button>
    </div>

    <!-- Search -->
    <div class="card">
      <input v-model="searchQuery" type="text" placeholder="Search specializations..." class="input-field" />
    </div>

    <!-- Specializations Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="spec in filteredSpecializations" :key="spec.id" class="spec-card">
        <div class="spec-icon" :style="{ backgroundColor: spec.color + '20', color: spec.color }">
          <component :is="spec.icon" :size="32" />
        </div>
        <div class="mt-4">
          <h3 class="text-lg font-semibold mb-2" style="color: var(--text-primary)">{{ spec.name }}</h3>
          <p class="text-sm mb-4" style="color: var(--text-secondary)">{{ spec.description }}</p>
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm" style="color: var(--text-secondary)">{{ spec.doctorCount }} doctors</span>
            <span class="badge" :class="`badge-${spec.status}`">{{ spec.status }}</span>
          </div>
          <div class="flex items-center space-x-2">
            <button @click="editSpecialization(spec)" class="btn-secondary flex-1">
              <Edit :size="16" class="mr-1" />
              Edit
            </button>
            <button @click="toggleStatus(spec)" class="icon-btn">
              <component :is="spec.status === 'active' ? Lock : Unlock" :size="18" />
            </button>
            <button @click="deleteSpecialization(spec)" class="icon-btn text-red-600">
              <Trash2 :size="18" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Edit, Lock, Unlock, Trash2, Heart, Brain, Baby, Bone, Eye, Pill } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { showToast } = useToast()

const searchQuery = ref('')

const specializations = ref([
  {
    id: 1,
    name: 'Cardiology',
    description: 'Heart and cardiovascular system',
    doctorCount: 45,
    status: 'active',
    icon: Heart,
    color: '#ef4444'
  },
  {
    id: 2,
    name: 'Neurology',
    description: 'Brain and nervous system',
    doctorCount: 32,
    status: 'active',
    icon: Brain,
    color: '#8b5cf6'
  },
  {
    id: 3,
    name: 'Pediatrics',
    description: 'Children and infants care',
    doctorCount: 56,
    status: 'active',
    icon: Baby,
    color: '#10b981'
  },
  {
    id: 4,
    name: 'Orthopedics',
    description: 'Bones and musculoskeletal system',
    doctorCount: 38,
    status: 'active',
    icon: Bone,
    color: '#f59e0b'
  },
  {
    id: 5,
    name: 'Ophthalmology',
    description: 'Eye and vision care',
    doctorCount: 24,
    status: 'active',
    icon: Eye,
    color: '#3b82f6'
  },
  {
    id: 6,
    name: 'General Medicine',
    description: 'Primary care and general health',
    doctorCount: 78,
    status: 'active',
    icon: Pill,
    color: '#06b6d4'
  }
])

const filteredSpecializations = computed(() => {
  if (!searchQuery.value) return specializations.value
  
  return specializations.value.filter(spec =>
    spec.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    spec.description.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const addSpecialization = () => {
  router.push('/specializations/add')
}

const editSpecialization = (spec: any) => {
  router.push(`/specializations/edit/${spec.id}`)
}

const toggleStatus = (spec: any) => {
  spec.status = spec.status === 'active' ? 'inactive' : 'active'
  showToast(`Specialization ${spec.status === 'active' ? 'activated' : 'deactivated'}`, 'success')
}

const deleteSpecialization = (spec: any) => {
  if (confirm(`Delete ${spec.name}?`)) {
    specializations.value = specializations.value.filter(s => s.id !== spec.id)
    showToast('Specialization deleted', 'success')
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

.input-field {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.spec-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.5rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.spec-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.spec-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
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
</style>
