<template>
  <article
    class="bg-card border border-border rounded-2xl p-5 transition-all duration-200"
    :class="hover ? 'hover:shadow-md hover:-translate-y-0.5 cursor-pointer' : ''"
  >
    <!-- Top row: avatar + name + badge -->
    <div class="flex items-start gap-4 mb-4">
      <!-- Avatar -->
      <div
        class="w-14 h-14 rounded-2xl flex items-center justify-center text-primary font-bold text-xl flex-shrink-0 bg-primary/10 select-none"
        :aria-label="`${doctor.name} photo placeholder`"
      >
        {{ initials }}
      </div>

      <div class="flex-1 min-w-0">
        <h2 class="font-bold text-ink truncate text-base">{{ doctor.name }}</h2>
        <p class="text-sm text-muted truncate">{{ doctor.specialty }}</p>

        <!-- Rating -->
        <div class="flex items-center gap-1 mt-1" :aria-label="`Rating: ${doctor.rating} out of 5 from ${doctor.reviewCount} reviews`">
          <span class="text-warning text-sm leading-none" aria-hidden="true">★</span>
          <span class="text-sm font-semibold text-ink">{{ doctor.rating }}</span>
          <span class="text-xs text-muted">({{ doctor.reviewCount }})</span>
        </div>
      </div>

      <!-- Verified badge -->
      <span
        v-if="doctor.verified"
        class="flex-shrink-0 text-xs font-semibold px-2.5 py-1 rounded-full bg-success/10 text-success"
        title="Verified doctor"
      >
        ✓ Verified
      </span>
    </div>

    <!-- Consultation type chips -->
    <div class="flex flex-wrap gap-2 mb-4" role="list" :aria-label="`${doctor.name} consultation types`">
      <span
        v-if="doctor.offersVideo"
        role="listitem"
        class="text-xs font-medium px-2.5 py-1 rounded-full bg-ai/10 text-ai"
      >
        📹 Video
      </span>
      <span
        v-if="doctor.offersInPerson"
        role="listitem"
        class="text-xs font-medium px-2.5 py-1 rounded-full bg-primary/10 text-primary"
      >
        🏥 In-Person
      </span>
      <span
        class="text-xs font-medium px-2.5 py-1 rounded-full bg-surface text-muted border border-border"
        role="listitem"
      >
        {{ doctor.experience }}
      </span>
    </div>

    <!-- Next available -->
    <p class="text-xs mb-4">
      <span class="font-semibold text-success">Next available: </span>
      <span class="text-muted">{{ doctor.nextSlot ?? 'Check availability' }}</span>
    </p>

    <!-- Actions slot — default: Book button -->
    <slot name="action">
      <RouterLink
        :to="`/find-doctor/${doctor.id}`"
        class="block w-full text-center py-2.5 bg-primary text-white text-sm font-semibold rounded-xl hover:bg-action transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
      >
        View Profile & Book
      </RouterLink>
    </slot>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  doctor: {
    type: Object,
    required: true,
    // Expected shape: { id, name, specialty, rating, reviewCount, verified,
    //   offersVideo, offersInPerson, experience, nextSlot }
  },
  hover: { type: Boolean, default: true },
})

const initials = computed(() =>
  (props.doctor.name ?? '')
    .split(' ')
    .map(w => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
)
</script>