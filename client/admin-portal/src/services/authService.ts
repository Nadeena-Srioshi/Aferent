import api from './api'

const authService = {
  async login(credentials: { email: string; password: string }) {
    try {
      const response = await api.post('/api/auth/login', credentials)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async logout() {
    try {
      await api.post('/api/auth/logout')
    } catch (error) {
      console.error('Logout error:', error)
    }
  },

  async refreshToken(refreshToken: string) {
    try {
      const response = await api.post('/api/auth/refresh', { refreshToken })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getCurrentUser() {
    try {
      const response = await api.get('/api/auth/me')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async changePassword(data: { currentPassword: string; newPassword: string }) {
    try {
      const response = await api.post('/api/auth/change-password', data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async forgotPassword(email: string) {
    try {
      const response = await api.post('/api/auth/forgot-password', { email })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async resetPassword(token: string, newPassword: string) {
    try {
      const response = await api.post('/api/auth/reset-password', { token, newPassword })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default authService
