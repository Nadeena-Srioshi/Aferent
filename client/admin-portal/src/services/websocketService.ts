import { io, Socket } from 'socket.io-client'
import { WS_URL, STORAGE_KEYS } from '@/utils/constants'

type EventCallback = (data: unknown) => void

class WebSocketService {
  private socket: Socket | null = null
  private listeners: Map<string, EventCallback[]> = new Map()

  connect(): void {
    if (this.socket?.connected) return

    const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)

    this.socket = io(WS_URL, {
      auth: { token },
      transports: ['websocket'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      reconnectionAttempts: 5,
    })

    this.setupEventHandlers()
  }

  private setupEventHandlers(): void {
    if (!this.socket) return

    this.socket.on('connect', () => console.log('WebSocket connected'))
    this.socket.on('disconnect', () => console.log('WebSocket disconnected'))
    this.socket.on('error', (error) => console.error('WebSocket error:', error))

    const events = [
      'payment:created', 'payment:updated', 'payment:refunded',
      'doctor:verification:pending', 'doctor:verification:approved', 'doctor:verification:rejected',
      'doctor:registered',
      'appointment:created', 'appointment:updated',
      'refund:requested',
      'system:health',
    ]

    events.forEach((event) => {
      this.socket!.on(event, (data: unknown) => this.emit(event, data))
    })
  }

  on(event: string, callback: EventCallback): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event)!.push(callback)

    return () => {
      const callbacks = this.listeners.get(event)
      if (callbacks) {
        const index = callbacks.indexOf(callback)
        if (index > -1) callbacks.splice(index, 1)
      }
    }
  }

  private emit(event: string, data: unknown): void {
    const callbacks = this.listeners.get(event) || []
    callbacks.forEach((cb) => cb(data))
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.disconnect()
      this.socket = null
    }
    this.listeners.clear()
  }

  send(event: string, data: unknown): void {
    if (this.socket?.connected) {
      this.socket.emit(event, data)
    }
  }
}

const websocketService = new WebSocketService()
export default websocketService
