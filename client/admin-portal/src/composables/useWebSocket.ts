import { onUnmounted } from 'vue'
import websocketService from '@/services/websocketService'

type EventCallback = (data: unknown) => void

export function useWebSocket() {
  const onPaymentCreated = (callback: EventCallback) => {
    const unsub = websocketService.on('payment:created', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onPaymentUpdated = (callback: EventCallback) => {
    const unsub = websocketService.on('payment:updated', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onPaymentRefunded = (callback: EventCallback) => {
    const unsub = websocketService.on('payment:refunded', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onDoctorVerificationPending = (callback: EventCallback) => {
    const unsub = websocketService.on('doctor:verification:pending', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onDoctorVerificationApproved = (callback: EventCallback) => {
    const unsub = websocketService.on('doctor:verification:approved', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onDoctorVerificationRejected = (callback: EventCallback) => {
    const unsub = websocketService.on('doctor:verification:rejected', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onDoctorRegistered = (callback: EventCallback) => {
    const unsub = websocketService.on('doctor:registered', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onAppointmentCreated = (callback: EventCallback) => {
    const unsub = websocketService.on('appointment:created', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onAppointmentUpdated = (callback: EventCallback) => {
    const unsub = websocketService.on('appointment:updated', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onRefundRequested = (callback: EventCallback) => {
    const unsub = websocketService.on('refund:requested', callback)
    onUnmounted(unsub)
    return unsub
  }

  const onSystemHealth = (callback: EventCallback) => {
    const unsub = websocketService.on('system:health', callback)
    onUnmounted(unsub)
    return unsub
  }

  const send = (event: string, data: unknown) => {
    websocketService.send(event, data)
  }

  return {
    onPaymentCreated,
    onPaymentUpdated,
    onPaymentRefunded,
    onDoctorVerificationPending,
    onDoctorVerificationApproved,
    onDoctorVerificationRejected,
    onDoctorRegistered,
    onAppointmentCreated,
    onAppointmentUpdated,
    onRefundRequested,
    onSystemHealth,
    send,
  }
}
