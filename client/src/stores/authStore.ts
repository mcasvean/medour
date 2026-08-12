import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') as string | null,
    user: null as null | Record<string, unknown>,
    isAuthenticated: !!localStorage.getItem('token')
  })
})
