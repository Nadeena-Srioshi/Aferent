<template>
  <div class="bg-surface flex">
    <div class="w-full flex overflow-hidden">

      <!-- Left panel -->
      <div class="hidden md:flex w-5/12 flex-col justify-between p-12 relative overflow-hidden"
           style="background: #004D51;">
        <!-- Blobs -->
        <div class="absolute -top-14 -right-16 w-56 h-56 rounded-full opacity-20 bg-action"></div>
        <div class="absolute -right-10 bottom-16 w-36 h-36 rounded-full opacity-25 bg-primary"></div>

        <!-- Logo -->
        <RouterLink to="/" class="flex items-center gap-2 relative z-10">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center" style="background:rgba(255,255,255,0.15);">
            <HeartPulse class="w-4 h-4 text-white" aria-hidden="true" />
          </div>
          <span class="text-lg font-medium text-white tracking-tight">Aferent</span>
        </RouterLink>

        <!-- Illustration -->
        <LoginIllustration />

        <!-- Tagline -->
        <div class="relative z-10">
          <h2 class="text-xl font-medium text-white leading-snug mb-2">Care that fits your life.</h2>
          <p class="text-xs leading-relaxed" style="color:rgba(255,255,255,0.55);">
            Book appointments, consult doctors, and manage your health.
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
            <Chrome class="w-4 h-4 text-muted" aria-hidden="true" />
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
import { useRoute, useRouter } from 'vue-router'
import { RouterLink } from 'vue-router'
import { useAuth } from '@/stores/useAuth'
import LoginIllustration from '@/components/sections/LoginIllustration.vue'
import { Chrome, HeartPulse } from 'lucide-vue-next'

const form    = reactive({ email: '', password: '' })
const loading = ref(false)
const error   = ref(null)
const router  = useRouter()
const route   = useRoute()
const auth    = useAuth()

function normalizeRole(role) {
  return typeof role === 'string' ? role.trim().toUpperCase() : ''
}

async function handleSubmit() {
  error.value = null
  loading.value = true
  try {
    const user = await auth.login({ email: form.email, password: form.password })
    const role = normalizeRole(user?.role)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''

    if (role === 'DOCTOR') {
      await router.push('/doctor/dashboard')
      return
    }

    if (redirect.startsWith('/') && !redirect.startsWith('/doctor')) {
      await router.push(redirect)
      return
    }

    await router.push('/')
  } catch (e) {
    error.value = e?.message || 'Login failed. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>