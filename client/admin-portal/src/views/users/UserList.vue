<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-semibold mb-2" style="color: var(--text-primary)">User Management</h1>
        <p style="color: var(--text-secondary)">Manage all registered users</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label class="block text-sm font-medium mb-2" style="color: var(--text-primary)">Search</label>
          <input
            v-model="filters.search"
            type="text"
            placeholder="Search users..."
            class="input-field"
          />
        </div>
        <div>
          <label class="block text-sm font-medium mb-2" style="color: var(--text-primary)">Status</label>
          <select v-model="filters.status" class="input-field">
            <option value="">All Status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
            <option value="suspended">Suspended</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium mb-2" style="color: var(--text-primary)">Role</label>
          <select v-model="filters.role" class="input-field">
            <option value="">All Roles</option>
            <option value="patient">Patient</option>
            <option value="doctor">Doctor</option>
            <option value="admin">Admin</option>
          </select>
        </div>
        <div class="flex items-end">
          <button @click="resetFilters" class="btn-secondary w-full">Reset Filters</button>
        </div>
      </div>
    </div>

    <!-- Users Table -->
    <div class="card">
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in filteredUsers" :key="user.id">
              <td>
                <div class="flex items-center space-x-3">
                  <div class="avatar">{{ user.name.charAt(0) }}</div>
                  <div>
                    <p class="font-medium" style="color: var(--text-primary)">{{ user.name }}</p>
                  </div>
                </div>
              </td>
              <td style="color: var(--text-secondary)">{{ user.email }}</td>
              <td style="color: var(--text-secondary)">{{ user.phone }}</td>
              <td>
                <span class="badge" :class="`badge-${user.role}`">{{ user.role }}</span>
              </td>
              <td>
                <span class="badge" :class="`badge-${user.status}`">{{ user.status }}</span>
              </td>
              <td style="color: var(--text-secondary)">{{ formatDate(user.joinedDate) }}</td>
              <td>
                <div class="flex items-center space-x-2">
                  <button @click="viewUser(user)" class="icon-btn" title="View">
                    <Eye :size="18" />
                  </button>
                  <button @click="editUser(user)" class="icon-btn" title="Edit">
                    <Edit :size="18" />
                  </button>
                  <button @click="toggleStatus(user)" class="icon-btn" title="Toggle Status">
                    <component :is="user.status === 'active' ? Lock : Unlock" :size="18" />
                  </button>
                  <button @click="deleteUser(user)" class="icon-btn text-red-600" title="Delete">
                    <Trash2 :size="18" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex justify-between items-center mt-4 pt-4 border-t" style="border-color: var(--border-color)">
        <p class="text-sm" style="color: var(--text-secondary)">
          Showing {{ (currentPage - 1) * pageSize + 1 }} to {{ Math.min(currentPage * pageSize, filteredUsers.length) }} of {{ filteredUsers.length }} users
        </p>
        <div class="flex space-x-2">
          <button @click="currentPage--" :disabled="currentPage === 1" class="btn-secondary">Previous</button>
          <button @click="currentPage++" :disabled="currentPage * pageSize >= filteredUsers.length" class="btn-secondary">Next</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Eye, Edit, Lock, Unlock, Trash2 } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { showToast } = useToast()

const filters = ref({
  search: '',
  status: '',
  role: ''
})

const currentPage = ref(1)
const pageSize = ref(10)

const users = ref([
  {
    id: 1,
    name: 'John Doe',
    email: 'john@example.com',
    phone: '+1234567890',
    role: 'patient',
    status: 'active',
    joinedDate: '2024-01-15'
  },
  {
    id: 2,
    name: 'Dr. Sarah Smith',
    email: 'sarah@example.com',
    phone: '+1234567891',
    role: 'doctor',
    status: 'active',
    joinedDate: '2024-02-20'
  },
  {
    id: 3,
    name: 'Mike Johnson',
    email: 'mike@example.com',
    phone: '+1234567892',
    role: 'patient',
    status: 'inactive',
    joinedDate: '2024-03-10'
  },
  {
    id: 4,
    name: 'Admin User',
    email: 'admin@example.com',
    phone: '+1234567893',
    role: 'admin',
    status: 'active',
    joinedDate: '2023-12-01'
  },
  {
    id: 5,
    name: 'Emily Davis',
    email: 'emily@example.com',
    phone: '+1234567894',
    role: 'patient',
    status: 'suspended',
    joinedDate: '2024-04-05'
  }
])

const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchesSearch = !filters.value.search || 
      user.name.toLowerCase().includes(filters.value.search.toLowerCase()) ||
      user.email.toLowerCase().includes(filters.value.search.toLowerCase())
    
    const matchesStatus = !filters.value.status || user.status === filters.value.status
    const matchesRole = !filters.value.role || user.role === filters.value.role
    
    return matchesSearch && matchesStatus && matchesRole
  })
})

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const resetFilters = () => {
  filters.value = { search: '', status: '', role: '' }
}

const viewUser = (user: any) => {
  showToast(`Viewing user: ${user.name}`, 'info')
}

const editUser = (user: any) => {
  showToast(`Editing user: ${user.name}`, 'info')
}

const toggleStatus = (user: any) => {
  user.status = user.status === 'active' ? 'inactive' : 'active'
  showToast(`User status updated to ${user.status}`, 'success')
}

const deleteUser = (user: any) => {
  if (confirm(`Are you sure you want to delete ${user.name}?`)) {
    users.value = users.value.filter(u => u.id !== user.id)
    showToast('User deleted successfully', 'success')
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
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.input-field:focus {
  outline: none;
  border-color: var(--primary);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 0.75rem;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 2px solid var(--border-color);
}

.data-table td {
  padding: 0.75rem;
  border-bottom: 1px solid var(--border-color);
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

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: capitalize;
}

.badge-patient { background: #dbeafe; color: #1e40af; }
.badge-doctor { background: #dcfce7; color: #166534; }
.badge-admin { background: #f3e8ff; color: #6b21a8; }
.badge-active { background: #dcfce7; color: #166534; }
.badge-inactive { background: #fee2e2; color: #991b1b; }
.badge-suspended { background: #fef3c7; color: #92400e; }

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

.btn-secondary {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
  transition: all 0.2s;
}

.btn-secondary:hover:not(:disabled) {
  background: var(--bg-hover);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
