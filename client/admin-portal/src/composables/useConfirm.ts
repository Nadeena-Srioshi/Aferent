import { ref } from 'vue'

interface ConfirmData {
  title: string
  message: string
  confirmText: string
  cancelText: string
  type: string
  onConfirm: (() => void) | null
  onCancel: (() => void) | null
}

interface ConfirmOptions {
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  type?: string
}

const isOpen = ref(false)
const confirmData = ref<ConfirmData>({
  title: '',
  message: '',
  confirmText: 'Confirm',
  cancelText: 'Cancel',
  type: 'warning',
  onConfirm: null,
  onCancel: null,
})

export function useConfirm() {
  const confirm = ({
    title = 'Confirm Action',
    message = 'Are you sure you want to proceed?',
    confirmText = 'Confirm',
    cancelText = 'Cancel',
    type = 'warning',
  }: ConfirmOptions = {}): Promise<boolean> => {
    return new Promise((resolve, reject) => {
      confirmData.value = {
        title,
        message,
        confirmText,
        cancelText,
        type,
        onConfirm: () => {
          isOpen.value = false
          resolve(true)
        },
        onCancel: () => {
          isOpen.value = false
          reject(false)
        },
      }
      isOpen.value = true
    })
  }

  const close = () => {
    isOpen.value = false
  }

  return { isOpen, confirmData, confirm, close }
}
