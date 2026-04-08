import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authService from '@/services/authService'
import { STORAGE_KEYS, USER_ROLES } from '@/utils/constants'
import router from '@/router'
import websocketService from '@/services/websocketService'

interface User {
  id: string
  email: string
  role: string
  [key: string]: unknown
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === USER_ROLES.ADMIN)
  const userName = computed(() => user.value?.email?.split('@')[0] || 'Admin')

  async function login(credentials: { email: string; password: string }) {
    try {
      loading.value = true
      error.value = null

      const response = await authService.login(credentials)

      if (response.user.role !== USER_ROLES.ADMIN) {
        throw new Error('Access denied. Admin privileges required.')
      }

      accessToken.value = response.accessToken
      refreshToken.value = response.refreshToken
      user.value = response.user

      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, response.accessToken)
      localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, response.refreshToken)
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(response.user))

      websocketService.connect()

      return response
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Login failed'
      error.value = message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await authService.logout()
    } catch (err) {
      console.error('Logout error:', err)
    } finally {
      user.value = null
      accessToken.value = null
      refreshToken.value = null

      localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
      localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
      localStorage.removeItem(STORAGE_KEYS.USER)

      websocketService.disconnect()
      router.push('/login')
    }
  }

  async function refreshAccessToken() {
    try {
      const response = await authService.refreshToken(refreshToken.value!)
      accessToken.value = response.accessToken
      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, response.accessToken)
      return response.accessToken
    } catch (err) {
      await logout()
      throw err
    }
  }

  async function fetchCurrentUser() {
    try {
      const response = await authService.getCurrentUser()
      user.value = response
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(response))
      return response
    } catch (err) {
      console.error('Fetch user error:', err)
      throw err
    }
  }

  function initializeAuth() {
    const storedToken = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    const storedRefreshToken = localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
    const storedUser = localStorage.getItem(STORAGE_KEYS.USER)

    if (storedToken && storedUser) {
      accessToken.value = storedToken
      refreshToken.value = storedRefreshToken
      user.value = JSON.parse(storedUser)
      websocketService.connect()
    }
  }

  async function changePassword(currentPassword: string, newPassword: string) {
    try {
      loading.value = true
      error.value = null
      await authService.changePassword({ currentPassword, newPassword })
      return true
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to change password'
      error.value = message
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    user, accessToken, refreshToken, loading, error,
    isAuthenticated, isAdmin, userName,
    login, logout, refreshAccessToken, fetchCurrentUser, initializeAuth, changePassword,
  }
})
