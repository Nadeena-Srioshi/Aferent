<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">FAQ Management</h1>
        <p style="color: var(--text-secondary)">Manage frequently asked questions</p>
      </div>
      <button @click="addFAQ" class="btn-primary">
        <Plus :size="20" class="mr-2" />
        Add FAQ
      </button>
    </div>

    <!-- Search & Filter -->
    <div class="card">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <input v-model="searchQuery" type="text" placeholder="Search FAQs..." class="input-field" />
        <select v-model="filterCategory" class="input-field">
          <option value="">All Categories</option>
          <option value="general">General</option>
          <option value="appointments">Appointments</option>
          <option value="payments">Payments</option>
          <option value="doctors">Doctors</option>
        </select>
      </div>
    </div>

    <!-- FAQs List -->
    <div class="space-y-4">
      <div v-for="faq in filteredFAQs" :key="faq.id" class="faq-card">
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center space-x-3 mb-2">
              <span class="badge">{{ faq.category }}</span>
              <span class="badge" :class="`badge-${faq.status}`">{{ faq.status }}</span>
            </div>
            <h3 class="text-lg font-semibold mb-2" style="color: var(--text-primary)">{{ faq.question }}</h3>
            <p style="color: var(--text-secondary)">{{ faq.answer }}</p>
            <p class="text-sm mt-3" style="color: var(--text-muted)">Last updated: {{ formatDate(faq.updatedAt) }}</p>
          </div>
          <div class="flex items-center space-x-2 ml-4">
            <button @click="editFAQ(faq)" class="icon-btn" title="Edit">
              <Edit :size="18" />
            </button>
            <button @click="toggleStatus(faq)" class="icon-btn" title="Toggle Status">
              <component :is="faq.status === 'active' ? Lock : Unlock" :size="18" />
            </button>
            <button @click="deleteFAQ(faq)" class="icon-btn text-red-600" title="Delete">
              <Trash2 :size="18" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h2 class="text-xl font-semibold mb-4" style="color: var(--text-primary)">
          {{ editingFAQ ? 'Edit FAQ' : 'Add FAQ' }}
        </h2>
        <form @submit.prevent="saveFAQ" class="space-y-4">
          <div>
            <label class="label">Category *</label>
            <select v-model="faqForm.category" required class="input-field">
              <option value="general">General</option>
              <option value="appointments">Appointments</option>
              <option value="payments">Payments</option>
              <option value="doctors">Doctors</option>
            </select>
          </div>
          <div>
            <label class="label">Question *</label>
            <input v-model="faqForm.question" type="text" required class="input-field" />
          </div>
          <div>
            <label class="label">Answer *</label>
            <textarea v-model="faqForm.answer" rows="4" required class="input-field"></textarea>
          </div>
          <div>
            <label class="label">Status</label>
            <select v-model="faqForm.status" class="input-field">
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>
          <div class="flex justify-end space-x-3 pt-4 border-t" style="border-color: var(--border-color)">
            <button type="button" @click="closeModal" class="btn-secondary">Cancel</button>
            <button type="submit" class="btn-primary">Save FAQ</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Edit, Lock, Unlock, Trash2 } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

const searchQuery = ref('')
const filterCategory = ref('')
const showModal = ref(false)
const editingFAQ = ref<any>(null)

const faqForm = ref({
  category: 'general',
  question: '',
  answer: '',
  status: 'active'
})

const faqs = ref([
  {
    id: 1,
    category: 'general',
    question: 'What is this platform?',
    answer: 'This is a healthcare platform connecting patients with qualified doctors.',
    status: 'active',
    updatedAt: '2024-04-01'
  },
  {
    id: 2,
    category: 'appointments',
    question: 'How do I book an appointment?',
    answer: 'You can book an appointment by selecting a doctor and choosing an available time slot.',
    status: 'active',
    updatedAt: '2024-04-02'
  },
  {
    id: 3,
    category: 'payments',
    question: 'What payment methods are accepted?',
    answer: 'We accept credit cards, debit cards, and PayPal.',
    status: 'active',
    updatedAt: '2024-04-03'
  }
])

const filteredFAQs = computed(() => {
  return faqs.value.filter(faq => {
    const matchesSearch = !searchQuery.value || 
      faq.question.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      faq.answer.toLowerCase().includes(searchQuery.value.toLowerCase())
    
    const matchesCategory = !filterCategory.value || faq.category === filterCategory.value
    
    return matchesSearch && matchesCategory
  })
})

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const addFAQ = () => {
  editingFAQ.value = null
  faqForm.value = { category: 'general', question: '', answer: '', status: 'active' }
  showModal.value = true
}

const editFAQ = (faq: any) => {
  editingFAQ.value = faq
  faqForm.value = { ...faq }
  showModal.value = true
}

const saveFAQ = () => {
  if (editingFAQ.value) {
    Object.assign(editingFAQ.value, faqForm.value)
    showToast('FAQ updated successfully', 'success')
  } else {
    faqs.value.push({
      id: faqs.value.length + 1,
      ...faqForm.value,
      updatedAt: new Date().toISOString().split('T')[0] as string
    })
    showToast('FAQ added successfully', 'success')
  }
  closeModal()
}

const toggleStatus = (faq: any) => {
  faq.status = faq.status === 'active' ? 'inactive' : 'active'
  showToast(`FAQ ${faq.status === 'active' ? 'activated' : 'deactivated'}`, 'success')
}

const deleteFAQ = (faq: any) => {
  if (confirm('Delete this FAQ?')) {
    faqs.value = faqs.value.filter(f => f.id !== faq.id)
    showToast('FAQ deleted', 'success')
  }
}

const closeModal = () => {
  showModal.value = false
  editingFAQ.value = null
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

.faq-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.5rem;
  transition: box-shadow 0.2s;
}

.faq-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: capitalize;
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.badge-active { background: #dcfce7; color: #166534; }
.badge-inactive { background: #fee2e2; color: #991b1b; }

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
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.modal-content {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 2rem;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--text-primary);
}
</style>
