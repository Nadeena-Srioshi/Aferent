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
          <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center shrink-0">
            <HeartPulse class="w-4.5 h-4.5 text-white" aria-hidden="true" />
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
              <Bell class="w-5 h-5" aria-hidden="true" />
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
                  class="w-8 h-8 rounded-full bg-primary text-white text-sm font-bold flex items-center justify-center shrink-0 select-none"
                  aria-hidden="true"
                >
                  {{ initials }}
                </div>
                <span class="text-sm font-semibold text-ink max-w-30 truncate">{{ firstName }}</span>
                <ChevronDown
                  class="w-4 h-4 text-muted transition-transform duration-200"
                  :class="{ 'rotate-180': profileOpen }"
                  aria-hidden="true"
                />
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
                      <component :is="item.icon" class="w-4 h-4 text-muted shrink-0" aria-hidden="true" />
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
                      <LogOut class="w-4 h-4 shrink-0" aria-hidden="true" />
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
          <Menu v-if="!mobileOpen" class="w-6 h-6" aria-hidden="true" />
          <X v-else class="w-6 h-6" aria-hidden="true" />
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
              <div class="w-10 h-10 rounded-full bg-primary text-white text-sm font-bold flex items-center justify-center shrink-0 select-none" aria-hidden="true">
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
                <component :is="item.icon" class="w-4 h-4 text-muted shrink-0" aria-hidden="true" />
                {{ item.label }}
              </RouterLink>
            </div>

            <!-- Sign out -->
            <button
              class="w-full flex items-center justify-center gap-2 py-3 text-sm font-semibold text-danger border-2 border-danger/20 rounded-xl hover:bg-danger/5 transition-colors"
              @click="handleLogout"
            >
              <LogOut class="w-4 h-4" aria-hidden="true" />
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
import {
  Bell,
  CalendarDays,
  ChevronDown,
  FileText,
  HeartPulse,
  LogOut,
  Menu,
  Settings,
  Sparkles,
  Stethoscope,
  UserRound,
  X,
} from 'lucide-vue-next'

const router   = useRouter()
const auth     = useAuth()
const notify   = useNotificationStore()

// ── Auth state ────────────────────────────────────────────────
const { isAuthenticated, isDoctor, user } = storeToRefs(auth)

const fullName  = computed(() => user.value?.name ?? auth.fullName ?? '')
const firstName = computed(() => fullName.value.split(' ')[0] || 'Account')
const userEmail = computed(() => user.value?.email ?? '')
const initials  = computed(() =>
  (fullName.value || auth.fullName || '')
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
const navLinks = computed(() => (
  isAuthenticated.value
    ? (isDoctor.value
      ? [
          { label: 'Dashboard',    to: '/doctor/dashboard' },
          { label: 'Appointments', to: '/appointments' },
          { label: 'Profile',      to: '/doctor/profile' },
        ]
      : [
          { label: 'Find a Doctor',   to: '/find-doctor' },
          { label: 'Appointments',    to: '/appointments' },
          { label: 'Medical History', to: '/medical-history' },
          { label: 'Medical Records', to: '/records' },
        ])
    : [
        { label: 'Find a Doctor', to: '/find-doctor' },
        { label: 'About',         to: '/about' },
      ]
))

// ── Profile dropdown items ────────────────────────────────────
const profileMenuItems = computed(() => (
  isDoctor.value
    ? [
        { label: 'Doctor Dashboard', to: '/doctor/dashboard', icon: Stethoscope },
        { label: 'My Profile',       to: '/doctor/profile',   icon: UserRound },
        { label: 'Appointments',     to: '/appointments',     icon: CalendarDays },
      ]
    : [
        { label: 'My Profile',       to: '/profile',          icon: UserRound },
        { label: 'My Appointments',  to: '/appointments',     icon: CalendarDays },
        { label: 'Medical Records',  to: '/records',          icon: FileText },
        { label: 'AI Health Tools',  to: '/ai-tools',         icon: Sparkles },
        { label: 'Account Settings', to: '/settings',         icon: Settings },
      ]
))

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