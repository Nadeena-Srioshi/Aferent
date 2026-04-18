<template>
  <section class="bg-white border-b border-border" aria-labelledby="search-heading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

      <div class="text-center mb-8">
        <h2 id="search-heading" class="text-2xl sm:text-3xl font-bold text-ink mb-2">
          Find the care you need
        </h2>
        <p class="text-muted">Search by doctor name, specialization, hospital, or date</p>
      </div>

      <!-- Search form -->
      <div class="max-w-6xl mx-auto">
        <form class="bg-surface border border-border rounded-2xl p-3 shadow-sm" @submit.prevent="handleSearch">
          <div class="flex flex-col xl:flex-row gap-3 items-stretch xl:items-center">
            <!-- Specialization -->
            <div class="relative flex-[1.1] min-w-0">
              <label for="specialty-select" class="sr-only">Specialization</label>
              <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none" aria-hidden="true">
                <Stethoscope class="w-5 h-5 text-primary" />
              </div>
              <select
                id="specialty-select"
                v-model="selectedSpecialtyId"
                required
                class="w-full pl-10 pr-4 py-3.5 bg-white border border-border rounded-xl text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors appearance-none cursor-pointer"
              >
                <option value="">Specialization *</option>
                <option v-for="s in specializationOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select>
            </div>

            <!-- Doctor name -->
            <div class="relative flex-[1.2] min-w-0">
              <label for="doctor-name" class="sr-only">Doctor name</label>
              <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none" aria-hidden="true">
                <UserRound class="w-5 h-5 text-muted" />
              </div>
              <input
                id="doctor-name"
                v-model="doctorName"
                type="text"
                placeholder="Doctor name"
                class="w-full pl-10 pr-4 py-3.5 bg-white border border-border rounded-xl text-sm text-ink placeholder-muted focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors"
              />
            </div>

            <!-- Hospital dropdown -->
            <div class="relative flex-[1.2] min-w-0">
              <label for="hospital" class="sr-only">Hospital</label>
              <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none" aria-hidden="true">
                <Building2 class="w-5 h-5 text-primary" />
              </div>
              <select
                id="hospital"
                v-model="hospital"
                class="w-full pl-10 pr-4 py-3.5 bg-white border border-border rounded-xl text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors appearance-none cursor-pointer"
              >
                <option value="">Hospital</option>
                <option
                  v-for="option in hospitals"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </div>

            <!-- Date -->
            <div class="relative flex-[1.1] min-w-[200px]">
              <label for="appointment-date" class="sr-only">Date</label>
              <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none" aria-hidden="true">
                <CalendarDays class="w-5 h-5 text-muted" />
              </div>
              <input
                id="appointment-date"
                v-model="appointmentDate"
                type="date"
                class="w-full pl-10 pr-4 py-3.5 bg-white border border-border rounded-xl text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors"
              />
            </div>

            <!-- Search button -->
            <button
              type="submit"
              class="flex items-center justify-center gap-2 px-6 py-3.5 bg-primary text-white font-semibold rounded-xl hover:bg-action transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 flex-shrink-0 whitespace-nowrap"
            >
              <Search class="w-5 h-5" aria-hidden="true" />
              <span>Search Doctors</span>
            </button>
          </div>

          <p v-if="error" class="mt-3 text-sm text-danger bg-danger/10 px-4 py-2 rounded-xl">{{ error }}</p>
        </form>

        <!-- Quick filters -->
        <div class="mt-4 flex flex-wrap gap-2 justify-center" role="group" aria-label="Quick specialization filters">
          <button
            v-for="option in quickFilters"
            :key="option.id"
            class="px-4 py-1.5 rounded-full text-sm font-medium border border-border text-muted hover:border-primary hover:text-primary hover:bg-primary/5 transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            @click="applyQuickFilter(option)"
          >
            {{ option.name }}
          </button>
        </div>
      </div>

    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Building2, CalendarDays, Search, Stethoscope, UserRound } from 'lucide-vue-next'
import { getHospitals, getSpecializations } from '../../services/doctorService'

const route = useRoute()
const router = useRouter()
const doctorName = ref('')
const selectedSpecialtyId = ref('')
const hospital = ref('')
const appointmentDate = ref('')
const error = ref('')
const specializationOptions = ref([])
const hospitals = ref([])
const legacySpecialtyName = ref('')

