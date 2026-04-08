import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { DISPLAY_DATE_FORMAT, DISPLAY_DATETIME_FORMAT } from './constants'

dayjs.extend(relativeTime)

export const formatDate = (date: string | Date | null | undefined, format = DISPLAY_DATE_FORMAT): string => {
  if (!date) return '-'
  return dayjs(date).format(format)
}

export const formatDateTime = (date: string | Date | null | undefined, format = DISPLAY_DATETIME_FORMAT): string => {
  if (!date) return '-'
  return dayjs(date).format(format)
}

export const formatRelativeTime = (date: string | Date | null | undefined): string => {
  if (!date) return '-'
  return dayjs(date).fromNow()
}

export const formatCurrency = (amount: number | null | undefined, currency = 'USD'): string => {
  if (amount === null || amount === undefined) return '-'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount)
}

export const formatNumber = (num: number | null | undefined): string => {
  if (num === null || num === undefined) return '-'
  return new Intl.NumberFormat('en-US').format(num)
}

export const truncate = (text: string | null | undefined, length = 50): string => {
  if (!text) return ''
  if (text.length <= length) return text
  return text.substring(0, length) + '...'
}

export const capitalize = (text: string | null | undefined): string => {
  if (!text) return ''
  return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase()
}

export const getInitials = (name: string | null | undefined): string => {
  if (!name) return '?'
  const parts = name.split(' ')
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase()
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
}

export const getRandomColor = (seed?: string): string => {
  const colors = [
    '#3b82f6', '#a855f7', '#10b981', '#f59e0b',
    '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899',
  ]
  if (!seed) return colors[Math.floor(Math.random() * colors.length)]
  const index = seed.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)
  return colors[index % colors.length]
}

export const debounce = <T extends (...args: unknown[]) => void>(func: T, wait = 300): ((...args: Parameters<T>) => void) => {
  let timeout: ReturnType<typeof setTimeout>
  return function (...args: Parameters<T>) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

export const downloadFile = (data: string, filename: string, type = 'text/csv'): void => {
  const blob = new Blob([data], { type })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export const copyToClipboard = async (text: string): Promise<boolean> => {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch (err) {
    console.error('Failed to copy:', err)
    return false
  }
}

export const isValidEmail = (email: string): boolean => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return regex.test(email)
}

export const getStatusBadgeClass = (status: string): string => {
  const statusMap: Record<string, string> = {
    ACTIVE: 'badge-success',
    INACTIVE: 'badge-gray',
    PENDING: 'badge-warning',
    APPROVED: 'badge-success',
    REJECTED: 'badge-danger',
    COMPLETED: 'badge-success',
    CANCELLED: 'badge-danger',
    SCHEDULED: 'badge-primary',
    REFUNDED: 'badge-warning',
    FAILED: 'badge-danger',
  }
  return statusMap[status] || 'badge-gray'
}

export const calculatePercentageChange = (current: number, previous: number): string => {
  if (!previous || previous === 0) return '0'
  return ((current - previous) / previous * 100).toFixed(1)
}

export const getMonthName = (monthNumber: number): string => {
  const months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ]
  return months[monthNumber - 1] || ''
}

export const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms))
