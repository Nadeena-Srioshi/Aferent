<template>
  <header
    class="flex items-center justify-between h-16 px-6 border-b flex-shrink-0"
    :style="{ backgroundColor: 'var(--navbar-bg)', borderColor: 'var(--navbar-border)' }"
  >
    <!-- Left: hamburger -->
    <button
      @click="$emit('toggle-sidebar')"
      class="p-2 rounded-lg transition-colors"
      style="color: var(--text-secondary)"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
      </svg>
    </button>

    <!-- Right: theme + user -->
    <div class="flex items-center gap-3">
      <!-- Theme toggle -->
      <button
        @click="themeStore.toggleTheme()"
        class="p-2 rounded-lg transition-colors"
        style="color: var(--text-secondary)"
        :title="themeStore.isDark ? 'Switch to light mode' : 'Switch to dark mode'"
      >
        <svg v-if="themeStore.isDark" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
        <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
        </svg>
      </button>

      <!-- User menu -->
      <div class="relative" ref="userMenuRef">
        <button
          @click="userMenuOpen = !userMenuOpen"
          class="flex items-center gap-2 p-2 rounded-lg transition-colors"
          style="color: var(--text-primary)"
        >
          <div class="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center text-white text-sm font-medium">
            {{ authStore.userName.charAt(0).toUpperCase() }}
          </div>
          <span class="text-sm font-medium hidden sm:block">{{ authStore.userName }}</span>
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        <!-- Dropdown -->
        <div
          v-if="userMenuOpen"
          class="absolute right-0 mt-1 w-48 rounded-lg shadow-lg border py-1 z-50"
          :style="{ backgroundColor: 'var(--card-bg)', borderColor: 'var(--border-color)' }"
        >
          <button
            @click="handleLogout"
            class="flex items-center gap-2 w-full px-4 py-2 text-sm transition-colors"
            style="color: var(--text-primary)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            Sign Out
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

defineEmits<{ 'toggle-sidebar': [] }>()

const authStore = useAuthStore()
const themeStore = useThemeStore()
const userMenuOpen = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)

const handleLogout = async () => {
  userMenuOpen.value = false
  await authStore.logout()
}

const handleClickOutside = (e: MouseEvent) => {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target as Node)) {
    userMenuOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>
