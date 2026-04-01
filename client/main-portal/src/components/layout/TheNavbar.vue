<template>
  <header
    class="sticky top-0 z-50 bg-white border-b border-border transition-shadow duration-200"
    :class="{ 'shadow-md': scrolled }"
    role="banner"
  >
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16">

        <!-- ── Logo ────────────────────────────────────────── -->
        <RouterLink
          to="/"
          class="flex items-center gap-2 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          aria-label="Aferent — go to homepage"
        >
          <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
              <path d="M9 2C5.13 2 2 5.13 2 9s3.13 7 7 7 7-3.13 7-7-3.13-7-7-7zm0 3a2 2 0 110 4 2 2 0 010-4zm0 9.2a5.01 5.01 0 01-4.2-2.28C4.81 10.34 7.27 9.8 9 9.8c1.72 0 4.18.54 4.2 2.12A5.01 5.01 0 019 14.2z" fill="white"/>
            </svg>
          </div>
          <span class="text-xl font-bold text-primary tracking-tight">Aferent</span>
        </RouterLink>

        <!-- ── Desktop nav ──────────────────────────────────── -->
        <nav class="hidden md:flex items-center gap-1" aria-label="Main navigation">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="px-4 py-2 rounded-lg text-sm font-medium text-muted hover:text-ink hover:bg-surface transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            active-class="text-primary bg-primary/10 font-semibold"
          >
            {{ link.label }}
          </RouterLink>
        </nav>

        <!-- ── Desktop right side ───────────────────────────── -->
        <div class="hidden md:flex items-center gap-3">

          <!-- GUEST: Sign In + Get Started -->
          <template v-if="!isAuthenticated">
            <RouterLink
              to="/login"
              class="px-4 py-2 text-sm font-semibold text-primary hover:bg-primary/10 rounded-xl transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              Sign In
            </RouterLink>
            <RouterLink
              to="/register"
              class="px-5 py-2.5 text-sm font-semibold text-white bg-primary hover:bg-action rounded-xl shadow-sm hover:shadow-md transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
            >
              Get Started
            </RouterLink>
          </template>

          <!-- AUTHENTICATED: Notification bell + Profile dropdown -->
          <template v-else>

            <!-- Notification bell -->
            <button
              class="relative p-2 rounded-xl text-muted hover:text-ink hover:bg-surface transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              aria-label="Notifications"
              @click="notifOpen = !notifOpen"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
              </svg>
              <!-- Unread badge — bind to real unread count when notification system is built -->
              <span
                class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-danger border-2 border-white"
                aria-hidden="true"
              ></span>
            </button>

            <!-- Profile dropdown trigger -->
            <div class="relative" ref="profileRef">
              <button
                class="flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-xl hover:bg-surface transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                :aria-expanded="profileOpen"
                aria-haspopup="true"
                aria-controls="profile-menu"
                @click="profileOpen = !profileOpen"
              >
                <!-- Avatar initials -->
                <div
                  class="w-8 h-8 rounded-full bg-primary text-white text-sm font-bold flex items-center justify-center flex-shrink-0 select-none"
                  aria-hidden="true"
                >
                  {{ initials }}
                </div>
                <span class="text-sm font-semibold text-ink max-w-[120px] truncate">{{ firstName }}</span>
                <svg
                  class="w-4 h-4 text-muted transition-transform duration-200"
                  :class="{ 'rotate-180': profileOpen }"
                  fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
              </button>

              <!-- Dropdown menu -->
              <Transition
                enter-active-class="transition duration-150 ease-out"
                enter-from-class="opacity-0 scale-95 -translate-y-1"
                enter-to-class="opacity-100 scale-100 translate-y-0"
                leave-active-class="transition duration-100 ease-in"
                leave-from-class="opacity-100 scale-100 translate-y-0"
                leave-to-class="opacity-0 scale-95 -translate-y-1"
              >
                <div
                  v-if="profileOpen"
                  id="profile-menu"
                  role="menu"
                  aria-label="Profile menu"
                  class="absolute right-0 top-full mt-2 w-56 bg-card border border-border rounded-2xl shadow-lg overflow-hidden origin-top-right"
                >
                  <!-- User info header -->
                  <div class="px-4 py-3 border-b border-border bg-surface">
                    <p class="text-sm font-bold text-ink truncate">{{ fullName }}</p>
                    <p class="text-xs text-muted truncate">{{ userEmail }}</p>
                  </div>

                  <!-- Menu items -->
                  <div class="py-1.5">
                    <RouterLink
                      v-for="item in profileMenuItems"
                      :key="item.to"
                      :to="item.to"
                      role="menuitem"
                      class="flex items-center gap-3 px-4 py-2.5 text-sm text-ink hover:bg-surface transition-colors focus-visible:outline-none focus-visible:bg-surface"
                      @click="profileOpen = false"
                    >
                      <svg class="w-4 h-4 text-muted flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon"/>
                      </svg>
                      {{ item.label }}
                    </RouterLink>
                  </div>

                  <!-- Sign out -->
                  <div class="border-t border-border py-1.5">
                    <button
                      role="menuitem"
                      class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-danger hover:bg-danger/5 transition-colors focus-visible:outline-none focus-visible:bg-danger/5"
                      @click="handleLogout"
                    >
                      <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                      </svg>
                      Sign Out
                    </button>
                  </div>
                </div>
              </Transition>
            </div>

          </template>
        </div>

        <!-- ── Mobile toggle ────────────────────────────────── -->
        <button
          class="md:hidden p-2 rounded-lg text-muted hover:text-ink hover:bg-surface transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          :aria-expanded="mobileOpen"
          aria-controls="mobile-menu"
          aria-label="Toggle navigation menu"
          @click="mobileOpen = !mobileOpen"
        >
          <svg v-if="!mobileOpen" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
          </svg>
          <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>

      </div>
    </div>

    <!-- ── Mobile menu ──────────────────────────────────────── -->
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div
        v-if="mobileOpen"
        id="mobile-menu"
        class="md:hidden border-t border-border bg-white px-4 pb-5 pt-2"
      >
        <!-- Nav links -->
        <nav class="flex flex-col gap-1 mb-4" aria-label="Mobile navigation">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="px-4 py-3 rounded-xl text-sm font-medium text-muted hover:text-ink hover:bg-surface transition-colors"
            active-class="text-primary bg-primary/10 font-semibold"
            @click="mobileOpen = false"
          >
            {{ link.label }}
          </RouterLink>
        </nav>

        <!-- GUEST mobile actions -->
        <template v-if="!isAuthenticated">
          <div class="flex flex-col gap-2 pt-4 border-t border-border">
            <RouterLink
              to="/login"
              class="w-full text-center py-3 text-sm font-semibold text-primary border-2 border-primary rounded-xl hover:bg-primary/10 transition-colors"
              @click="mobileOpen = false"
            >
              Sign In
            </RouterLink>
            <RouterLink
              to="/register"
              class="w-full text-center py-3 text-sm font-semibold text-white bg-primary rounded-xl hover:bg-action transition-colors shadow-sm"
              @click="mobileOpen = false"
            >
              Get Started Free
            </RouterLink>
          </div>
        </template>

        <!-- AUTHENTICATED mobile actions -->
        <template v-else>
          <div class="pt-4 border-t border-border">
            <!-- User info strip -->
            <div class="flex items-center gap-3 px-3 py-3 bg-surface rounded-xl mb-3">
              <div class="w-10 h-10 rounded-full bg-primary text-white text-sm font-bold flex items-center justify-center flex-shrink-0 select-none" aria-hidden="true">
                {{ initials }}
              </div>
              <div class="min-w-0">
                <p class="text-sm font-bold text-ink truncate">{{ fullName }}</p>
                <p class="text-xs text-muted truncate">{{ userEmail }}</p>
              </div>
            </div>

            <!-- Profile links -->
            <div class="flex flex-col gap-1 mb-3">
              <RouterLink
                v-for="item in profileMenuItems"
                :key="item.to"
                :to="item.to"
                class="flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm text-ink hover:bg-surface transition-colors"
                @click="mobileOpen = false"
              >
                <svg class="w-4 h-4 text-muted flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon"/>
                </svg>
                {{ item.label }}
              </RouterLink>
            </div>

            <!-- Sign out -->
            <button
              class="w-full flex items-center justify-center gap-2 py-3 text-sm font-semibold text-danger border-2 border-danger/20 rounded-xl hover:bg-danger/5 transition-colors"
              @click="handleLogout"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
              </svg>
              Sign Out
            </button>
          </div>
        </template>

      </div>
    </Transition>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuth } from '@/stores/useAuth'
