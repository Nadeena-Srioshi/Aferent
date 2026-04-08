import { useToast as useVueToast } from 'vue-toastification'

interface ToastOptions {
  timeout?: number
  [key: string]: unknown
}

type ToastType = 'success' | 'error' | 'warning' | 'info'

export function useToast() {
  const toast = useVueToast()

  const success = (message: string, options: ToastOptions = {}) => {
    toast.success(message, { timeout: 3000, ...options })
  }

  const error = (message: string, options: ToastOptions = {}) => {
    toast.error(message, { timeout: 4000, ...options })
  }

  const warning = (message: string, options: ToastOptions = {}) => {
    toast.warning(message, { timeout: 3500, ...options })
  }

  const info = (message: string, options: ToastOptions = {}) => {
    toast.info(message, { timeout: 3000, ...options })
  }

  const showToast = (
    message: string,
    type: ToastType = 'info',
    options: ToastOptions = {}
  ) => {
    toast[type](message, options)
  }

  return { success, error, warning, info, showToast }
}