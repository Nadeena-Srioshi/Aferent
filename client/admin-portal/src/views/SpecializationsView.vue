<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { referenceApi, type Specialization } from '@/api/reference'

const specializations = ref<Specialization[]>([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const sortDir = ref<'asc' | 'desc'>('asc')

const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const editingId = ref<string | null>(null)
const formName = ref('')
const formError = ref('')
const formSubmitting = ref(false)

const showDeleteModal = ref(false)
const deletingSpecialization = ref<Specialization | null>(null)
const deleteSubmitting = ref(false)

onMounted(fetchSpecializations)

async function fetchSpecializations() {
  loading.value = true
  error.value = ''
  try {
    const res = await referenceApi.getAllSpecializations()
    specializations.value = res.data
  } catch {
    error.value = 'Failed to load specializations.'
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  let list = [...specializations.value]
  const q = search.value.trim().toLowerCase()
  if (q) {
    list = list.filter(s => s.name.toLowerCase().includes(q))
  }
  list.sort((a, b) => {
    const compare = a.name.localeCompare(b.name)
    return sortDir.value === 'asc' ? compare : -compare
  })
  return list
})

function openAddModal() {
  modalMode.value = 'add'
  editingId.value = null
  formName.value = ''
  formError.value = ''
  showModal.value = true
}

function openEditModal(specialization: Specialization) {
  modalMode.value = 'edit'
  editingId.value = specialization.id
  formName.value = specialization.name
  formError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  formName.value = ''
  formError.value = ''
  editingId.value = null
}

async function submitForm() {
  formError.value = ''
  const name = formName.value.trim()
  if (!name) {
    formError.value = 'Name is required'
    return
  }
  formSubmitting.value = true
  try {
    if (modalMode.value === 'add') {
      await referenceApi.addSpecialization({ name })
    } else if (editingId.value) {
      await referenceApi.updateSpecialization(editingId.value, { name })
    }
    await fetchSpecializations()
    closeModal()
  } catch (err: any) {
    formError.value = err.response?.data?.message || 'Operation failed'
  } finally {
    formSubmitting.value = false
  }
}

function openDeleteModal(specialization: Specialization) {
  deletingSpecialization.value = specialization
  showDeleteModal.value = true
}

function closeDeleteModal() {
  showDeleteModal.value = false
  deletingSpecialization.value = null
}

async function confirmDelete() {
  if (!deletingSpecialization.value) return
  deleteSubmitting.value = true
  try {
    await referenceApi.deleteSpecialization(deletingSpecialization.value.id)
    await fetchSpecializations()
    closeDeleteModal()
  } catch {
    alert('Failed to delete specialization')
  } finally {
    deleteSubmitting.value = false
  }
}
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Specializations</h1>
      <button @click="openAddModal" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">
        + Add Specialization
      </button>
    </div>

    <div v-if="error" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
      {{ error }}
    </div>

    <div class="mb-4 flex gap-3">
      <input v-model="search" type="text" placeholder="Search specializations..." class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
      <button @click="sortDir = sortDir === 'asc' ? 'desc' : 'asc'" class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">
        Sort {{ sortDir === 'asc' ? '↑' : '↓' }}
      </button>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-500">Loading...</div>

    <div v-else-if="filtered.length === 0" class="text-center py-12 text-gray-500">
      {{ search ? 'No specializations found' : 'No specializations yet' }}
    </div>

    <div v-else class="bg-white rounded-lg shadow overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Name</th>
            <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Created</th>
            <th class="px-6 py-3 text-right text-xs font-semibold text-gray-600 uppercase">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr v-for="spec in filtered" :key="spec.id" class="hover:bg-gray-50">
            <td class="px-6 py-4 text-sm text-gray-900">{{ spec.name }}</td>
            <td class="px-6 py-4 text-sm text-gray-500">{{ spec.createdAt ? new Date(spec.createdAt).toLocaleDateString() : '-' }}</td>
            <td class="px-6 py-4 text-sm text-right space-x-2">
              <button @click="openEditModal(spec)" class="text-blue-600 hover:text-blue-800 font-medium">Edit</button>
              <button @click="openDeleteModal(spec)" class="text-red-600 hover:text-red-800 font-medium">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="closeModal">
      <div class="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h2 class="text-xl font-bold mb-4">{{ modalMode === 'add' ? 'Add Specialization' : 'Edit Specialization' }}</h2>
        <div v-if="formError" class="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">{{ formError }}</div>
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">Specialization Name</label>
          <input v-model="formName" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="Enter specialization name" @keyup.enter="submitForm" />
        </div>
        <div class="flex gap-3 justify-end">
          <button @click="closeModal" class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50" :disabled="formSubmitting">Cancel</button>
          <button @click="submitForm" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50" :disabled="formSubmitting">
            {{ formSubmitting ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Modal -->
    <div v-if="showDeleteModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click.self="closeDeleteModal">
      <div class="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h2 class="text-xl font-bold mb-4">Delete Specialization</h2>
        <p class="text-gray-700 mb-6">Are you sure you want to delete <strong>{{ deletingSpecialization?.name }}</strong>?</p>
        <div class="flex gap-3 justify-end">
          <button @click="closeDeleteModal" class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50" :disabled="deleteSubmitting">Cancel</button>
          <button @click="confirmDelete" class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50" :disabled="deleteSubmitting">
            {{ deleteSubmitting ? 'Deleting...' : 'Delete' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
