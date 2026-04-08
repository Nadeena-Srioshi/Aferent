<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">Privacy Policy</h1>
        <p style="color: var(--text-secondary)">Manage privacy policy content</p>
      </div>
      <div class="flex space-x-3">
        <button @click="preview" class="btn-secondary">
          <Eye :size="20" class="mr-2" />
          Preview
        </button>
        <button @click="saveContent" class="btn-primary">
          <Save :size="20" class="mr-2" />
          Save Changes
        </button>
      </div>
    </div>

    <!-- Editor -->
    <div class="card">
      <div class="mb-4">
        <label class="label">Last Updated</label>
        <p style="color: var(--text-secondary)">{{ formatDate(content.lastUpdated) }}</p>
      </div>

      <div>
        <label class="label">Content *</label>
        <textarea v-model="content.text" rows="20" class="input-field font-mono text-sm"></textarea>
        <p class="text-sm mt-2" style="color: var(--text-secondary)">
          Supports Markdown formatting
        </p>
      </div>

      <div class="mt-4">
        <label class="label">Status</label>
        <select v-model="content.status" class="input-field max-w-xs">
          <option value="draft">Draft</option>
          <option value="published">Published</option>
        </select>
      </div>
    </div>

    <!-- Version History -->
    <div class="card">
      <h3 class="text-lg font-semibold mb-4" style="color: var(--text-primary)">Version History</h3>
      <div class="space-y-3">
        <div v-for="version in versions" :key="version.id" class="version-item">
          <div class="flex items-center justify-between">
            <div>
              <p class="font-medium" style="color: var(--text-primary)">Version {{ version.version }}</p>
              <p class="text-sm" style="color: var(--text-secondary)">
                Updated by {{ version.updatedBy }} on {{ formatDate(version.date) }}
              </p>
            </div>
            <button @click="restoreVersion(version)" class="btn-secondary-sm">
              <RotateCcw :size="16" class="mr-1" />
              Restore
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Eye, Save, RotateCcw } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

const content = ref({
  text: `# Privacy Policy

Last updated: April 8, 2024

## 1. Acceptance of Terms

By accessing and using this platform, you accept and agree to be bound by the terms and provision of this agreement.

## 2. Use License

Permission is granted to temporarily access the services for personal, non-commercial use only.

## 3. User Accounts

You are responsible for maintaining the confidentiality of your account and password.

## 4. Privacy Policy

Your use of our platform is also governed by our Privacy Policy.

## 5. Service Modifications

We reserve the right to modify or discontinue the service with or without notice.

## 6. Limitation of Liability

We shall not be liable for any indirect, incidental, special, consequential or punitive damages.

## 7. Governing Law

These terms shall be governed by and construed in accordance with applicable laws.

## 8. Contact Information

For questions about these terms, please contact us at legal@example.com`,
  lastUpdated: '2024-04-08',
  status: 'published'
})

const versions = ref([
  {
    id: 1,
    version: '1.2',
    updatedBy: 'Admin User',
    date: '2024-04-08'
  },
  {
    id: 2,
    version: '1.1',
    updatedBy: 'Admin User',
    date: '2024-03-15'
  },
  {
    id: 3,
    version: '1.0',
    updatedBy: 'Admin User',
    date: '2024-01-01'
  }
])

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const saveContent = () => {
  content.value.lastUpdated = new Date().toISOString().split('T')[0] as string
  showToast('Privacy Policy saved successfully', 'success')
}

const preview = () => {
  showToast('Opening preview...', 'info')
}

const restoreVersion = (version: any) => {
  if (confirm(`Restore version ${version.version}?`)) {
    showToast(`Restored version ${version.version}`, 'success')
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

.label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--text-primary);
}

.input-field {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  resize: vertical;
}

.version-item {
  padding: 1rem;
  background: var(--bg-secondary);
  border-radius: 8px;
}

.btn-primary, .btn-secondary {
  display: flex;
  align-items: center;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 500;
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

.btn-secondary-sm {
  display: flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 0.875rem;
}
</style>
