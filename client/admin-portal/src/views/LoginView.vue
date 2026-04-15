<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const mounted = ref(false)

// If already logged in, skip to dashboard
onMounted(() => {
  mounted.value = true
  if (auth.isLoggedIn) {
    router.replace('/dashboard')
    return
  }
  // Show message if redirected here due to unauthorized access
  if (route.query.error === 'unauthorized') {
    error.value = 'Access denied. Admin accounts only.'
  }
})

async function handleLogin() {
  if (!email.value || !password.value) {
    error.value = 'Please enter your email and password.'
    return
  }

  error.value = ''
  loading.value = true

  try {
    const res = await authApi.login({ email: email.value, password: password.value })
    const data = res.data

    if (data.role !== 'ADMIN') {
      error.value = 'Access denied. This portal is for administrators only.'
      return
    }

    auth.setSession({
      accessToken: data.accessToken,
      role: data.role,
      authId: data.authId,
    })
    console.log('Login successful, redirecting to dashboard...')
    //router.push('/dashboard')
  } catch (err: any) {
    const msg = err.response?.data?.message || err.response?.data || ''
    if (err.response?.status === 401 || err.response?.status === 400) {
      error.value = 'Invalid email or password.'
    } else if (typeof msg === 'string' && msg.length > 0) {
      error.value = msg
    } else {
      error.value = 'Unable to connect. Please try again.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page" :class="{ mounted }">

    <!-- Left panel — branding -->
    <div class="left-panel">
      <div class="panel-inner">
        <div class="brand">
          <div class="brand-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="white" fill-opacity="0.15"/>
              <path d="M14 6C14 6 8 10 8 15.5C8 18.537 10.686 21 14 21C17.314 21 20 18.537 20 15.5C20 10 14 6 14 6Z" fill="white"/>
              <path d="M14 21V13M11 16H17" stroke="#1a3a5c" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </div>
          <span class="brand-name">MediCore</span>
        </div>

        <div class="panel-content">
          <h1 class="panel-heading">Healthcare<br/>Admin Portal</h1>
          <p class="panel-sub">Manage patients, doctors, appointments and platform operations from a single secure interface.</p>

          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>Patient & doctor management</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>Appointment oversight</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>Payment & refund control</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>Notification monitoring</span>
            </div>
          </div>
        </div>

        <p class="panel-footer">© 2025 MediCore Platform</p>
      </div>

      <!-- Decorative circles -->
      <div class="deco deco-1"></div>
      <div class="deco deco-2"></div>
    </div>

    <!-- Right panel — form -->
    <div class="right-panel">
      <div class="form-wrapper">

        <div class="form-header">
          <h2 class="form-title">Sign in</h2>
          <p class="form-subtitle">Administrator access only</p>
        </div>

        <!-- Error alert -->
        <Transition name="fade-down">
          <div v-if="error" class="error-alert" role="alert">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <circle cx="8" cy="8" r="7" stroke="#c0392b" stroke-width="1.5"/>
              <path d="M8 5v3.5M8 10.5v.5" stroke="#c0392b" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            {{ error }}
          </div>
        </Transition>

        <form @submit.prevent="handleLogin" novalidate>

          <!-- Email -->
          <div class="field-group">
            <label class="field-label" for="email">Email address</label>
            <div class="input-wrap">
              <svg class="input-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                <rect x="1" y="3" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.3"/>
                <path d="M1 5.5L8 9.5L15 5.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              </svg>
              <input
                id="email"
                v-model="email"
                type="email"
                class="field-input"
                placeholder="admin@medicore.lk"
                autocomplete="email"
                :disabled="loading"
              />
            </div>
          </div>

          <!-- Password -->
          <div class="field-group">
            <label class="field-label" for="password">Password</label>
            <div class="input-wrap">
              <svg class="input-icon" width="16" height="16" viewBox="0 0 16 16" fill="none">
                <rect x="3" y="7" width="10" height="8" rx="1.5" stroke="currentColor" stroke-width="1.3"/>
                <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
              </svg>
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                class="field-input"
                placeholder="••••••••"
                autocomplete="current-password"
                :disabled="loading"
              />
              <button
                type="button"
                class="eye-btn"
                @click="showPassword = !showPassword"
                :aria-label="showPassword ? 'Hide password' : 'Show password'"
              >
                <!-- Eye open -->
                <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M1 8C1 8 3.5 3 8 3C12.5 3 15 8 15 8C15 8 12.5 13 8 13C3.5 13 1 8 1 8Z" stroke="currentColor" stroke-width="1.3"/>
                  <circle cx="8" cy="8" r="2" stroke="currentColor" stroke-width="1.3"/>
                </svg>
                <!-- Eye closed -->
                <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M2 2L14 14M6.5 6.7C6.19 7.04 6 7.5 6 8C6 9.1 6.9 10 8 10C8.5 10 8.96 9.81 9.3 9.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                  <path d="M4.15 4.3C2.8 5.3 1.5 7 1.5 8C1.5 8 4 13 8 13C9.45 13 10.75 12.4 11.8 11.7M6.5 3.1C7 3.03 7.5 3 8 3C12 3 14.5 8 14.5 8C14.5 8 13.9 9.3 12.8 10.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- Submit -->
          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="!loading">Sign in to portal</span>
            <span v-else class="btn-loading">
              <span class="spinner"></span>
              Signing in…
            </span>
          </button>

        </form>

        <p class="form-note">
          This portal is restricted to authorised administrators.<br/>
          Contact your system administrator if you need access.
        </p>
      </div>
    </div>

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=DM+Sans:wght@300;400;500&display=swap');

/* ── Layout ─────────────────────────────────────────────── */
.login-page {
  display: flex;
  min-height: 100vh;
  font-family: 'DM Sans', sans-serif;
  opacity: 0;
  transform: translateY(8px);
  transition: opacity 0.5s ease, transform 0.5s ease;
}
.login-page.mounted {
  opacity: 1;
  transform: translateY(0);
}

/* ── Left panel ─────────────────────────────────────────── */
.left-panel {
  width: 420px;
  flex-shrink: 0;
  background: #0f2744;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-inner {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 40px 44px;
}

/* Brand */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: auto;
}
.brand-icon {
  display: flex;
}
.brand-name {
  font-family: 'Syne', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.3px;
}

/* Panel content */
.panel-content {
  margin: auto 0;
  padding: 40px 0;
}
.panel-heading {
  font-family: 'Syne', sans-serif;
  font-size: 38px;
  font-weight: 800;
  color: white;
  line-height: 1.1;
  letter-spacing: -1px;
  margin: 0 0 16px;
}
.panel-sub {
  font-size: 14px;
  color: rgba(255,255,255,0.55);
  line-height: 1.65;
  margin: 0 0 36px;
  max-width: 280px;
}
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: rgba(255,255,255,0.65);
}
.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4a9eff;
  flex-shrink: 0;
}

