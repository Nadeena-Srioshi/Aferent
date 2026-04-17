<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const sidebarCollapsed = ref(false)
const loggingOut = ref(false)

const navItems = [
  {
    name: 'Dashboard',
    path: '/dashboard',
    icon: `<path d="M3 9.5L9 3.5L15 9.5V16.5H11V12.5H7V16.5H3V9.5Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" fill="none"/>`,
  },
  {
    name: 'Patients',
    path: '/patients',
    icon: `<circle cx="9" cy="6" r="3" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M2 18C2 14.686 5.134 12 9 12C12.866 12 16 14.686 16 18" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" fill="none"/>`,
  },
  {
    name: 'Doctors',
    path: '/doctors',
    icon: `<rect x="3" y="3" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M6 9H12M9 6V12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>`,
  },
  {
    name: 'Appointments',
    path: '/appointments',
    icon: `<rect x="3" y="4" width="12" height="13" rx="1.5" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M7 2V5M11 2V5M3 8H15" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>`,
  },
  {
    name: 'Payments',
    path: '/payments',
    icon: `<rect x="2" y="5" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M2 9H16" stroke="currentColor" stroke-width="1.4"/><circle cx="6" cy="12" r="1" fill="currentColor"/>`,
  },
  {
    name: 'Notifications',
    path: '/notifications',
    icon: `<path d="M9 2C9 2 5 4 5 9V13L3 15H15L13 13V9C13 4 9 2 9 2Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" fill="none"/><path d="M7 15C7 16.1 7.9 17 9 17C10.1 17 11 16.1 11 15" stroke="currentColor" stroke-width="1.4" fill="none"/>`,
  },
  {
    name: 'Hospitals',
    path: '/hospitals',
    icon: `<rect x="3" y="4" width="12" height="12" rx="1.5" stroke="currentColor" stroke-width="1.4" fill="none"/><path d="M9 7V13M6 10H12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>`,
  },
  {
    name: 'Specializations',
    path: '/specializations',
    icon: `<path d="M9 2L11 7H16L12 10L13 15L9 12L5 15L6 10L2 7H7L9 2Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" fill="none"/>`,
  },
]

const currentLabel = computed(() => {
  const match = navItems.find((n) => route.path.startsWith(n.path))
  return match?.name ?? 'Admin Portal'
})

function isActive(path: string) {
  return route.path.startsWith(path)
}

async function handleLogout() {
  loggingOut.value = true
  try {
    await authApi.logout()
  } catch {
    // swallow — we log out locally regardless
  } finally {
    auth.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="shell" :class="{ collapsed: sidebarCollapsed }">

    <!-- ── Sidebar ── -->
    <aside class="sidebar">
      <!-- Brand -->
      <div class="brand">
        <div class="brand-icon">
          <svg width="22" height="22" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="white" fill-opacity="0.15"/>
            <path d="M14 6C14 6 8 10 8 15.5C8 18.537 10.686 21 14 21C17.314 21 20 18.537 20 15.5C20 10 14 6 14 6Z" fill="white"/>
            <path d="M14 21V13M11 16H17" stroke="#0f2744" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <Transition name="label-fade">
          <span v-if="!sidebarCollapsed" class="brand-name">Aferent</span>
        </Transition>
      </div>

      <!-- Nav -->
      <nav class="nav">
        <Transition name="label-fade">
          <p v-if="!sidebarCollapsed" class="nav-section-label">Main menu</p>
        </Transition>

        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :title="sidebarCollapsed ? item.name : ''"
        >
          <span class="nav-icon">
            <svg width="18" height="18" viewBox="0 0 18 18" v-html="item.icon" />
          </span>
          <Transition name="label-fade">
            <span v-if="!sidebarCollapsed" class="nav-label">{{ item.name }}</span>
          </Transition>
          <span v-if="isActive(item.path) && !sidebarCollapsed" class="active-dot"></span>
        </RouterLink>
      </nav>

      <!-- Sidebar footer -->
      <div class="sidebar-footer">
        <!-- Admin badge -->
        <div v-if="!sidebarCollapsed" class="admin-badge">
          <div class="admin-avatar">A</div>
          <div class="admin-info">
            <span class="admin-label">Administrator</span>
            <span class="admin-role">ADMIN</span>
          </div>
        </div>

        <!-- Collapse toggle -->
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" :style="{ transform: sidebarCollapsed ? 'rotate(180deg)' : 'none', transition: 'transform 0.3s' }">
            <path d="M10 3L5 8L10 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>

        <!-- Logout -->
        <button class="logout-btn" @click="handleLogout" :disabled="loggingOut" :title="sidebarCollapsed ? 'Sign out' : ''">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 2H3C2.448 2 2 2.448 2 3V13C2 13.552 2.448 14 3 14H6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
            <path d="M11 5L14 8L11 11M14 8H6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <Transition name="label-fade">
            <span v-if="!sidebarCollapsed">{{ loggingOut ? 'Signing out…' : 'Sign out' }}</span>
          </Transition>
        </button>
      </div>
    </aside>

    <!-- ── Main area ── -->
    <div class="main">

      <!-- Top bar -->
      <header class="topbar">
        <div class="topbar-left">
          <h1 class="page-title">{{ currentLabel }}</h1>
        </div>
        <div class="topbar-right">
          <div class="topbar-time">
            {{ new Date().toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }) }}
          </div>
          <div class="topbar-role-pill">ADMIN</div>
        </div>
      </header>

      <!-- Page content -->
      <main class="content">
        <RouterView />
      </main>
    </div>

  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@600;700;800&family=DM+Sans:wght@300;400;500&display=swap');

