import { defineStore } from 'pinia'
import authService from '@/services/authService'

const TOKEN_STORAGE_KEY = 'aferent.auth.accessToken'
const USER_STORAGE_KEY = 'aferent.auth.user'

function safeParse(value, fallback = null) {
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function parseJwt(token) {
  if (!token || typeof token !== 'string') return null

  const parts = token.split('.')
  if (parts.length < 2) return null

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join(''),
    )

    return JSON.parse(json)
  } catch {
    return null
  }
}

function tokenExpired(token) {
  const claims = parseJwt(token)
  if (!claims?.exp) return false
  return Date.now() >= claims.exp * 1000
}

function initialsFromName(name) {
  if (!name || typeof name !== 'string') return 'P'
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'P'
  return parts
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('')
}

export const useAuth = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) || '',
    user: safeParse(localStorage.getItem(USER_STORAGE_KEY), null),
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isPatient: (state) => state.user?.role === 'PATIENT',
    fullName: (state) => {
      const direct = state.user?.name?.trim()
      if (direct) return direct

      const emailName = state.user?.email?.split('@')?.[0]
      if (emailName) return emailName

      return 'Patient'
    },
    initials() {
      return initialsFromName(this.fullName)
    },
  },

  actions: {
    saveSession(token, user) {
      this.token = token || ''
      this.user = user || null

      if (this.token) {
        localStorage.setItem(TOKEN_STORAGE_KEY, this.token)
        localStorage.setItem('accessToken', this.token) // legacy compatibility for older views
      } else {
        localStorage.removeItem(TOKEN_STORAGE_KEY)
        localStorage.removeItem('accessToken')
      }

      if (this.user) {
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(this.user))
      } else {
        localStorage.removeItem(USER_STORAGE_KEY)
      }
    },

    hydrateUserFromToken(token, fallbackUser = null) {
      const claims = parseJwt(token) || {}
      const mergedUser = {
        ...(this.user || {}),
        ...(fallbackUser || {}),
        authId: claims.sub || fallbackUser?.authId || this.user?.authId || null,
        email: claims.email || fallbackUser?.email || this.user?.email || null,
        role: claims.role || fallbackUser?.role || this.user?.role || null,
      }

      this.saveSession(token, mergedUser)
      return mergedUser
    },

    async login({ email, password }) {
      const data = await authService.login({ email, password })
      this.hydrateUserFromToken(data.accessToken, {
        role: data.role,
        authId: data.authId,
        email,
      })
      return this.user
    },

    async register({ email, password, role = 'PATIENT' }) {
      const data = await authService.register({ email, password, role })
      this.hydrateUserFromToken(data.accessToken, {
        role: data.role,
        authId: data.authId,
        email,
      })
      return this.user
    },

    async fetchMe() {
      if (!this.token) {
        this.saveSession('', null)
        return null
      }

      if (tokenExpired(this.token)) {
        try {
          const refreshed = await authService.refresh()
          this.saveSession(refreshed.accessToken || '', this.user)
        } catch {
          this.saveSession('', null)
          return null
        }
      }

      return this.hydrateUserFromToken(this.token)
    },

    async logout() {
      try {
        await authService.logout()
      } catch {
        // ignore logout transport errors and clear local session anyway
      } finally {
        this.saveSession('', null)
      }
    },

    // Doctor auth is intentionally excluded for now.
    // Future work: create a dedicated doctorAuthStore.js with doctor-specific flows and claims.
  },
})
