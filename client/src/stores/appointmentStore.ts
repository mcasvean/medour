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
  },
})

