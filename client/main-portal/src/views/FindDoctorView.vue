<template>
  <main id="main-content" class="bg-surface min-h-screen">
    <section class="border-b border-border bg-white">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div class="max-w-3xl">
          <p class="text-sm font-semibold text-primary uppercase tracking-[0.2em] mb-2">Doctor search results</p>
          <h1 class="text-3xl sm:text-4xl font-bold text-ink mb-3">Find the right doctor</h1>
        </div>
      </div>
    </section>

    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="grid grid-cols-1 xl:grid-cols-[320px_minmax(0,1fr)] gap-6">
        <!-- Active search summary -->
        <aside class="bg-card border border-border rounded-2xl p-5 h-fit sticky top-24">
          <h2 class="font-bold text-ink mb-4">Search filters</h2>

          <div class="space-y-4 text-sm">
            <div>
              <p class="text-muted mb-1">Specialization</p>
              <p class="font-semibold text-ink">{{ search.specialty || '—' }}</p>
            </div>
            <div>
              <p class="text-muted mb-1">Doctor name</p>
              <p class="font-semibold text-ink">{{ search.name || 'Any' }}</p>
            </div>
            <div>
              <p class="text-muted mb-1">Hospital</p>
              <p class="font-semibold text-ink">{{ search.hospital || 'Any' }}</p>
            </div>
            <div>
              <p class="text-muted mb-1">Date</p>
              <p class="font-semibold text-ink">{{ search.date || 'Any' }}</p>
            </div>
          </div>

          <p v-if="!search.specialty" class="mt-4 text-sm text-warning bg-warning/10 px-4 py-3 rounded-xl">
            Pick a specialization from the search box.
          </p>
        </aside>

        <!-- Results -->
        <div>
          <div class="flex items-center justify-between gap-3 mb-5">
            <p class="text-sm text-muted">
              <span class="font-semibold text-ink">{{ filteredDoctors.length }}</span>
              doctor{{ filteredDoctors.length === 1 ? '' : 's' }} found
            </p>
          </div>

          <div v-if="!search.specialty" class="bg-card border border-border rounded-2xl p-8 text-center">
            <h2 class="text-xl font-bold text-ink mb-2">Choose a specialization to begin</h2>
            <p class="text-muted">Your homepage search requires a specialization. Once selected, matching doctors will appear here.</p>
          </div>

          <div v-else-if="filteredDoctors.length" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            <Doctorcard
              v-for="doctor in filteredDoctors"
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
import { computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import Doctorcard from '@/components/shared/Doctorcard.vue'

const route = useRoute()

const search = computed(() => ({
  name: typeof route.query.name === 'string' ? route.query.name.trim() : '',
  specialty: typeof route.query.specialty === 'string' ? route.query.specialty.trim() : '',
  hospital: typeof route.query.hospital === 'string' ? route.query.hospital.trim() : '',
  date: typeof route.query.date === 'string' ? route.query.date.trim() : '',
}))

const doctors = [
  {
    id: 'dr-sarah-mitchell',
    name: 'Dr. Sarah Mitchell',
    specialty: 'Cardiology',
    hospital: 'Aferent Heart Institute',
    date: '2026-04-14',
    rating: 4.9,
    reviewCount: 218,
    verified: true,
    offersVideo: true,
    offersInPerson: true,
    experience: '12 years experience',
    nextSlot: '14 Apr 2026 · 10:30 AM',
  },
  {
    id: 'dr-riya-patel',
    name: 'Dr. Riya Patel',
    specialty: 'Mental Health',
    hospital: 'Aferent Wellness Center',
    date: '2026-04-16',
    rating: 4.8,
    reviewCount: 164,
    verified: true,
    offersVideo: true,
    offersInPerson: false,
    experience: '9 years experience',
    nextSlot: '16 Apr 2026 · 2:00 PM',
  },
  {
    id: 'dr-omar-khan',
    name: 'Dr. Omar Khan',
    specialty: 'Pediatrics',
    hospital: 'Aferent Children’s Hospital',
    date: '2026-04-14',
    rating: 4.7,
    reviewCount: 143,
    verified: true,
    offersVideo: false,
    offersInPerson: true,
    experience: '11 years experience',
    nextSlot: '14 Apr 2026 · 1:15 PM',
  },
  {
    id: 'dr-anika-sharma',
    name: 'Dr. Anika Sharma',
    specialty: 'Dermatology',
    hospital: 'SkinCare Medical Center',
    date: '2026-04-18',
    rating: 4.9,
    reviewCount: 192,
    verified: false,
    offersVideo: true,
    offersInPerson: true,
    experience: '8 years experience',
    nextSlot: '18 Apr 2026 · 11:00 AM',
  },
  {
    id: 'dr-emma-lee',
    name: 'Dr. Emma Lee',
    specialty: 'Gynecology',
    hospital: 'Aferent Women’s Health Center',
    date: '2026-04-15',
    rating: 4.8,
    reviewCount: 176,
    verified: true,
    offersVideo: false,
    offersInPerson: true,
    experience: '10 years experience',
    nextSlot: '15 Apr 2026 · 9:00 AM',
  },
  {
    id: 'dr-noah-wilson',
    name: 'Dr. Noah Wilson',
    specialty: 'Orthopedics',
    hospital: 'Aferent Orthopedic Clinic',
    date: '2026-04-20',
    rating: 4.6,
    reviewCount: 98,
    verified: true,
    offersVideo: true,
    offersInPerson: true,
    experience: '15 years experience',
    nextSlot: '20 Apr 2026 · 4:15 PM',
  },
]

const normalized = (value) => String(value || '').trim().toLowerCase()

const filteredDoctors = computed(() => {
  if (!search.value.specialty) return []

  const name = normalized(search.value.name)
  const specialty = normalized(search.value.specialty)
  const hospital = normalized(search.value.hospital)
  const date = normalized(search.value.date)

  return doctors.filter((doctor) => {
    const doctorName = normalized(doctor.name)
    const doctorSpecialty = normalized(doctor.specialty)
    const doctorHospital = normalized(doctor.hospital)
    const doctorDate = normalized(doctor.date)

    const matchesSpecialty = doctorSpecialty.includes(specialty)
    const matchesName = !name || doctorName.includes(name)
    const matchesHospital = !hospital || doctorHospital.includes(hospital)
    const matchesDate = !date || doctorDate === date

    return matchesSpecialty && matchesName && matchesHospital && matchesDate
  })
})

function selectDoctor(doctor) {
  // Placeholder action: keep the flow on the results page until booking/details routes are added.
  console.info('Selected doctor:', doctor)
}
</script>
