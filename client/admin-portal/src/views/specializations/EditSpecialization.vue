<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="goBack" class="icon-btn">
        <ArrowLeft :size="20" />
      </button>
      <div>
        <h1 class="text-2xl font-semibold" style="color: var(--text-primary)">Edit Specialization</h1>
        <p style="color: var(--text-secondary)">Edit medical specialization</p>
      </div>
    </div>

    <form @submit.prevent="submitForm" class="card">
      <div class="space-y-4">
        <div>
          <label class="label">Specialization Name *</label>
          <input v-model="form.name" type="text" required class="input-field" placeholder="e.g., Cardiology" />
        </div>

        <div>
          <label class="label">Description *</label>
          <textarea v-model="form.description" rows="3" required class="input-field" placeholder="Brief description of the specialization"></textarea>
        </div>

        <div>
          <label class="label">Color Theme</label>
          <div class="grid grid-cols-6 gap-3">
            <button
              v-for="color in colors"
              :key="color"
              type="button"
              @click="form.color = color"
              class="color-option"
              :class="{ 'selected': form.color === color }"
              :style="{ backgroundColor: color }"
            >
              <Check v-if="form.color === color" :size="20" class="text-white" />
            </button>
          </div>
        </div>

        <div>
          <label class="label">Status</label>
          <select v-model="form.status" class="input-field">
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
      </div>

      <div class="flex justify-end space-x-3 mt-6 pt-6 border-t" style="border-color: var(--border-color)">
        <button type="button" @click="goBack" class="btn-secondary">Cancel</button>
        <button type="submit" class="btn-primary">Edit Specialization</button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Check } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { showToast } = useToast()

const form = ref({
  name: '',
  description: '',
  color: '#3b82f6',
  status: 'active'
})

const colors = [
  '#ef4444', '#f59e0b', '#10b981', '#3b82f6', 
  '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'
]

const goBack = () => router.push('/specializations')

const submitForm = () => {
  showToast('Specialization updated successfully!', 'success')
  setTimeout(() => router.push('/specializations'), 1500)
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

.color-option {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 2px solid transparent;
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.selected {
  border-color: var(--text-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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
