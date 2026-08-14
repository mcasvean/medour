import { defineStore } from 'pinia'
import api from '../api/index'

export interface SlotDisplay {
  startTime: string
  endTime: string
  state: 'AVAILABLE' | 'LOCKED' | 'UNAVAILABLE'
}

export interface PatientAppointment {
  id: number
  scheduledDate: string
  startTime: string
  doctorFirstName: string
  doctorSurname: string
  doctorSpeciality: string
  doctorRemoved: boolean
  status: string
  createdAt: string
  wherebyRoomUrl: string | null
  ratingValue: number | null
  ratingId: number | null
}

export interface DoctorAppointment {
  id: number
  scheduledDate: string
  startTime: string
  patientFirstName: string
  patientSurname: string
  patientRemoved: boolean
  status: string
  createdAt: string
  wherebyRoomUrl: string | null
}

export interface AdminAppointmentDto {
  id: number
  patientName: string
  doctorName: string
  scheduledDate: string
  startTime: string
  status: string
  wherebyRoomUrl: string | null
}

interface SlotEventPayload {
  doctorId: number
  date: string
  startTime: string
  state: 'AVAILABLE' | 'LOCKED' | 'UNAVAILABLE'
}

export const useAppointmentStore = defineStore('appointment', {
  state: () => ({
    selectedDoctorId: null as number | null,
    selectedDate: '' as string,
    slots: [] as SlotDisplay[],
    _eventSource: null as EventSource | null,
    _appointmentEventSource: null as EventSource | null,
    reservationId: null as number | null,
    bookingStep: 'searching' as 'searching' | 'confirming' | 'done',
    lockedStartTime: null as string | null,
    errorMessage: '' as string,
    wherebyRoomUrl: null as string | null,
    patientAppointments: [] as PatientAppointment[],
    doctorAppointments: [] as DoctorAppointment[],
    adminAppointments: [] as AdminAppointmentDto[],
  }),

  actions: {
    async fetchSlots(doctorId: number, date: string) {
      const response = await api.get<SlotDisplay[]>(`/doctors/${doctorId}/slots`, {
        params: { date },
      })
      this.slots = response.data
    },

    connectSse(doctorId: number, date: string) {
      this.disconnectSse()
      const es = new EventSource('/api/v1/sse/slots')
      this._eventSource = es
      es.onmessage = (event: MessageEvent) => {
        let payload: SlotEventPayload
        try {
          payload = JSON.parse(event.data)
        } catch {
          return
        }
        if (payload.doctorId !== this.selectedDoctorId || payload.date !== this.selectedDate) {
          return
        }
        const slot = this.slots.find(s => s.startTime === payload.startTime)
        if (slot) {
          slot.state = payload.state
        }
      }
    },

    disconnectSse() {
      if (this._eventSource) {
        this._eventSource.close()
        this._eventSource = null
      }
    },

    connectAppointmentSse() {
      this.disconnectAppointmentSse()
      const es = new EventSource('/api/v1/sse/slots')
      this._appointmentEventSource = es
      es.addEventListener('appointment-status', (e: MessageEvent) => {
        try {
          const p = JSON.parse(e.data)
          const appt = this.patientAppointments.find(a => a.id === p.appointmentId)
          if (appt) appt.status = p.newStatus
        } catch {
          // malformed JSON — silently drop
        }
      })
    },

    disconnectAppointmentSse() {
      if (this._appointmentEventSource) {
        this._appointmentEventSource.close()
        this._appointmentEventSource = null
      }
    },

    async lockSlot(startTime: string) {
      if (this.selectedDoctorId === null) return
      const slot = this.slots.find(s => s.startTime === startTime)
      if (!slot) return
      const originalState = slot.state
      slot.state = 'LOCKED'
      this.errorMessage = ''
      try {
        const response = await api.post<{ reservationId: number }>('/slots/reserve', {
          doctorId: this.selectedDoctorId,
          date: this.selectedDate,
          startTime,
        })
        this.reservationId = response.data.reservationId
        this.lockedStartTime = startTime
        this.bookingStep = 'confirming'
      } catch {
        slot.state = originalState
        this.errorMessage = 'Slot already reserved'
      }
    },

    async cancelBooking() {
      if (this.reservationId === null) return
      const rid = this.reservationId
      try {
        await api.delete(`/slots/reserve/${rid}`)
      } catch {
        // best-effort cancel; cleanup always runs regardless
      } finally {
        const slot = this.slots.find(s => s.startTime === this.lockedStartTime)
        if (slot) slot.state = 'AVAILABLE'
        this.reservationId = null
        this.lockedStartTime = null
        this.bookingStep = 'searching'
      }
    },

    async confirmBooking() {
      if (this.reservationId === null) return
      try {
        const response = await api.post<{ id: number, wherebyRoomUrl: string }>('/appointments', { reservationId: this.reservationId })
        this.wherebyRoomUrl = response.data.wherebyRoomUrl
        this.bookingStep = 'done'
        this.reservationId = null
        this.lockedStartTime = null
      } catch {
        this.errorMessage = 'Booking failed. Please try again.'
      }
    },

    async fetchPatientAppointments() {
      const response = await api.get<PatientAppointment[]>('/appointments/my')
      this.patientAppointments = response.data
    },

    async submitRating(appointmentId: number, value: number) {
      const appt = this.patientAppointments.find(a => a.id === appointmentId)
      const response = appt?.ratingId != null
        ? await api.patch<{ id: number; value: number }>(`/ratings/${appt.ratingId}`, { value })
        : await api.post<{ id: number; value: number }>('/ratings', { appointmentId, value })
      if (appt) {
        appt.ratingValue = response.data.value
        appt.ratingId = response.data.id
      }
    },

    async fetchDoctorAppointments() {
      const response = await api.get<DoctorAppointment[]>('/appointments/doctor/my')
      this.doctorAppointments = response.data
    },

    async fetchAdminAppointments() {
      const res = await api.get<AdminAppointmentDto[]>('/admin/appointments')
      this.adminAppointments = res.data
    },

    async deleteAdminAppointment(id: number) {
      await api.delete('/admin/appointments/' + id)
      try {
        await this.fetchAdminAppointments()
      } catch {
        // ignore refresh failure
      }
    },

    async updateDoctorAppointmentStatus(id: number, newStatus: 'CANCELED' | 'COMPLETED') {
      try {
        await api.patch(`/appointments/doctor/${id}/status`, { newStatus })
        const appt = this.doctorAppointments.find(a => a.id === id)
        if (appt) appt.status = newStatus
      } catch {
        this.errorMessage = 'Failed to update appointment status.'
      }
    },
  },
})

export function isJoinActive(scheduledDate: string, startTime: string, status: string): boolean {
  const start = new Date(`${scheduledDate}T${startTime}`)
  const windowStart = new Date(start.getTime() - 10 * 60 * 1000)
  const windowEnd = new Date(start.getTime() + 30 * 60 * 1000)
  const now = new Date()
  return status === 'OPEN' && now >= windowStart && now <= windowEnd
}
