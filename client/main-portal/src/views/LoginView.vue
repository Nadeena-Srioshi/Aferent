<template>
  <div class="min-h-screen bg-surface flex items-center justify-center px-4 py-12">
    <div class="w-full max-w-md">

      <!-- Logo -->
      <RouterLink to="/" class="flex items-center justify-center gap-2 mb-8 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg">
        <div class="w-9 h-9 rounded-xl bg-primary flex items-center justify-center">
          <svg width="20" height="20" viewBox="0 0 18 18" fill="none" aria-hidden="true">
            <path d="M9 2C5.13 2 2 5.13 2 9s3.13 7 7 7 7-3.13 7-7-3.13-7-7-7zm0 3a2 2 0 110 4 2 2 0 010-4zm0 9.2a5.01 5.01 0 01-4.2-2.28C4.81 10.34 7.27 9.8 9 9.8c1.72 0 4.18.54 4.2 2.12A5.01 5.01 0 019 14.2z" fill="white"/>
          </svg>
        </div>
        <span class="text-2xl font-bold text-primary tracking-tight">Aferent</span>
      </RouterLink>

      <!-- Card -->
      <div class="bg-card border border-border rounded-2xl shadow-sm p-8">
        <h1 class="text-2xl font-bold text-ink mb-1">Welcome back</h1>
        <p class="text-muted text-sm mb-7">Sign in to your patient account</p>

        <!-- TODO: Wire up useAuth().login() on submit -->
        <form class="space-y-5" novalidate @submit.prevent="handleSubmit">
          <div>
            <label for="email" class="block text-sm font-medium text-ink mb-1.5">Email</label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              autocomplete="email"
              placeholder="you@example.com"
              required
              class="w-full px-4 py-3 rounded-xl border border-border bg-white text-ink placeholder-muted text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors"
            />
          </div>

          <div>
            <div class="flex items-center justify-between mb-1.5">
              <label for="password" class="block text-sm font-medium text-ink">Password</label>
              <RouterLink to="/forgot-password" class="text-xs text-primary hover:text-action focus-visible:outline-none focus-visible:underline">
                Forgot password?
              </RouterLink>
            </div>
            <input
              id="password"
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••"
              required
              class="w-full px-4 py-3 rounded-xl border border-border bg-white text-ink placeholder-muted text-sm focus:outline-none focus:ring-2 focus:ring-primary/40 focus:border-primary transition-colors"
            />
          </div>

          <!-- TODO: show auth.error here if login fails -->
          <p v-if="error" role="alert" class="text-xs text-danger bg-danger/10 px-4 py-2 rounded-xl">{{ error }}</p>

          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3.5 bg-primary text-white font-semibold rounded-xl hover:bg-action transition-colors duration-150 shadow-sm disabled:opacity-60 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
          >
            <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle" aria-hidden="true"></span>
            {{ loading ? 'Signing in…' : 'Sign In' }}
          </button>
        </form>

        <p class="mt-6 text-center text-sm text-muted">
          Don't have an account?
          <RouterLink to="/register" class="text-primary font-semibold hover:text-action focus-visible:outline-none focus-visible:underline">
            Create one free
          </RouterLink>
        </p>
      </div>

    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { RouterLink } from 'vue-router'
import { useAuth } from '@/stores/useAuth'

const form    = reactive({ email: '', password: '' })
const loading = ref(false)
const error   = ref(null)
const router  = useRouter()
const auth    = useAuth()

async function handleSubmit() {
  error.value = null
  loading.value = true
  try {
    await auth.login({ email: form.email, password: form.password })
    router.push('/')
  } catch (e) {
    error.value = e?.message || 'Login failed. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>