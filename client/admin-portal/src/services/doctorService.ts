import api from './api'

const doctorService = {
  async getDoctors(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/doctors', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getPendingVerifications(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/doctors/verifications/pending', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getVerifiedDoctors(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/doctors/verifications/approved', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getRejectedDoctors(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/doctors/verifications/rejected', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getDoctor(id: string) {
    try {
      const response = await api.get(`/api/doctors/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getDoctorDocuments(id: string) {
    try {
      const response = await api.get(`/api/doctors/${id}/documents`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async approveDoctor(id: string, comments = '') {
    try {
      const response = await api.post(`/api/doctors/${id}/verify`, { status: 'APPROVED', comments })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async rejectDoctor(id: string, reason: string) {
    try {
      const response = await api.post(`/api/doctors/${id}/verify`, { status: 'REJECTED', comments: reason })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getVerificationStats() {
    try {
      const response = await api.get('/api/doctors/verifications/stats')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getTopDoctors(limit = 5) {
    try {
      const response = await api.get('/api/doctors/top', { params: { limit } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default doctorService
