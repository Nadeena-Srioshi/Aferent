<template>
  <main id="main-content" class="bg-surface min-h-screen">

    <SearchSection />

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="grid grid-cols-1 xl:grid-cols-[320px_minmax(0,1fr)] gap-6">
        <!-- Active search summary -->
        <aside class="bg-card border border-border rounded-2xl p-5 h-fit sticky top-24">
          <h2 class="font-bold text-ink mb-4">Search filters</h2>

          <div class="space-y-4 text-sm">
            <div>
              <p class="text-muted mb-1">Specialization</p>
              <p class="font-semibold text-ink">{{ selectedSpecializationLabel || '—' }}</p>
            </div>
            <div>
              <p class="text-muted mb-1">Doctor name</p>
              <p class="font-semibold text-ink">{{ search.name || 'Any' }}</p>
            </div>
            <div>
              <p class="text-muted mb-1">Hospital</p>
              <p class="font-semibold text-ink">{{ selectedHospitalLabel || 'Any' }}</p>
            </div>
            <div>
              <p class="text-muted mb-1">Date</p>
              <p class="font-semibold text-ink">{{ search.date || 'Any' }}</p>
            </div>
          </div>

          <p v-if="!hasSpecializationFilter" class="mt-4 text-sm text-warning bg-warning/10 px-4 py-3 rounded-xl">
            Pick a specialization from the search box.
          </p>
        </aside>

        <!-- Results -->
        <div>
          <div class="flex items-center justify-between gap-3 mb-5">
            <p class="text-sm text-muted">
              <span class="font-semibold text-ink">{{ doctors.length }}</span>
              doctor{{ doctors.length === 1 ? '' : 's' }} found
            </p>
          </div>

          <div v-if="!hasSpecializationFilter" class="bg-card border border-border rounded-2xl p-8 text-center">
            <h2 class="text-xl font-bold text-ink mb-2">Choose a specialization to begin</h2>
            <p class="text-muted">Your homepage search requires a specialization. Once selected, matching doctors will appear here.</p>
          </div>

          <div v-else-if="loading" class="bg-card border border-border rounded-2xl p-8 text-center">
            <h2 class="text-xl font-bold text-ink mb-2">Searching doctors...</h2>
            <p class="text-muted">Fetching latest availability from doctor service.</p>
          </div>

          <div v-else-if="error" class="bg-card border border-border rounded-2xl p-8 text-center">
            <h2 class="text-xl font-bold text-ink mb-2">Could not load doctors</h2>
            <p class="text-muted mb-5">{{ error }}</p>
            <button
              type="button"
              class="inline-flex items-center justify-center px-5 py-3 rounded-xl bg-primary text-white font-semibold hover:bg-action transition-colors"
              @click="loadDoctors"
            >
              Try again
            </button>
          </div>

          <div v-else-if="doctors.length" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            <Doctorcard
              v-for="doctor in doctors"
              :key="doctor.id"
              :doctor="doctor"
            >
              <template #action>
                <button
                  type="button"
                  class="block w-full text-center py-2.5 bg-primary text-white text-sm font-semibold rounded-xl hover:bg-action transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
                  @click="selectDoctor(doctor)"
                >
                  Select Doctor
                </button>
              </template>
            </Doctorcard>
          </div>

          <div v-else class="bg-card border border-border rounded-2xl p-8 text-center">
            <h2 class="text-xl font-bold text-ink mb-2">No matches found</h2>
            <p class="text-muted mb-5">Try widening the name, hospital, or date filters while keeping the specialization selected.</p>
            <RouterLink
              to="/"
              class="inline-flex items-center justify-center px-5 py-3 rounded-xl bg-primary text-white font-semibold hover:bg-action transition-colors"
            >
              Back to search
            </RouterLink>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import Doctorcard from '@/components/shared/Doctorcard.vue'
import SearchSection from '@/components/sections/SearchSection.vue'
import { getHospitals, getSpecializations, searchDoctors } from '@/services/doctorService'

const route = useRoute()
const router = useRouter()
const doctors = ref([])
const hospitals = ref([])
const specializations = ref([])
const loading = ref(false)
const error = ref('')
let searchRequestId = 0

