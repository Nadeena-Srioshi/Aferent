<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="goBack" class="icon-btn">
        <ArrowLeft :size="20" />
      </button>
      <div>
        <h1 class="text-2xl font-semibold" style="color: var(--text-primary)">Edit Hospital</h1>
        <p style="color: var(--text-secondary)">Edit hospital to the system</p>
      </div>
    </div>

    <form @submit.prevent="submitForm" class="space-y-6">
      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Basic Information</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="label">Hospital Name *</label>
            <input v-model="form.name" type="text" required class="input-field" />
          </div>
          <div>
            <label class="label">Email *</label>
            <input v-model="form.email" type="email" required class="input-field" />
          </div>
          <div>
            <label class="label">Phone *</label>
            <input v-model="form.phone" type="tel" required class="input-field" />
          </div>
          <div>
            <label class="label">Website</label>
            <input v-model="form.website" type="url" class="input-field" />
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Address</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="md:col-span-2">
            <label class="label">Street Address *</label>
            <input v-model="form.address" type="text" required class="input-field" />
          </div>
          <div>
            <label class="label">City *</label>
            <input v-model="form.city" type="text" required class="input-field" />
          </div>
          <div>
            <label class="label">State *</label>
            <input v-model="form.state" type="text" required class="input-field" />
          </div>
          <div>
            <label class="label">ZIP Code *</label>
            <input v-model="form.zip" type="text" required class="input-field" />
          </div>
          <div>
            <label class="label">Country *</label>
            <input v-model="form.country" type="text" required class="input-field" />
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Facilities</h3>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
          <label v-for="facility in availableFacilities" :key="facility" class="checkbox-label">
            <input type="checkbox" :value="facility" v-model="form.facilities" class="checkbox" />
            <span>{{ facility }}</span>
          </label>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="goBack" class="btn-secondary">Cancel</button>
        <button type="submit" class="btn-primary">Edit Hospital</button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { showToast } = useToast()

const form = ref({
  name: '',
  email: '',
  phone: '',
  website: '',
  address: '',
  city: '',
  state: '',
  zip: '',
  country: 'USA',
  facilities: []
})

const availableFacilities = [
  'Emergency Care',
  'ICU',
  'Surgery',
  'Radiology',
  'Laboratory',
  'Pharmacy',
  'Cardiology',
  'Neurology',
  'Pediatrics',
  'Orthopedics',
  'Oncology',
  'Maternity'
]

const goBack = () => router.push('/hospitals')

const submitForm = () => {
  showToast('Hospital updated successfully!', 'success')
  setTimeout(() => router.push('/hospitals'), 1500)
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

.label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--text-primary);
}

.input-field {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.input-field:focus {
  outline: none;
  border-color: var(--primary);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}

.checkbox-label:hover {
  background: var(--bg-hover);
}

.checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.btn-primary, .btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 500;
  transition: opacity 0.2s;
}

.btn-primary {
  background: var(--primary);
  color: white;
}

.btn-secondary {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn-primary:hover, .btn-secondary:hover {
  opacity: 0.9;
}
</style>
