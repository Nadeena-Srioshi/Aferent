<template>
  <!-- Skip to main content — keyboard / screen reader users -->
  <a
    href="#main-content"
    class="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[100] focus:px-4 focus:py-2 focus:bg-primary focus:text-white focus:rounded-lg focus:text-sm focus:font-semibold"
  >
    Skip to main content
  </a>

  <!-- Global page-level loading overlay (route transitions) -->
  <ThePageLoader :show="routeLoading" message="Loading page…" />

  <!-- Layout -->
  <TheNavbar />
  <RouterView v-slot="{ Component, route }">
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
      mode="out-in"
    >
      <component :is="Component" :key="route.path" />
    </Transition>
  </RouterView>
  <TheFooter />

  <!-- Global toast notifications (rendered via Teleport to body) -->
  <TheToastStack />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, RouterView } from 'vue-router'

import TheNavbar     from '@/components/layout/TheNavbar.vue'
import TheFooter     from '@/components/layout/TheFooter.vue'
import TheToastStack from '@/components/shared/Thetoaststack.vue'
import ThePageLoader from '@/components/shared/ThePageLoader.vue'

import { useAuth } from '@/stores/useAuth'

const router       = useRouter()
const routeLoading = ref(false)

// Route transition loader
router.beforeEach(() => { routeLoading.value = true })
router.afterEach(()  => { routeLoading.value = false })

// Boot: rehydrate auth session from stored token on every page load
const auth = useAuth()
onMounted(async () => {
  if (auth.token) {
    await auth.fetchMe()
  }
})
</script>