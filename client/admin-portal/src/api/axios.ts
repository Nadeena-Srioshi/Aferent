import axios from 'axios'

// All requests go through the API Gateway
const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor — attach JWT to every request automatically
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  const role = localStorage.getItem('admin_role')  // Assuming you store the role
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (role) {
    config.headers['X-User-Role'] = role
  }
  return config
})

// Response interceptor — handle 401/403 globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      // Token expired or invalid — clear session and redirect to login
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_role')
      localStorage.removeItem('admin_authId')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export default api