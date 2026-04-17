import { defineStore } from 'pinia'
import { ref } from 'vue'

export type NotificationType = 'success' | 'error' | 'warning' | 'info'

export interface AppNotification {
  id: number
  message: string
  type: NotificationType
}

export const useNotificationStore = defineStore('notifications', () => {
  const notifications = ref<AppNotification[]>([])

  function push(message: string, type: NotificationType = 'info'): void {
    const id = Date.now() + Math.floor(Math.random() * 1000)
    notifications.value.push({ id, message, type })

    window.setTimeout(() => dismiss(id), 3500)
  }

  function dismiss(id: number): void {
    notifications.value = notifications.value.filter((n) => n.id !== id)
  }

  return {
    notifications,
    push,
    dismiss,
  }
})
