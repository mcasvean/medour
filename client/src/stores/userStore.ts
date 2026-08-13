import { defineStore } from 'pinia'
import api from '../api/index'

export interface AdminUser {
  id: number
  email: string
  firstName: string
  surname: string
  role: string
  speciality: string | null
  county: string | null
  city: string | null
  age: number | null
  gender: string | null
  address: string | null
  mustChangePassword: boolean
  isDeleted: boolean
}

export const useUserStore = defineStore('user', {
  state: () => ({
    adminUsers: [] as AdminUser[]
  }),
  actions: {
    async fetchAdminUsers() {
      const response = await api.get<AdminUser[]>('/admin/users')
      this.adminUsers = response.data
    },
    async createAdminUser(data: Omit<AdminUser, 'id' | 'isDeleted'> & { password: string }) {
      await api.post('/admin/users', data)
      await this.fetchAdminUsers()
    },
    async updateAdminUser(id: number, data: Partial<AdminUser>) {
      await api.put(`/admin/users/${id}`, data)
      await this.fetchAdminUsers()
    },
    async deleteAdminUser(id: number) {
      await api.delete(`/admin/users/${id}`)
      // refresh is best-effort; a refresh failure doesn't mean the delete failed
      try { await this.fetchAdminUsers() } catch { /* ignored */ }
    }
  }
})