.panel-footer {
  font-size: 12px;
  color: rgba(255,255,255,0.3);
}

/* Decorative circles */
.deco {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.06);
}
.deco-1 {
  width: 380px;
  height: 380px;
  bottom: -120px;
  right: -140px;
}
.deco-2 {
  width: 220px;
  height: 220px;
  bottom: -40px;
  right: -60px;
  background: rgba(74,158,255,0.04);
}

/* ── Right panel ─────────────────────────────────────────── */
.right-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fc;
  padding: 40px 24px;
}

.form-wrapper {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 32px;
}
.form-title {
  font-family: 'Syne', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: #0f2744;
  letter-spacing: -0.5px;
  margin: 0 0 6px;
}
.form-subtitle {
  font-size: 14px;
  color: #8a94a6;
  margin: 0;
}

/* Error */
.error-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fdf2f2;
  border: 1px solid #f5c6c6;
  border-radius: 8px;
  padding: 11px 14px;
  font-size: 13px;
  color: #c0392b;
  margin-bottom: 20px;
}

/* Fields */
.field-group {
  margin-bottom: 18px;
}
.field-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #3d4a5c;
  margin-bottom: 7px;
}
.input-wrap {
  position: relative;
}
.input-icon {
  position: absolute;
  left: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: #a0aab8;
  pointer-events: none;
}
.field-input {
  width: 100%;
  height: 44px;
  padding: 0 42px 0 40px;
  background: white;
  border: 1px solid #dde2eb;
  border-radius: 10px;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  color: #0f2744;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.field-input::placeholder {
  color: #bbc4d0;
}
.field-input:focus {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.12);
}
.field-input:disabled {
  background: #f5f6f8;
  color: #9aa3b0;
}

/* Eye toggle */
.eye-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #a0aab8;
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.15s;
}
.eye-btn:hover { color: #4a9eff; }

/* Submit */
.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 8px;
  background: #0f2744;
  color: white;
  border: none;
  border-radius: 10px;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.submit-btn:hover:not(:disabled) {
  background: #1a3a5c;
}
.submit-btn:active:not(:disabled) {
  transform: scale(0.99);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Loading state */
.btn-loading {
  display: flex;
  align-items: center;
  gap: 8px;
}
.spinner {
  width: 15px;
  height: 15px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Footer note */
.form-note {
  margin-top: 28px;
  font-size: 12px;
  color: #a0aab8;
  text-align: center;
  line-height: 1.6;
}

/* Transition */
.fade-down-enter-active,
.fade-down-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.fade-down-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}
.fade-down-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ── Responsive ─────────────────────────────────────────── */
@media (max-width: 768px) {
  .left-panel { display: none; }
  .right-panel { background: white; }
}
</style>