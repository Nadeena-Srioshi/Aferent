<template>
  <div class="login-page">
    <div class="login-card">
      <!-- Header -->
      <div class="login-header">
        <div class="login-icon">
          <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <h1>{{ appName }}</h1>
        <p>Administrator Login</p>
      </div>

      <!-- Error -->
      <div v-if="error" class="alert alert-danger">{{ error }}</div>

      <!-- Form -->
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">Email</label>
          <input v-model="credentials.email" type="email" class="input"
            :class="{ 'input-error': errors.email }" placeholder="admin@example.com" autocomplete="email" />
          <span v-if="errors.email" class="form-error">{{ errors.email }}</span>
        </div>

        <div class="form-group">
          <label class="form-label">Password</label>
          <div style="position:relative">
            <input v-model="credentials.password" :type="showPassword ? 'text' : 'password'"
              class="input" style="padding-right:2.5rem"
              :class="{ 'input-error': errors.password }" placeholder="Password" autocomplete="current-password" />
            <button type="button" @click="showPassword = !showPassword" class="pw-toggle">
              <svg v-if="!showPassword" width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
              </svg>
              <svg v-else width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
              </svg>
            </button>
          </div>
          <span v-if="errors.password" class="form-error">{{ errors.password }}</span>
        </div>

        <button type="submit" class="btn btn-primary" style="width:100%;margin-top:8px" :disabled="loading">
          <span v-if="loading" class="spinner spinner-sm"></span>
          <span v-else>Sign In</span>
        </button>
      </form>

      <p class="login-footer">© 2024 Aferent Healthcare</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { APP_NAME } from '@/utils/constants'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const appName = APP_NAME
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

const credentials = reactive({ email: '', password: '' })
const errors = reactive({ email: '', password: '' })

const validateForm = () => {
  errors.email = ''
  errors.password = ''
  let valid = true
  if (!credentials.email) { errors.email = 'Email is required'; valid = false }
  else if (!/\S+@\S+\.\S+/.test(credentials.email)) { errors.email = 'Invalid email'; valid = false }
  if (!credentials.password) { errors.password = 'Password is required'; valid = false }
  else if (credentials.password.length < 6) { errors.password = 'Min 6 characters'; valid = false }
  return valid
}

const handleLogin = async () => {
  error.value = ''
  if (!validateForm()) return
  try {
    loading.value = true
    await authStore.login(credentials)
    toast.success('Welcome back!')
    router.push('/')
  } catch (err: unknown) {
    error.value = (err as { message?: string })?.message || 'Invalid credentials'
    toast.error(error.value)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-secondary);
  padding: 16px;
}

.login-card {
  background-color: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  box-shadow: var(--card-shadow);
  padding: 32px;
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-icon {
  width: 48px;
  height: 48px;
  background-color: var(--primary-500);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  color: white;
}

.login-header h1 {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.login-header p {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
}

.pw-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.pw-toggle:hover { color: var(--text-primary); }

.login-footer {
  text-align: center;
  font-size: 0.75rem;
  color: var(--text-tertiary);
  margin-top: 20px;
  margin-bottom: 0;
}
</style>