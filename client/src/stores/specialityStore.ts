import { defineStore } from 'pinia'
import api from '../api/index'

export interface Speciality {
  id: number
  name: string
}

export const useSpecialityStore = defineStore('speciality', {
  state: () => ({
    specialities: [] as Speciality[]
  }),
  actions: {
    async fetchSpecialities() {
      const response = await api.get<Speciality[]>('/specialities')
      this.specialities = response.data
    },
    async addSpeciality(name: string) {
      await api.post('/specialities', { name })
      await this.fetchSpecialities()
    },
    async updateSpeciality(id: number, name: string) {
      await api.put(`/specialities/${id}`, { name })
      await this.fetchSpecialities()
    },
    async deleteSpeciality(id: number) {
      await api.delete(`/specialities/${id}`)
      await this.fetchSpecialities()
    }
  }
})
