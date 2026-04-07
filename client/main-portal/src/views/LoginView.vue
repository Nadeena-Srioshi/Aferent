<template>
  <div class="bg-surface flex">
    <div class="w-full flex overflow-hidden">

      <!-- Left panel -->
      <div class="hidden md:flex w-5/12 flex-col justify-between p-10 relative overflow-hidden"
           style="background: #004D51;">
        <!-- Blobs -->
        <div class="absolute -top-14 -right-16 w-56 h-56 rounded-full opacity-20 bg-action"></div>
        <div class="absolute -right-10 bottom-16 w-36 h-36 rounded-full opacity-25 bg-primary"></div>

        <!-- Logo -->
        <RouterLink to="/" class="flex items-center gap-2 relative z-10">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center" style="background:rgba(255,255,255,0.15);">
            <svg width="16" height="16" viewBox="0 0 18 18" fill="none">
              <path d="M9 2C5.13 2 2 5.13 2 9s3.13 7 7 7 7-3.13 7-7-3.13-7-7-7zm0 3a2 2 0 110 4 2 2 0 010-4zm0 9.2a5.01 5.01 0 01-4.2-2.28C4.81 10.34 7.27 9.8 9 9.8c1.72 0 4.18.54 4.2 2.12A5.01 5.01 0 019 14.2z" fill="white"/>
            </svg>
          </div>
          <span class="text-lg font-medium text-white tracking-tight">Aferent</span>
        </RouterLink>

        <!-- Illustration -->
        <LoginIllustration />

        <!-- Tagline -->
        <div class="relative z-10">
          <h2 class="text-xl font-medium text-white leading-snug mb-2">Care that fits<br>your life.</h2>
          <p class="text-xs leading-relaxed" style="color:rgba(255,255,255,0.55);">
            Book appointments, consult doctors, and manage your health — all in one place.
          </p>
        </div>
      </div>

      <!-- Right panel -->
      <div class="flex-1 bg-white flex items-center justify-center p-10">
        <div class="w-full max-w-xs">

          <div class="mb-6">
            <h1 class="text-xl font-medium text-gray-900 mb-1">Welcome back</h1>
            <p class="text-sm text-gray-400">Sign in to your patient account</p>
          </div>

          <form novalidate @submit.prevent="handleSubmit" class="space-y-4">
            <div>
              <label for="email" class="block text-xs font-medium uppercase tracking-wide text-gray-400 mb-1.5">Email</label>
              <input
                id="email"
                v-model="form.email"
                type="email"
                autocomplete="email"
                placeholder="you@example.com"
                required
                class="w-full px-4 py-2.5 rounded-xl border text-sm text-gray-900 placeholder-gray-300 focus:outline-none focus:ring-2 transition-colors"
                style="border-color:#e0e0e0; background:#f8f8f8; --tw-ring-color:rgba(0,96,100,0.15);"
              />
            </div>

            <div>
              <div class="flex items-center justify-between mb-1.5">
                <label for="password" class="block text-xs font-medium uppercase tracking-wide text-gray-400">Password</label>
                <RouterLink to="/forgot-password" class="text-xs font-medium text-primary">Forgot password?</RouterLink>
              </div>
              <input
                id="password"
                v-model="form.password"
                type="password"
                autocomplete="current-password"
                placeholder="••••••••"
                required
                class="w-full px-4 py-2.5 rounded-xl border text-sm text-gray-900 placeholder-gray-300 focus:outline-none focus:ring-2 transition-colors"
                style="border-color:#e0e0e0; background:#f8f8f8; --tw-ring-color:rgba(0,96,100,0.15);"
              />
            </div>

            <p v-if="error" role="alert" class="text-xs px-4 py-2.5 rounded-xl bg-red-50 text-red-500">{{ error }}</p>

            <button
              type="submit"
              :disabled="loading"
              class="w-full py-3 text-white text-sm font-medium rounded-xl transition-colors disabled:opacity-60 disabled:cursor-not-allowed bg-primary hover:bg-action"
            >
              <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2 align-middle"></span>
              {{ loading ? 'Signing in…' : 'Sign in' }}
            </button>
          </form>

          <div class="flex items-center gap-3 my-5">
            <hr class="flex-1 border-t border-gray-100" />
            <span class="text-xs text-gray-300">or continue with</span>
            <hr class="flex-1 border-t border-gray-100" />
          </div>

          <button
            class="w-full py-2.5 flex items-center justify-center gap-2 text-sm text-gray-600 rounded-xl border transition-colors hover:bg-gray-50"
            style="border-color:#e0e0e0; background:#f8f8f8;"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            </svg>
            Sign in with Google
          </button>

          <p class="mt-5 text-center text-xs text-gray-400">
            Don't have an account?
            <RouterLink to="/register" class="font-semibold text-primary">Create one free</RouterLink>
          </p>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { RouterLink } from 'vue-router'
import { useAuth } from '@/stores/useAuth'
import LoginIllustration from '@/components/sections/LoginIllustration.vue'

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