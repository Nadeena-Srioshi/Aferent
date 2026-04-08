import api from './api'

const appointmentService = {
  async getAppointments(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/appointments', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getAppointment(id: string) {
    try {
      const response = await api.get(`/api/appointments/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getAppointmentStats() {
    try {
      const response = await api.get('/api/appointments/stats')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getTodayAppointments() {
    try {
      const response = await api.get('/api/appointments/today')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getAppointmentStatusBreakdown() {
    try {
      const response = await api.get('/api/appointments/analytics/status-breakdown')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getAppointmentTypeBreakdown() {
    try {
      const response = await api.get('/api/appointments/analytics/type-breakdown')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getUserGrowth(period = '12months') {
    try {
      const response = await api.get('/api/appointments/analytics/user-growth', { params: { period } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default appointmentService
