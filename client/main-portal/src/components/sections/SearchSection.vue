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
                <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.153-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"/>
                </svg>
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
                <svg class="w-5 h-5 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.121 17.804A4 4 0 018 16h8a4 4 0 012.879 1.204M15 11a3 3 0 11-6 0 3 3 0 016 0zM12 22a10 10 0 100-20 10 10 0 000 20z"/>
                </svg>
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
                <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-2 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                </svg>
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
                <svg class="w-5 h-5 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
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
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
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