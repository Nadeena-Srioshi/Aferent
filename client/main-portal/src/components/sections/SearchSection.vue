<template>
  <section class="bg-white border-b border-border" aria-labelledby="search-heading">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

      <div class="text-center mb-8">
        <h2 id="search-heading" class="text-2xl sm:text-3xl font-bold text-ink mb-2">
          Find the care you need
        </h2>
        <p class="text-muted">Search by doctor name, specialty, or condition</p>
      </div>

      <!-- Search bar -->
      <div class="max-w-3xl mx-auto">
        <div class="flex flex-col sm:flex-row gap-3 bg-surface border border-border rounded-2xl p-3 shadow-sm">

          <!-- Specialty select -->
          <div class="relative flex-1 min-w-0">
            <label for="specialty-select" class="sr-only">Select specialty</label>
            <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none" aria-hidden="true">
              <svg class="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.153-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"/>
              </svg>
            </div>
            <select
              id="specialty-select"
              v-model="selectedSpecialty"
              class="w-full pl-10 pr-4 py-3 bg-white border border-border rounded-xl text-sm text-ink focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors appearance-none cursor-pointer"
            >
              <option value="">All Specialties</option>
              <option v-for="s in specialties" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>

          <!-- Keyword input -->
          <div class="relative flex-[2] min-w-0">
            <label for="search-input" class="sr-only">Search doctors or conditions</label>
            <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none" aria-hidden="true">
              <svg class="w-5 h-5 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
            </div>
            <input
              id="search-input"
              v-model="searchQuery"
              type="search"
              placeholder="Doctor name, condition, or keyword…"
              class="w-full pl-10 pr-4 py-3 bg-white border border-border rounded-xl text-sm text-ink placeholder-muted focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors"
              @keyup.enter="handleSearch"
            />
          </div>

          <!-- Search button -->
          <button
            class="flex items-center justify-center gap-2 px-6 py-3 bg-primary text-white font-semibold rounded-xl hover:bg-action transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 flex-shrink-0"
            @click="handleSearch"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
            <span>Search</span>
          </button>
        </div>

        <!-- Quick filters -->
        <div class="mt-4 flex flex-wrap gap-2 justify-center" role="group" aria-label="Quick specialty filters">
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
const searchQuery = ref('')
const selectedSpecialty = ref('')

const specialties = [
  'Cardiology', 'Pediatrics', 'Mental Health', 'Dermatology',
  'Neurology', 'Orthopedics', 'Gynecology', 'Endocrinology',
]

const quickFilters = ['Anxiety & Depression', 'Heart Health', 'Child Care', 'Skin Issues', 'Women\'s Health', 'Diabetes']

const handleSearch = () => {
  router.push({
    path: '/find-doctor',
    query: {
      q: searchQuery.value || undefined,
      specialty: selectedSpecialty.value || undefined,
    },
  })
}

const applyQuickFilter = (tag) => {
  searchQuery.value = tag
  handleSearch()
}
</script>