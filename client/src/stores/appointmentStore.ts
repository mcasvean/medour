import { defineStore } from 'pinia'
import api from '../api/index'

export interface SlotDisplay {
  startTime: string
  endTime: string
  state: 'AVAILABLE' | 'LOCKED' | 'UNAVAILABLE'
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
    reservationId: null as number | null,
    bookingStep: 'searching' as 'searching' | 'confirming' | 'done',
    lockedStartTime: null as string | null,
    errorMessage: '' as string,
    wherebyRoomUrl: null as string | null,
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
  },
})

