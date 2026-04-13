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
                v-model="selectedSpecialty"
                required
                class="w-full pl-10 pr-4 py-3.5 bg-white border border-border rounded-xl text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors appearance-none cursor-pointer"
              >
                <option value="">Specialization *</option>
                <option v-for="s in specialties" :key="s" :value="s">{{ s }}</option>
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
                <option v-for="option in hospitals" :key="option" :value="option">{{ option }}</option>
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
            v-for="tag in quickFilters"
            :key="tag"
            class="px-4 py-1.5 rounded-full text-sm font-medium border border-border text-muted hover:border-primary hover:text-primary hover:bg-primary/5 transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            @click="applyQuickFilter(tag)"
          >
            {{ tag }}
          </button>
        </div>
      </div>

    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Building2, CalendarDays, Search, Stethoscope, UserRound } from 'lucide-vue-next'

const router = useRouter()
const doctorName = ref('')
const selectedSpecialty = ref('')
const hospital = ref('')
const appointmentDate = ref('')
const error = ref('')

const specialties = [
  'Cardiology', 'Pediatrics', 'Mental Health', 'Dermatology',
  'Neurology', 'Orthopedics', 'Gynecology', 'Endocrinology',
]

const hospitals = [
  'Aferent Heart Institute',
  'Aferent Wellness Center',
  'Aferent Children\'s Hospital',
  'SkinCare Medical Center',
  'Aferent Women\'s Health Center',
  'Aferent Orthopedic Clinic',
]

const quickFilters = ['Cardiology', 'Pediatrics', 'Mental Health', 'Dermatology', 'Gynecology', 'Orthopedics']

const handleSearch = () => {
  if (!selectedSpecialty.value) {
    error.value = 'Please select a specialization before searching.'
    return
  }

  error.value = ''
  router.push({
    path: '/find-doctor',
    query: {
      name: doctorName.value || undefined,
      specialty: selectedSpecialty.value || undefined,
      hospital: hospital.value || undefined,
      date: appointmentDate.value || undefined,
    },
  })
}

const applyQuickFilter = (tag) => {
  selectedSpecialty.value = tag
  handleSearch()
}
</script>