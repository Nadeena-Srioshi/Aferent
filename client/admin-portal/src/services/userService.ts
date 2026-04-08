import api from './api'

const userService = {
  async getUsers(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/users', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getUsersByRole(role: string, params: Record<string, unknown> = {}) {
    try {
      const response = await api.get(`/api/users/role/${role}`, { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getUser(id: string) {
    try {
      const response = await api.get(`/api/users/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async createUser(data: Record<string, unknown>) {
    try {
      const response = await api.post('/api/users', data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async updateUser(id: string, data: Record<string, unknown>) {
    try {
      const response = await api.put(`/api/users/${id}`, data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async deleteUser(id: string) {
    try {
      const response = await api.delete(`/api/users/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async toggleUserStatus(id: string) {
    try {
      const response = await api.patch(`/api/users/${id}/toggle-status`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getUserStats() {
    try {
      const response = await api.get('/api/users/stats')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default userService