const search = computed(() => ({
  name: typeof route.query.name === 'string' ? route.query.name.trim() : '',
  specializationId: typeof route.query.specializationId === 'string' ? route.query.specializationId.trim() : '',
  specialty: typeof route.query.specialty === 'string' ? route.query.specialty.trim() : '',
  hospital: typeof route.query.hospital === 'string' ? route.query.hospital.trim() : '',
  date: typeof route.query.date === 'string' ? route.query.date.trim() : '',
}))

const hasSpecializationFilter = computed(() => Boolean(search.value.specializationId || search.value.specialty))

const selectedSpecializationLabel = computed(() => {
  if (search.value.specializationId) {
    const byId = specializations.value.find((s) => s?.id === search.value.specializationId)
    if (byId?.name) return byId.name
    return search.value.specializationId
  }

  if (search.value.specialty) {
    const byName = specializations.value.find((s) =>
      typeof s?.name === 'string' && s.name.toLowerCase() === search.value.specialty.toLowerCase()
    )
    return byName?.name || search.value.specialty
  }

  return ''
})

const selectedHospitalLabel = computed(() => {
  if (!search.value.hospital) return ''

  const byId = hospitals.value.find((h) => h?.id === search.value.hospital)
  if (byId?.name) return byId.name

  const byName = hospitals.value.find((h) =>
    typeof h?.name === 'string' && h.name.toLowerCase() === search.value.hospital.toLowerCase()
  )

  return byName?.name || search.value.hospital
})

function selectDoctor(doctor) {
  if (!doctor?.id) return

  const query = {}
  if (search.value.date) {
    query.date = search.value.date
  }

  router.push({
    name: 'doctor-booking',
    params: { doctorId: doctor.id },
    query,
  })
}

function mapDoctorToCard(doctor) {
  const fullName = [doctor?.firstName, doctor?.lastName].filter(Boolean).join(' ').trim()
  const years = Number.isFinite(doctor?.yearsOfExperience)
    ? `${doctor.yearsOfExperience} years experience`
    : 'Experience not specified'

  const specializationLabel = (() => {
    if (!doctor?.specialization) return 'General'

    const byId = specializations.value.find((s) => s?.id === doctor.specialization)
    if (byId?.name) return byId.name

    const byName = specializations.value.find((s) =>
      typeof s?.name === 'string' && s.name.toLowerCase() === String(doctor.specialization).toLowerCase()
    )
    return byName?.name || doctor.specialization
  })()

  return {
    id: doctor?.doctorId || doctor?.id,
    name: fullName || doctor?.email || 'Doctor',
    specialty: specializationLabel,
    rating: 0,
    reviewCount: 0,
    verified: doctor?.status === 'ACTIVE',
    offersVideo: true,
    offersInPerson: true,
    experience: years,
    nextSlot: search.value.date ? `${search.value.date} · Check availability` : 'Check availability',
  }
}

async function loadHospitals() {
  try {
    hospitals.value = await getHospitals()
  } catch {
    hospitals.value = []
  }
}

async function loadSpecializations() {
  try {
    specializations.value = await getSpecializations()
  } catch {
    specializations.value = []
  }
}

async function loadDoctors() {
  if (!hasSpecializationFilter.value) {
    doctors.value = []
    error.value = ''
    loading.value = false
    return
  }

  const requestId = ++searchRequestId
  loading.value = true
  error.value = ''

  try {
    const results = await searchDoctors({
      specializationId: search.value.specializationId || undefined,
      specialty: search.value.specialty,
      name: search.value.name || undefined,
      hospital: search.value.hospital || undefined,
      date: search.value.date || undefined,
    })

    if (requestId !== searchRequestId) return
    doctors.value = Array.isArray(results) ? results.map(mapDoctorToCard).filter((d) => d.id) : []
  } catch (e) {
    if (requestId !== searchRequestId) return
    doctors.value = []
    error.value = e?.message || 'Unable to fetch doctors right now.'
  } finally {
    if (requestId === searchRequestId) {
      loading.value = false
    }
  }
}

watch(search, () => {
  loadDoctors()
})

onMounted(() => {
  loadSpecializations()
  loadHospitals()
  loadDoctors()
})
</script>
