<template>
  <div class="flex h-screen overflow-hidden" style="background-color: var(--bg-secondary)">
    <!-- Sidebar -->
    <Sidebar :collapsed="sidebarCollapsed" @toggle="toggleSidebar" />

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col overflow-hidden" :class="{ 'ml-64': !sidebarCollapsed, 'ml-20': sidebarCollapsed }">
      <!-- Navbar -->
      <Navbar @toggle-sidebar="toggleSidebar" />

      <!-- Page Content -->
      <main class="flex-1 overflow-x-hidden overflow-y-auto p-6">
        <router-view />
      </main>
    </div>

    <!-- Confirmation Dialog -->
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import Sidebar from '@/components/layout/Sidebar.vue'
import Navbar from '@/components/layout/Navbar.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const authStore = useAuthStore()
const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

onMounted(() => {
  // Initialize auth on mount
  authStore.initializeAuth()
})
</script>

<style scoped>
/* Smooth transitions */
.ml-64, .ml-20 {
  transition: margin-left 0.3s ease-in-out;
}
</style>