const quickFilters = computed(() => specializationOptions.value.slice(0, 6))

const toSpecializationOptions = (items) => {
  if (!Array.isArray(items)) return []

  return items
    .map((item) => {
      if (typeof item === 'string') {
        const value = item.trim()
        if (!value) return null
        return { id: value, name: value }
      }

      if (item && typeof item === 'object') {
        const id = item.id ?? null
        const name = item.name ?? null
        if (!id || !name) return null
        return { id: String(id), name: String(name) }
      }

      return null
    })
    .filter(Boolean)
}

const toHospitalOptions = (items) => {
  if (!Array.isArray(items)) return []

  return items
    .map((item) => {
      if (typeof item === 'string') {
        return { value: item, label: item, name: item }
      }

      if (item && typeof item === 'object') {
        const id = item.id ?? item.name ?? null
        const name = item.name ?? null
        if (!id || !name) return null

        const city = item.city ? String(item.city).trim() : ''
        return {
          value: id,
          label: city ? `${name} (${city})` : name,
          name,
        }
      }

      return null
    })
    .filter(Boolean)
}

const syncFromRouteQuery = () => {
  doctorName.value = typeof route.query.name === 'string' ? route.query.name.trim() : ''
  selectedSpecialtyId.value = typeof route.query.specializationId === 'string' ? route.query.specializationId.trim() : ''
  legacySpecialtyName.value = !selectedSpecialtyId.value && typeof route.query.specialty === 'string'
    ? route.query.specialty.trim()
    : ''
  hospital.value = typeof route.query.hospital === 'string' ? route.query.hospital.trim() : ''
  appointmentDate.value = typeof route.query.date === 'string' ? route.query.date.trim() : ''
}

const normalizeSpecializationSelection = () => {
  if (!specializationOptions.value.length) return

  const exactId = specializationOptions.value.find((option) => option.id === selectedSpecialtyId.value)
  if (exactId) {
    legacySpecialtyName.value = ''
    return
  }

  if (!legacySpecialtyName.value) return

  const byName = specializationOptions.value.find((option) =>
    option.name.toLowerCase() === legacySpecialtyName.value.toLowerCase()
  )

  if (byName) {
    selectedSpecialtyId.value = byName.id
    legacySpecialtyName.value = ''
  }
}

const normalizeHospitalSelection = () => {
  if (!hospital.value || !hospitals.value.length) return

  const exactId = hospitals.value.find((option) => option.value === hospital.value)
  if (exactId) return

  const byName = hospitals.value.find((option) => {
    const label = option.label?.toLowerCase?.() || ''
    const name = option.name?.toLowerCase?.() || ''
    const current = hospital.value.toLowerCase()
    return name === current || label === current || label.startsWith(`${current} (`)
  })

  if (byName) {
    hospital.value = byName.value
  }
}

const loadReferenceData = async () => {
  try {
    const [specializationsResponse, hospitalsResponse] = await Promise.all([
      getSpecializations(),
      getHospitals(),
    ])

    specializationOptions.value = toSpecializationOptions(specializationsResponse)
    hospitals.value = toHospitalOptions(hospitalsResponse)
    normalizeSpecializationSelection()
    normalizeHospitalSelection()
  } catch (e) {
    error.value = e?.message || 'Failed to load hospitals and specializations.'
  }
}

const handleSearch = () => {
  if (!selectedSpecialtyId.value) {
    error.value = 'Please select a specialization before searching.'
    return
  }

  error.value = ''
  router.push({
    path: '/find-doctor',
    query: {
      name: doctorName.value || undefined,
      specializationId: selectedSpecialtyId.value || undefined,
      hospital: hospital.value || undefined,
      date: appointmentDate.value || undefined,
    },
  })
}

const applyQuickFilter = (option) => {
  selectedSpecialtyId.value = option.id
  handleSearch()
}

onMounted(() => {
  syncFromRouteQuery()
  loadReferenceData()
})

watch(
  () => route.query,
  () => {
    syncFromRouteQuery()
    normalizeSpecializationSelection()
    normalizeHospitalSelection()
  }
)
</script>