import { useNotificationStore } from '@/stores/notificationStore'

const router   = useRouter()
const auth     = useAuth()
const notify   = useNotificationStore()

// ── Auth state ────────────────────────────────────────────────
const { isAuthenticated, user } = storeToRefs(auth)

const fullName  = computed(() => user.value?.name ?? '')
const firstName = computed(() => fullName.value.split(' ')[0] || 'Account')
const userEmail = computed(() => user.value?.email ?? '')
const initials  = computed(() =>
  fullName.value
    .split(' ')
    .map(n => n[0])
    .slice(0, 2)
    .join('')
    .toUpperCase() || '?'
)

// ── UI state ──────────────────────────────────────────────────
const mobileOpen  = ref(false)
const profileOpen = ref(false)
const notifOpen   = ref(false)
const scrolled    = ref(false)
const profileRef  = ref(null)

// ── Nav links ─────────────────────────────────────────────────
const navLinks = [
  { label: 'Find a Doctor',  to: '/find-doctor' },
  { label: 'Specialties',    to: '/specialties' },
  { label: 'Telemedicine',   to: '/telemedicine' },
  { label: 'AI Health Tools',to: '/ai-tools' },
  { label: 'About',          to: '/about' },
]

// ── Profile dropdown items ────────────────────────────────────
const profileMenuItems = [
  { label: 'My Dashboard',    to: '/dashboard',    icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
  { label: 'My Appointments', to: '/appointments', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
  { label: 'Medical Records', to: '/records',      icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
  { label: 'AI Health Tools', to: '/ai-tools',     icon: 'M13 10V3L4 14h7v7l9-11h-7z' },
  { label: 'Account Settings',to: '/settings',     icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z' },
]

// ── Actions ───────────────────────────────────────────────────
async function handleLogout() {
  profileOpen.value = false
  mobileOpen.value  = false
  await auth.logout()
  notify.push('You have been signed out.', 'info')
  router.push('/')
}

// ── Click outside to close profile dropdown ───────────────────
function handleClickOutside(e) {
  if (profileRef.value && !profileRef.value.contains(e.target)) {
    profileOpen.value = false
  }
}

// ── Scroll shadow ─────────────────────────────────────────────
const handleScroll = () => { scrolled.value = window.scrollY > 8 }

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  document.addEventListener('mousedown', handleClickOutside)
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('mousedown', handleClickOutside)
})
</script>