import { defineStore } from 'pinia'

export interface User {
  id: number
  email: string
  firstName: string
  surname: string
  role: string
  mustChangePassword: boolean
  profilePicture: string | null
}

function loadStoredUser(): User | null {
  const raw = localStorage.getItem('auth_user')
  if (!raw) return null
  try {
    // spread ensures new fields exist even if persisted before they were added
    return { mustChangePassword: false, profilePicture: null, ...JSON.parse(raw) } as User
  } catch {
    localStorage.removeItem('auth_user')
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') as string | null,
    user: loadStoredUser()
  }),
  getters: {
    isAuthenticated: (state): boolean => !!state.token
  },
  actions: {
    setAuth(token: string, user: User) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('auth_user', JSON.stringify(user))
    },
    clearAuth() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('auth_user')
    },
    updateUser(updates: Partial<Pick<User, 'firstName' | 'surname' | 'mustChangePassword' | 'profilePicture'>>) {
      if (this.user) {
        this.user = { ...this.user, ...updates }
        localStorage.setItem('auth_user', JSON.stringify(this.user))
      }
    }
  }
})

