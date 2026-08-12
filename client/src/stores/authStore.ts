import { defineStore } from 'pinia'

export interface User {
  id: number
  email: string
  firstName: string
  surname: string
  role: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') as string | null,
    user: null as User | null,
    isAuthenticated: !!localStorage.getItem('token')
  }),
  actions: {
    setAuth(token: string, user: User) {
      this.token = token
      this.user = user
      this.isAuthenticated = true
      localStorage.setItem('token', token)
    }
  }
})
