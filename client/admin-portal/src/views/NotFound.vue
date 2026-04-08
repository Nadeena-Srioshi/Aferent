<template>
  <div class="min-h-screen flex items-center justify-center" style="background-color: var(--bg-secondary)">
    <div class="text-center">
      <h1 class="text-9xl font-bold text-primary-500">404</h1>
      <h2 class="text-3xl font-semibold mt-4" style="color: var(--text-primary)">Page Not Found</h2>
      <p class="text-lg mt-2" style="color: var(--text-secondary)">
        The page you're looking for doesn't exist.
      </p>
      <button
        @click="goHome"
        class="btn btn-primary mt-8"
      >
        Go Back Home
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>