/* ── CSS variables ───────────────────────────────────────── */
.shell {
  --sidebar-w: 232px;
  --sidebar-w-collapsed: 68px;
  --navy: #0f2744;
  --navy-light: #1a3a5c;
  --accent: #4a9eff;
  --accent-dim: rgba(74,158,255,0.12);
  --text-muted: rgba(255,255,255,0.45);
  --text-nav: rgba(255,255,255,0.7);
  --bg: #f4f5f7;
  --topbar-h: 62px;

  display: flex;
  min-height: 100vh;
  font-family: 'DM Sans', sans-serif;
  background: var(--bg);
}

/* ── Sidebar ─────────────────────────────────────────────── */
.sidebar {
  width: var(--sidebar-w);
  flex-shrink: 0;
  background: var(--navy);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  overflow: hidden;
  transition: width 0.28s cubic-bezier(0.4,0,0.2,1);
  z-index: 100;
}
.shell.collapsed .sidebar {
  width: var(--sidebar-w-collapsed);
}

/* Brand */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 22px 20px 18px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  white-space: nowrap;
  overflow: hidden;
}
.brand-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
}
.brand-name {
  font-family: 'Syne', sans-serif;
  font-size: 17px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.2px;
}

/* Nav */
.nav {
  flex: 1;
  padding: 16px 10px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
}
.nav::-webkit-scrollbar { display: none; }

.nav-section-label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--text-muted);
  text-transform: uppercase;
  padding: 0 10px 10px;
  white-space: nowrap;
  overflow: hidden;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 8px;
  text-decoration: none;
  color: var(--text-nav);
  font-size: 14px;
  font-weight: 400;
  margin-bottom: 2px;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
  overflow: hidden;
  position: relative;
}
.nav-item:hover {
  background: rgba(255,255,255,0.06);
  color: white;
}
.nav-item.active {
  background: var(--accent-dim);
  color: white;
  font-weight: 500;
}
.nav-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-label {
  flex: 1;
}
.active-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent);
  flex-shrink: 0;
}

/* Sidebar footer */
.sidebar-footer {
  padding: 12px 10px 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.admin-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
}
.admin-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent);
  color: white;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.admin-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.admin-label {
  font-size: 13px;
  color: white;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.admin-role {
  font-size: 10px;
  color: var(--accent);
  letter-spacing: 0.06em;
  font-weight: 500;
}

.collapse-btn,
.logout-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  background: none;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-nav);
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  transition: background 0.15s, color 0.15s;
  text-align: left;
}
.collapse-btn:hover,
.logout-btn:hover:not(:disabled) {
  background: rgba(255,255,255,0.06);
  color: white;
}
.logout-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── Main area ───────────────────────────────────────────── */
.main {
  flex: 1;
  margin-left: var(--sidebar-w);
  transition: margin-left 0.28s cubic-bezier(0.4,0,0.2,1);
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.shell.collapsed .main {
  margin-left: var(--sidebar-w-collapsed);
}

/* Top bar */
.topbar {
  height: var(--topbar-h);
  background: white;
  border-bottom: 1px solid #eaecf0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 50;
}
.topbar-left {}
.page-title {
  font-family: 'Syne', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #0f2744;
  letter-spacing: -0.3px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.topbar-time {
  font-size: 13px;
  color: #8a94a6;
}
.topbar-role-pill {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #4a9eff;
  background: rgba(74,158,255,0.1);
  border: 1px solid rgba(74,158,255,0.2);
  border-radius: 20px;
  padding: 3px 10px;
}

/* Page content */
.content {
  flex: 1;
  padding: 28px;
}

/* ── Transitions ─────────────────────────────────────────── */
.label-fade-enter-active {
  transition: opacity 0.15s ease 0.1s;
}
.label-fade-leave-active {
  transition: opacity 0.08s ease;
}
.label-fade-enter-from,
.label-fade-leave-to {
  opacity: 0;
}

/* ── Responsive ─────────────────────────────────────────── */
@media (max-width: 768px) {
  .shell {
    --sidebar-w: 0px;
  }
  .sidebar {
    display: none;
  }
  .main {
    margin-left: 0;
  }
  .content {
    padding: 16px;
  }
}
</style>