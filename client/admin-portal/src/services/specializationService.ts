import api from './api'

const specializationService = {
  async getSpecializations(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/specializations', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getSpecialization(id: string) {
    try {
      const response = await api.get(`/api/specializations/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async createSpecialization(data: Record<string, unknown>) {
    try {
      const response = await api.post('/api/specializations', data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async updateSpecialization(id: string, data: Record<string, unknown>) {
    try {
      const response = await api.put(`/api/specializations/${id}`, data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async deleteSpecialization(id: string) {
    try {
      const response = await api.delete(`/api/specializations/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async toggleSpecializationStatus(id: string) {
    try {
      const response = await api.patch(`/api/specializations/${id}/toggle-status`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getSpecializationStats(id: string) {
    try {
      const response = await api.get(`/api/specializations/${id}/stats`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getTopSpecializations(limit = 5) {
    try {
      const response = await api.get('/api/specializations/top', { params: { limit } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default specializationService
