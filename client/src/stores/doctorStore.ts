import { defineStore } from 'pinia'
import api from '../api'

export interface DoctorSearchResult {
  id: number
  firstName: string
  surname: string
  speciality: string
  county: string
  city: string
  averageRating: number | null
}

export interface DoctorFilters {
  speciality: string
  county: string
  city: string
  date: string
}

export const useDoctorStore = defineStore('doctor', {
  state: () => ({
    doctors: [] as DoctorSearchResult[],
    loading: false,
    filters: {
      speciality: '',
      county: '',
      city: '',
      date: ''
    } as DoctorFilters
  }),
  actions: {
    async searchDoctors() {
      this.loading = true
      try {
        const params: Record<string, string> = {}
        if (this.filters.speciality) params['speciality'] = this.filters.speciality
        if (this.filters.county) params['county'] = this.filters.county
        if (this.filters.city) params['city'] = this.filters.city
        if (this.filters.date) params['date'] = this.filters.date
        const response = await api.get<DoctorSearchResult[]>('/doctors/', { params })
        this.doctors = response.data
      } finally {
        this.loading = false
      }
    }
  }
})
