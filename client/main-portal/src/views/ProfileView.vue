<template>
  <div class="min-h-screen bg-surface">
    <!-- Loading skeleton -->
    <div v-if="loading" class="flex items-center justify-center min-h-screen">
      <div class="flex flex-col items-center gap-3">
        <div class="w-10 h-10 border-3 border-primary/20 border-t-primary rounded-full animate-spin"></div>
        <p class="text-sm text-muted">Loading profile…</p>
      </div>
    </div>

    <!-- Error state -->
    <div v-else-if="fetchError" class="flex items-center justify-center min-h-screen p-6">
      <div class="text-center max-w-sm">
        <div class="w-14 h-14 bg-red-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
          <AlertCircleIcon class="w-7 h-7 text-danger" />
        </div>
        <h2 class="text-base font-semibold text-ink mb-1">Couldn't load profile</h2>
        <p class="text-sm text-muted mb-4">{{ fetchError }}</p>
        <button @click="loadProfile" class="px-5 py-2.5 bg-primary text-white text-sm font-semibold rounded-xl hover:bg-action transition-colors">
          Try again
        </button>
      </div>
    </div>

    <!-- Profile content -->
    <div v-else-if="patient" class="max-w-4xl mx-auto px-4 sm:px-6 py-8 space-y-6">

      <!-- Header card -->
      <div class="bg-card rounded-2xl border border-border overflow-hidden">
        <!-- Banner -->
        <div class="h-28 sm:h-36 relative" style="background: linear-gradient(135deg, #004D51 0%, #00759a 100%);">
          <div class="absolute inset-0 opacity-10" style="background-image: radial-gradient(circle at 20% 80%, white 1px, transparent 1px), radial-gradient(circle at 80% 20%, white 1px, transparent 1px); background-size: 40px 40px;"></div>
        </div>

        <div class="px-5 sm:px-8 pb-6">
          <!-- Avatar row -->
          <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 -mt-10 sm:-mt-12 mb-5">
            <div class="relative w-fit">
              <div class="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl border-4 border-card bg-primary/10 flex items-center justify-center overflow-hidden shadow-sm">
                <img v-if="patient.avatarUrl" :src="patient.avatarUrl" :alt="fullName" class="w-full h-full object-cover" />
                <template v-else>
                  <!-- Fallback initials avatar -->
                  <span class="text-2xl font-bold text-primary select-none">{{ initials }}</span>
                </template>
              </div>
              <button
                v-if="isOwnProfile"
                @click="triggerAvatarUpload"
                class="absolute -bottom-1 -right-1 w-7 h-7 bg-primary rounded-lg flex items-center justify-center shadow-md hover:bg-action transition-colors"
                title="Change photo"
              >
                <CameraIcon class="w-3.5 h-3.5 text-white" />
              </button>
              <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="onAvatarChange" />
            </div>

            <div v-if="isOwnProfile" class="flex gap-2">
              <button
                @click="startEdit"
                class="flex items-center gap-1.5 px-4 py-2 text-sm font-semibold rounded-xl border border-border text-muted bg-surface hover:border-primary/40 hover:text-primary transition-colors"
              >
                <PencilIcon class="w-3.5 h-3.5" />
                Edit profile
              </button>
            </div>
          </div>

          <!-- Name & ID -->
          <div>
            <div class="flex flex-wrap items-center gap-2 mb-1">
              <h1 class="text-xl sm:text-2xl font-semibold text-ink">{{ fullName }}</h1>
              <span class="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-primary/10 text-primary">
                {{ patient.patientId }}
              </span>
            </div>
            <p class="text-sm text-muted">{{ patient.email }}</p>
          </div>

          <!-- Quick stats row -->
          <div class="flex flex-wrap gap-10 mt-5 pt-5 border-t border-border">
            <div v-for="stat in quickStats" :key="stat.label" class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-lg bg-surface flex items-center justify-center">
                <component :is="stat.icon" class="w-3.5 h-3.5 text-muted" />
              </div>
              <div>
                <p class="text-xs text-muted leading-none mb-0.5">{{ stat.label }}</p>
                <p class="text-sm font-medium text-ink leading-none">{{ stat.value || '—' }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Details grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">

        <!-- Personal information -->
        <div class="bg-card rounded-2xl border border-border p-5 sm:p-6">
          <div class="flex items-center justify-between mb-5">
            <h2 class="text-sm font-semibold text-ink flex items-center gap-2">
              <UserIcon class="w-4 h-4 text-primary" />
              Personal Information
            </h2>
          </div>
          <div class="space-y-4">
            <InfoRow label="Full Name" :value="fullName" />
            <InfoRow label="Email" :value="patient.email" />
            <InfoRow label="Phone" :value="patient.phone" />
            <InfoRow label="Date of Birth" :value="formattedDob" />
            <InfoRow label="Gender" :value="capitalize(patient.gender)" />
            <InfoRow label="Blood Group" :value="patient.bloodGroup" />
          </div>
        </div>

        <!-- Address -->
        <div class="bg-card rounded-2xl border border-border p-5 sm:p-6">
          <div class="flex items-center justify-between mb-5">
            <h2 class="text-sm font-semibold text-ink flex items-center gap-2">
              <MapPinIcon class="w-4 h-4 text-primary" />
              Address
            </h2>
          </div>
          <div v-if="hasAddress" class="space-y-4">
            <InfoRow label="Street" :value="patient.address?.street" />
            <InfoRow label="City" :value="patient.address?.city" />
            <InfoRow label="Country" :value="patient.address?.country" />
          </div>
          <div v-else class="flex flex-col items-center justify-center py-8 text-center">
            <MapPinIcon class="w-8 h-8 text-muted/30 mb-2" />
            <p class="text-sm text-muted">No address on file</p>
            <button v-if="isOwnProfile" @click="startEdit" class="mt-3 text-xs text-primary hover:underline">Add address</button>
          </div>
        </div>

        <!-- Account info -->
        <div class="bg-card rounded-2xl border border-border p-5 sm:p-6 md:col-span-2">
          <h2 class="text-sm font-semibold text-ink flex items-center gap-2 mb-5">
            <ShieldIcon class="w-4 h-4 text-primary" />
            Account
          </h2>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <InfoRow label="Patient ID" :value="patient.patientId" mono />
            <InfoRow label="Member since" :value="formattedCreatedAt" />
            <InfoRow label="Last updated" :value="formattedUpdatedAt" />
          </div>
        </div>
      </div>

      <!-- Danger zone -->
      <div v-if="isOwnProfile" class="bg-card rounded-2xl border border-danger/20 p-5 sm:p-6">
        <h2 class="text-sm font-semibold text-danger flex items-center gap-2 mb-3">
          <AlertTriangleIcon class="w-4 h-4" />
          Danger Zone
        </h2>
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <p class="text-sm font-medium text-ink">Deactivate account</p>
            <p class="text-xs text-muted mt-0.5">Your account will be disabled. You can contact support to reactivate it.</p>
          </div>
          <button
            @click="showDeactivateModal = true"
            class="shrink-0 px-4 py-2 text-sm font-semibold rounded-xl border border-danger/30 text-danger hover:bg-red-50 transition-colors"
          >
            Deactivate
          </button>
        </div>
      </div>
    </div>

    <!-- ── Edit profile modal ── -->
    <Teleport to="body">
      <transition name="modal">
        <div v-if="showEditModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="showEditModal = false"></div>
          <div class="relative bg-card rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto z-10">
            <div class="sticky top-0 bg-card border-b border-border px-6 py-4 flex items-center justify-between rounded-t-2xl">
              <h3 class="font-semibold text-ink">Edit Profile</h3>
              <button @click="showEditModal = false" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-surface transition-colors text-muted">
                <XIcon class="w-4 h-4" />
              </button>
            </div>

            <div class="p-6 space-y-4">
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">First Name</label>
                  <input v-model="editForm.firstName" type="text" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
                </div>
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Last Name</label>
                  <input v-model="editForm.lastName" type="text" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
                </div>
              </div>

              <div>
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Phone</label>
                <input v-model="editForm.phone" type="tel" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
              </div>

              <div>
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Date of Birth</label>
                <input v-model="editForm.dateOfBirth" type="date" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
              </div>

              <div>
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Gender</label>
                <div class="grid grid-cols-3 gap-2">
                  <button
                    v-for="g in genderOptions" :key="g.value"
                    type="button"
                    @click="editForm.gender = g.value"
                    class="py-2.5 rounded-xl border text-sm font-medium transition-all"
                    :class="editForm.gender === g.value ? 'border-primary bg-primary/5 text-primary' : 'border-border text-muted bg-surface hover:border-primary/30'"
                  >
                    {{ g.label }}
                  </button>
                </div>
              </div>

              <div>
                <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Blood Group</label>
                <select v-model="editForm.bloodGroup" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors">
                  <option value="">Not specified</option>
                  <option v-for="bg in bloodGroups" :key="bg" :value="bg">{{ bg }}</option>
                </select>
              </div>

              <div class="pt-1 border-t border-border">
                <p class="text-xs font-semibold uppercase tracking-wide text-muted mb-3">Address</p>
                <div class="space-y-3">
                  <input v-model="editForm.address.street" type="text" placeholder="Street" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
                  <div class="grid grid-cols-2 gap-3">
                    <input v-model="editForm.address.city" type="text" placeholder="City" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
                    <input v-model="editForm.address.country" type="text" placeholder="Country" class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors" />
                  </div>
                </div>
              </div>

              <div v-if="editError" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-red-50 border border-red-100">
                <AlertCircleIcon class="w-4 h-4 text-danger shrink-0 mt-0.5" />
                <p class="text-xs text-danger">{{ editError }}</p>
              </div>

              <div v-if="editSuccess" class="flex items-start gap-2.5 px-4 py-3 rounded-xl bg-green-50 border border-green-100">
                <CheckCircleIcon class="w-4 h-4 text-success shrink-0 mt-0.5" />
                <p class="text-xs text-success">Profile updated successfully.</p>
              </div>

              <div class="flex gap-3 pt-1">
                <button @click="showEditModal = false" class="flex-1 py-3 text-sm font-semibold rounded-xl border border-border text-muted hover:bg-surface transition-colors">
                  Cancel
                </button>
                <button
                  @click="saveEdit"
                  :disabled="editLoading"
                  class="flex-[2] py-3 text-white text-sm font-semibold rounded-xl transition-all disabled:opacity-60 bg-primary hover:bg-action"
                >
                  <span v-if="editLoading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
                  {{ editLoading ? 'Saving…' : 'Save changes' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- ── Deactivate modal ── -->
    <Teleport to="body">
      <transition name="modal">
        <div v-if="showDeactivateModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="showDeactivateModal = false"></div>
          <div class="relative bg-card rounded-2xl shadow-2xl w-full max-w-sm z-10 p-6">
            <div class="flex items-start gap-3 mb-4">
              <div class="w-10 h-10 bg-red-50 rounded-xl flex items-center justify-center shrink-0">
                <AlertTriangleIcon class="w-5 h-5 text-danger" />
              </div>
              <div>
                <h3 class="font-semibold text-ink">Deactivate account?</h3>
                <p class="text-xs text-muted mt-1">Your account will be disabled. Contact support to reactivate it.</p>
              </div>
            </div>

            <div>
              <label class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5">Reason (optional)</label>
              <textarea
                v-model="deactivateReason"
                rows="2"
                placeholder="Why are you leaving?"
                class="w-full px-3.5 py-2.5 rounded-xl border border-border text-sm text-ink bg-surface placeholder-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-colors resize-none"
              ></textarea>
            </div>

            <div v-if="deactivateError" class="mt-3 flex items-start gap-2 px-3 py-2.5 rounded-xl bg-red-50 border border-red-100">
              <AlertCircleIcon class="w-3.5 h-3.5 text-danger shrink-0 mt-0.5" />
              <p class="text-xs text-danger">{{ deactivateError }}</p>
            </div>

            <div class="flex gap-2 mt-5">
              <button @click="showDeactivateModal = false" class="flex-1 py-2.5 text-sm font-semibold rounded-xl border border-border text-muted hover:bg-surface transition-colors">
                Cancel
              </button>
              <button
                @click="deactivateAccount"
                :disabled="deactivateLoading"
                class="flex-1 py-2.5 text-sm font-semibold rounded-xl bg-danger text-white hover:bg-red-600 transition-colors disabled:opacity-60"
              >
                <span v-if="deactivateLoading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-1.5 align-middle"></span>
                {{ deactivateLoading ? 'Deactivating…' : 'Confirm' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'
import authService from '@/services/authService'
import patientService from '@/services/patientService'
import {
  User as UserIcon,
  Camera as CameraIcon,
  Pencil as PencilIcon,
  MapPin as MapPinIcon,
  Phone as PhoneIcon,
  Calendar as CalendarIcon,
  Droplets as DropletsIcon,
  Shield as ShieldIcon,
  AlertCircle as AlertCircleIcon,
  AlertTriangle as AlertTriangleIcon,
  CheckCircle as CheckCircleIcon,
  X as XIcon,
} from 'lucide-vue-next'

// ── Subcomponent: InfoRow ──
const InfoRow = {
  props: { label: String, value: String, mono: Boolean },
  setup(props) {
    return () => h('div', [
      h('p', { class: 'text-xs text-muted mb-0.5' }, props.label),
      h(
        'p',
        { class: ['text-sm font-medium text-ink break-all', props.mono ? 'font-mono text-xs' : ''] },
        props.value || '—',
      ),
    ])
  },
}

const router = useRouter()
const authStore = useAuth()
const notify = useNotificationStore()

// ── State ──
const patient = ref(null)
const loading = ref(true)
const fetchError = ref(null)
const avatarInput = ref(null)

const showEditModal = ref(false)
const editLoading = ref(false)
const editError = ref(null)

const showDeactivateModal = ref(false)
const deactivateLoading = ref(false)
const deactivateError = ref(null)
const deactivateReason = ref('')

const editForm = reactive({
  firstName: '',
  lastName: '',
  phone: '',
  dateOfBirth: '',
  gender: '',
  bloodGroup: '',
  address: { street: '', city: '', country: '' },
})

// ── Static data ──
const genderOptions = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
]
const bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']

// ── Computed ──
const isOwnProfile = computed(() => true) // adapt to router param vs stored authId

const fullName = computed(() => {
  if (!patient.value) return ''
  return [patient.value.firstName, patient.value.lastName].filter(Boolean).join(' ') || patient.value.email
})

const initials = computed(() => {
  const f = patient.value?.firstName?.[0] || ''
  const l = patient.value?.lastName?.[0] || ''
  return (f + l).toUpperCase() || '?'
})

const hasAddress = computed(() => {
  const a = patient.value?.address
  return a && (a.street || a.city || a.country)
})

const formattedDob = computed(() => {
  if (!patient.value?.dateOfBirth) return null
  return new Date(patient.value.dateOfBirth).toLocaleDateString('en-GB', {
    day: 'numeric', month: 'long', year: 'numeric',
  })
})

const formattedCreatedAt = computed(() => {
  if (!patient.value?.createdAt) return null
  return new Date(patient.value.createdAt).toLocaleDateString('en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
})

const formattedUpdatedAt = computed(() => {
  if (!patient.value?.updatedAt) return null
  return new Date(patient.value.updatedAt).toLocaleDateString('en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
})

const quickStats = computed(() => [
  { label: 'Phone', value: patient.value?.phone, icon: PhoneIcon },
  { label: 'Date of Birth', value: formattedDob.value, icon: CalendarIcon },
  { label: 'Blood Group', value: patient.value?.bloodGroup, icon: DropletsIcon },
])

// ── Helpers ──
function capitalize(s) {
  if (!s) return ''
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}

function getToken() {
  return authStore.token || localStorage.getItem('aferent.auth.accessToken') || localStorage.getItem('accessToken')
}

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${getToken()}`,
  }
}

// ── Load profile ──
async function loadProfile() {
  loading.value = true
  fetchError.value = null
  try {
    const session = await authStore.fetchMe()
    if (!session?.authId) {
      router.push('/login')
      return
    }

    const token = getToken()
    const me = await patientService.getCurrentProfile({ token })
    const patientId = me?.patientId
    if (!patientId) {
      fetchError.value = 'Patient ID not available. Please try again.'
      return
    }

    localStorage.setItem('patientId', patientId)

    patient.value = await patientService.getProfile({ patientId, token })
  } catch (e) {
    if (e?.status === 401) {
      router.push('/login')
      return
    }
    fetchError.value = 'Network error. Please check your connection.'
  } finally {
    loading.value = false
  }
}

// ── Edit ──
function startEdit() {
  if (!patient.value) return
  Object.assign(editForm, {
    firstName: patient.value.firstName || '',
    lastName: patient.value.lastName || '',
    phone: patient.value.phone || '',
    dateOfBirth: patient.value.dateOfBirth || '',
    gender: patient.value.gender || '',
    bloodGroup: patient.value.bloodGroup || '',
    address: {
      street: patient.value.address?.street || '',
      city: patient.value.address?.city || '',
      country: patient.value.address?.country || '',
    },
  })
  editError.value = null
  showEditModal.value = true
}

async function saveEdit() {
  editError.value = null
  editLoading.value = true
  try {
    const session = await authStore.fetchMe()
    if (!session?.authId) {
      router.push('/login')
      return
    }

    const payload = {
      firstName: editForm.firstName,
      lastName: editForm.lastName,
      phone: editForm.phone,
      dateOfBirth: editForm.dateOfBirth,
      gender: editForm.gender || undefined,
      bloodGroup: editForm.bloodGroup || undefined,
      address: (editForm.address.street || editForm.address.city || editForm.address.country)
        ? { ...editForm.address }
        : undefined,
    }

    const patientId = patient.value?.patientId || localStorage.getItem('patientId')
    if (!patientId) {
      editError.value = 'Patient ID not found. Reload your profile and try again.'
      return
    }

    const token = getToken()
    patient.value = await patientService.updateProfile({ patientId, token, payload })
    showEditModal.value = false
    notify.push('Profile updated successfully.', 'success')
  } catch (e) {
    if (e?.status === 401) {
      router.push('/login')
      return
    }
    if (e?.message) {
      editError.value = e.message
      notify.push(e.message, 'error')
      return
    }
    editError.value = 'Network error. Please try again.'
    notify.push(editError.value, 'error')
  } finally {
    editLoading.value = false
  }
}

// ── Deactivation ──
async function deactivateAccount() {
  deactivateError.value = null
  deactivateLoading.value = true
  try {
    const reason = deactivateReason.value.trim() || 'No longer using service'
    await authService.deactivateSelf({ reason, token: getToken() })

    authStore.saveSession('', null)
    localStorage.removeItem('patientId')
    notify.push('Account deactivated successfully.', 'info')
    router.push('/login')
  } catch (e) {
    if (e?.message) {
      deactivateError.value = e.message
      notify.push(e.message, 'error')
      return
    }
    deactivateError.value = 'Network error. Please try again.'
    notify.push(deactivateError.value, 'error')
  } finally {
    deactivateLoading.value = false
  }
}

// ── Avatar (upload logic deferred) ──
function triggerAvatarUpload() {
  avatarInput.value?.click()
}

function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  // TODO: upload logic will be implemented later
  console.log('Avatar file selected:', file.name)
}

// ── Init ──
onMounted(loadProfile)
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease;
}
.modal-enter-from .relative {
  transform: translateY(10px) scale(0.98);
}
.modal-leave-to .relative {
  transform: translateY(6px) scale(0.98);
}
</style>