import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'

export const useAuthStore = defineStore('auth', () => {
  // State — initialise from localStorage so a page refresh keeps you logged in
  const token = ref<string | null>(localStorage.getItem('admin_token'))
  const role = ref<string | null>(localStorage.getItem('admin_role'))
  const authId = ref<string | null>(localStorage.getItem('admin_authId'))

  // Getters
  const isLoggedIn = computed(() => !!token.value && role.value === 'ADMIN')

  // Actions
  function setSession(data: { accessToken: string; role: string; authId: string }) {
    token.value = data.accessToken
    role.value = data.role
    authId.value = data.authId

    localStorage.setItem('admin_token', data.accessToken)
    localStorage.setItem('admin_role', data.role)
    localStorage.setItem('admin_authId', data.authId)
  }

  function logout() {
    token.value = null
    role.value = null
    authId.value = null

    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_role')
    localStorage.removeItem('admin_authId')
  }

  return { token, role, authId, isLoggedIn, setSession, logout }
})