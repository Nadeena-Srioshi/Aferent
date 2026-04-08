import api from './api'

const hospitalService = {
  async getHospitals(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/hospitals', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getHospital(id: string) {
    try {
      const response = await api.get(`/api/hospitals/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async createHospital(data: Record<string, unknown>) {
    try {
      const response = await api.post('/api/hospitals', data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async updateHospital(id: string, data: Record<string, unknown>) {
    try {
      const response = await api.put(`/api/hospitals/${id}`, data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async deleteHospital(id: string) {
    try {
      const response = await api.delete(`/api/hospitals/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async toggleHospitalStatus(id: string) {
    try {
      const response = await api.patch(`/api/hospitals/${id}/toggle-status`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getHospitalStats(id: string) {
    try {
      const response = await api.get(`/api/hospitals/${id}/stats`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getHospitalDoctors(id: string, params: Record<string, unknown> = {}) {
    try {
      const response = await api.get(`/api/hospitals/${id}/doctors`, { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default hospitalService
