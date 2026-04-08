import api from './api'

const paymentService = {
  async getPayments(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/payments', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getPayment(id: string) {
    try {
      const response = await api.get(`/api/payments/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getPaymentStats(period = 'month') {
    try {
      const response = await api.get('/api/payments/stats', { params: { period } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getRevenueAnalytics(startDate: string, endDate: string) {
    try {
      const response = await api.get('/api/payments/analytics/revenue', { params: { startDate, endDate } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getRevenueByMonth() {
    try {
      const response = await api.get('/api/payments/analytics/revenue-by-month')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getPaymentMethodBreakdown() {
    try {
      const response = await api.get('/api/payments/analytics/payment-methods')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getAppointmentTypeBreakdown() {
    try {
      const response = await api.get('/api/payments/analytics/appointment-types')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getRefundRequests(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/payments/refunds', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async processRefund(paymentId: string, data: Record<string, unknown>) {
    try {
      const response = await api.post(`/api/payments/${paymentId}/refund`, data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async approveRefund(refundId: string) {
    try {
      const response = await api.post(`/api/payments/refunds/${refundId}/approve`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async rejectRefund(refundId: string, reason: string) {
    try {
      const response = await api.post(`/api/payments/refunds/${refundId}/reject`, { reason })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getRecentPayments(limit = 10) {
    try {
      const response = await api.get('/api/payments/recent', { params: { limit } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getTopEarningDoctors(limit = 5) {
    try {
      const response = await api.get('/api/payments/analytics/top-doctors', { params: { limit } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getTopEarningHospitals(limit = 5) {
    try {
      const response = await api.get('/api/payments/analytics/top-hospitals', { params: { limit } })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default paymentService
