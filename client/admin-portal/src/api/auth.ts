import api from './axios'

export interface LoginPayload {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  role: string
  authId: string
}

export const authApi = {
  login(payload: LoginPayload) {
    return api.post<AuthResponse>('/auth/login', payload)
  },

  logout() {
    return api.post('/auth/logout')
  },
}