import { defineStore } from 'pinia'
import { ref } from 'vue'
import { STORAGE_KEYS } from '@/utils/constants'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  function toggleTheme() {
    isDark.value = !isDark.value
    applyTheme()
  }

  function setTheme(dark: boolean) {
    isDark.value = dark
    applyTheme()
  }

  function applyTheme() {
    if (isDark.value) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
    localStorage.setItem(STORAGE_KEYS.THEME, isDark.value ? 'dark' : 'light')
  }

  function initTheme() {
    const savedTheme = localStorage.getItem(STORAGE_KEYS.THEME)

    if (savedTheme) {
      isDark.value = savedTheme === 'dark'
    } else {
      isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
    }

    applyTheme()

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (!localStorage.getItem(STORAGE_KEYS.THEME)) {
        isDark.value = e.matches
        applyTheme()
      }
    })
  }

  return { isDark, toggleTheme, setTheme, initTheme }
})
