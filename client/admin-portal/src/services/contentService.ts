import api from './api'

const contentService = {
  async getFAQs(params: Record<string, unknown> = {}) {
    try {
      const response = await api.get('/api/content/faqs', { params })
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getFAQ(id: string) {
    try {
      const response = await api.get(`/api/content/faqs/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async createFAQ(data: Record<string, unknown>) {
    try {
      const response = await api.post('/api/content/faqs', data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async updateFAQ(id: string, data: Record<string, unknown>) {
    try {
      const response = await api.put(`/api/content/faqs/${id}`, data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async deleteFAQ(id: string) {
    try {
      const response = await api.delete(`/api/content/faqs/${id}`)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getTerms() {
    try {
      const response = await api.get('/api/content/terms')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async updateTerms(data: Record<string, unknown>) {
    try {
      const response = await api.put('/api/content/terms', data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getTermsHistory() {
    try {
      const response = await api.get('/api/content/terms/history')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getPrivacyPolicy() {
    try {
      const response = await api.get('/api/content/privacy')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async updatePrivacyPolicy(data: Record<string, unknown>) {
    try {
      const response = await api.put('/api/content/privacy', data)
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },

  async getPrivacyHistory() {
    try {
      const response = await api.get('/api/content/privacy/history')
      return response.data
    } catch (error: unknown) {
      throw (error as { response?: { data: unknown } }).response?.data || error
    }
  },
}

export default contentService
