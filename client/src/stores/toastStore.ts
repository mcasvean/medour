import { defineStore } from 'pinia'
import type { AxiosError } from 'axios'

export interface Toast {
  id: number
  message: string
  type: 'error' | 'success' | 'warning'
}

const MAX_TOASTS = 5
const AUTO_DISMISS_MS = 5000
let _nextId = 1

export const useToastStore = defineStore('toast', {
  state: () => ({
    toasts: [] as Toast[]
  }),
  actions: {
    show(message: string, type: Toast['type']) {
      const id = _nextId++
      if (this.toasts.length >= MAX_TOASTS) {
        this.toasts.shift()
      }
      this.toasts.push({ id, message, type })
      setTimeout(() => this.dismiss(id), AUTO_DISMISS_MS)
    },
    showError(error: unknown) {
      const axiosErr = error as AxiosError<Record<string, string>>
      const data = axiosErr?.response?.data
      const message = data?.message ?? data?.error ?? 'An unexpected error occurred'
      this.show(message, 'error')
    },
    dismiss(id: number) {
      this.toasts = this.toasts.filter(t => t.id !== id)
    }
  }
})
