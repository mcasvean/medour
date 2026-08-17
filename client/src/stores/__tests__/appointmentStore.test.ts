import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAppointmentStore, isJoinActive } from '../appointmentStore'
import type { SlotDisplay } from '../appointmentStore'

vi.mock('../../api/index', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  }
}))

import api from '../../api/index'

const mockSlots: SlotDisplay[] = [
  { startTime: '08:00', endTime: '08:30', state: 'AVAILABLE' },
  { startTime: '10:00', endTime: '10:30', state: 'AVAILABLE' },
]

// Minimal EventSource mock that captures the onmessage handler and named listeners
function makeMockEventSource() {
  const listeners: Record<string, ((e: MessageEvent) => void)[]> = {}
  const es = {
    close: vi.fn(),
    onmessage: null as ((e: MessageEvent) => void) | null,
    addEventListener: vi.fn((event: string, handler: (e: MessageEvent) => void) => {
      if (!listeners[event]) listeners[event] = []
      listeners[event].push(handler)
    }),
    _trigger: (event: string, data: string) => {
      listeners[event]?.forEach(h => h({ data } as MessageEvent))
    },
  }
  return es
}

describe('appointmentStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchSlots sets slots from API response', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: mockSlots })
    const store = useAppointmentStore()

    await store.fetchSlots(1, '2026-09-01')

    expect(api.get).toHaveBeenCalledWith('/doctors/1/slots', { params: { date: '2026-09-01' } })
    expect(store.slots).toHaveLength(2)
    expect(store.slots[0].startTime).toBe('08:00')
  })

  it('SSE onmessage updates matching slot state', () => {
    const store = useAppointmentStore()
    store.selectedDoctorId = 1
    store.selectedDate = '2026-09-01'
    store.slots = [...mockSlots]

    const mockEs = makeMockEventSource()
    const origEventSource = globalThis.EventSource
    globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

    store.connectSse(1, '2026-09-01')

    // trigger the captured onmessage handler
    const payload = { doctorId: 1, date: '2026-09-01', startTime: '10:00', state: 'LOCKED' as const }
    mockEs.onmessage!({ data: JSON.stringify(payload) } as MessageEvent)

    expect(store.slots.find(s => s.startTime === '10:00')?.state).toBe('LOCKED')
    expect(store.slots.find(s => s.startTime === '08:00')?.state).toBe('AVAILABLE')

    globalThis.EventSource = origEventSource
  })

  it('SSE onmessage ignores events for a different doctor', () => {
    const store = useAppointmentStore()
    store.selectedDoctorId = 1
    store.selectedDate = '2026-09-01'
    store.slots = [...mockSlots]

    const mockEs = makeMockEventSource()
    const origEventSource = globalThis.EventSource
    globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

    store.connectSse(1, '2026-09-01')

    const payload = { doctorId: 99, date: '2026-09-01', startTime: '08:00', state: 'LOCKED' as const }
    mockEs.onmessage!({ data: JSON.stringify(payload) } as MessageEvent)

    expect(store.slots[0].state).toBe('AVAILABLE')

    globalThis.EventSource = origEventSource
  })

  it('disconnectSse closes and nulls the EventSource', () => {
    const store = useAppointmentStore()
    const mockEs = makeMockEventSource()
    const origEventSource = globalThis.EventSource
    globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

    store.connectSse(1, '2026-09-01')
    store.disconnectSse()

    expect(mockEs.close).toHaveBeenCalledOnce()
    expect(store._eventSource).toBeNull()

    globalThis.EventSource = origEventSource
  })

  it('lockSlot success sets reservationId and bookingStep=confirming', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { reservationId: 42 } })
    const store = useAppointmentStore()
    store.selectedDoctorId = 1
    store.selectedDate = '2026-09-01'
    store.slots = mockSlots.map(s => ({ ...s }))

    await store.lockSlot('08:00')

    expect(store.reservationId).toBe(42)
    expect(store.bookingStep).toBe('confirming')
    expect(store.lockedStartTime).toBe('08:00')
    expect(store.slots[0].state).toBe('LOCKED')
  })

  it('lockSlot error reverts slot state and sets errorMessage', async () => {
    vi.mocked(api.post).mockRejectedValueOnce(new Error('conflict'))
    const store = useAppointmentStore()
    store.selectedDoctorId = 1
    store.selectedDate = '2026-09-01'
    store.slots = mockSlots.map(s => ({ ...s }))

    await store.lockSlot('08:00')

    expect(store.slots[0].state).toBe('AVAILABLE')
    expect(store.errorMessage).toBe('Slot already reserved')
    expect(store.bookingStep).toBe('searching')
  })

  it('cancelBooking resets store even when api.delete rejects', async () => {
    vi.mocked(api.delete).mockRejectedValueOnce(new Error('network'))
    const store = useAppointmentStore()
    store.reservationId = 10
    store.lockedStartTime = '08:00'
    store.bookingStep = 'confirming'
    store.slots = [{ startTime: '08:00', endTime: '08:30', state: 'LOCKED' }]

    await store.cancelBooking()

    expect(store.bookingStep).toBe('searching')
    expect(store.reservationId).toBeNull()
    expect(store.slots[0].state).toBe('AVAILABLE')
  })

  it('confirmBooking sets bookingStep=done on success', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 99 } })
    const store = useAppointmentStore()
    store.reservationId = 10
    store.bookingStep = 'confirming'

    await store.confirmBooking()

    expect(store.bookingStep).toBe('done')
    expect(store.reservationId).toBeNull()
  })

  it('confirmBooking sets errorMessage on failure', async () => {
    vi.mocked(api.post).mockRejectedValueOnce(new Error('server error'))
    const store = useAppointmentStore()
    store.reservationId = 10
    store.bookingStep = 'confirming'

    await store.confirmBooking()

    expect(store.bookingStep).toBe('confirming')
    expect(store.errorMessage).toBe('Booking failed. Please try again.')
  })

  it('fetchPatientAppointments sets patientAppointments from API', async () => {
    const appointments = [
      { id: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'John',
        doctorSurname: 'Smith', doctorSpeciality: 'Cardiology', doctorRemoved: false,
        status: 'OPEN', createdAt: '2026-08-01T12:00:00' }
    ]
    vi.mocked(api.get).mockResolvedValueOnce({ data: appointments })
    const store = useAppointmentStore()

    await store.fetchPatientAppointments()

    expect(api.get).toHaveBeenCalledWith('/appointments/my')
    expect(store.patientAppointments).toHaveLength(1)
    expect(store.patientAppointments[0].doctorRemoved).toBe(false)
  })

  it('connectAppointmentSse updates matching appointment status on named event', () => {
    const store = useAppointmentStore()
    store.patientAppointments = [
      { id: 1, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
        doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
        status: 'OPEN', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
        ratingValue: null, ratingId: null },
    ]

    const mockEs = makeMockEventSource()
    const origEventSource = globalThis.EventSource
    globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

    store.connectAppointmentSse()
    mockEs._trigger('appointment-status', JSON.stringify({ appointmentId: 1, newStatus: 'CANCELED' }))

    expect(store.patientAppointments[0].status).toBe('CANCELED')

    globalThis.EventSource = origEventSource
  })

  it('connectAppointmentSse silently ignores unknown appointmentId in event', () => {
    const store = useAppointmentStore()
    store.patientAppointments = [
      { id: 1, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
        doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
        status: 'OPEN', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
        ratingValue: null, ratingId: null },
    ]

    const mockEs = makeMockEventSource()
    const origEventSource = globalThis.EventSource
    globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

    store.connectAppointmentSse()
    mockEs._trigger('appointment-status', JSON.stringify({ appointmentId: 999, newStatus: 'CANCELED' }))

    expect(store.patientAppointments[0].status).toBe('OPEN')

    globalThis.EventSource = origEventSource
  })

  it('connectAppointmentSse silently drops malformed JSON', () => {
    const store = useAppointmentStore()
    store.patientAppointments = [
      { id: 1, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
        doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
        status: 'OPEN', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
        ratingValue: null, ratingId: null },
    ]

    const mockEs = makeMockEventSource()
    const origEventSource = globalThis.EventSource
    globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

    store.connectAppointmentSse()
    expect(() => mockEs._trigger('appointment-status', 'not-json')).not.toThrow()
    expect(store.patientAppointments[0].status).toBe('OPEN')

    globalThis.EventSource = origEventSource
  })

  it('disconnectAppointmentSse closes and nulls the appointment EventSource', () => {
    const mockEs = makeMockEventSource()
    const origEventSource = globalThis.EventSource
    globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource
    const store = useAppointmentStore()

    store.connectAppointmentSse()
    store.disconnectAppointmentSse()

    expect(mockEs.close).toHaveBeenCalledOnce()
    expect(store._appointmentEventSource).toBeNull()

    globalThis.EventSource = origEventSource
  })
})

describe('isJoinActive', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns true when status is OPEN and now is within the join window', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-01T09:55:00')) // 5 min before 10:00 start — inside window
    expect(isJoinActive('2026-09-01', '10:00:00', 'OPEN')).toBe(true)
  })

  it('returns false when status is OPEN but now is before the join window', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-01T09:45:00')) // 15 min before 10:00 start — outside window
    expect(isJoinActive('2026-09-01', '10:00:00', 'OPEN')).toBe(false)
  })

  it('returns false for non-OPEN status even when inside the window', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-01T09:55:00')) // inside window
    expect(isJoinActive('2026-09-01', '10:00:00', 'COMPLETED')).toBe(false)
    expect(isJoinActive('2026-09-01', '10:00:00', 'CANCELED')).toBe(false)
    expect(isJoinActive('2026-09-01', '10:00:00', 'AUTO_CANCELED')).toBe(false)
  })

  describe('submitRating', () => {
    it('POSTs rating and updates ratingValue and ratingId on matching appointment', async () => {
      vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 42, value: 7 } })
      const store = useAppointmentStore()
      store.patientAppointments = [
        { id: 5, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
          doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
          status: 'COMPLETED', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
          ratingValue: null, ratingId: null },
      ]

      await store.submitRating(5, 7)

      expect(api.post).toHaveBeenCalledWith('/ratings', { appointmentId: 5, value: 7 })
      expect(store.patientAppointments[0].ratingId).toBe(42)
      expect(store.patientAppointments[0].ratingValue).toBe(7)
    })

    it('rejects when the API call fails', async () => {
      vi.mocked(api.post).mockRejectedValueOnce(new Error('409'))
      const store = useAppointmentStore()
      store.patientAppointments = [
        { id: 5, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
          doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
          status: 'COMPLETED', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
          ratingValue: null, ratingId: null },
      ]

      await expect(store.submitRating(5, 7)).rejects.toThrow('409')
      expect(store.patientAppointments[0].ratingId).toBeNull()
    })

    it('PATCHes rating when ratingId is non-null and updates state', async () => {
      vi.mocked(api.patch).mockResolvedValueOnce({ data: { id: 42, value: 9 } })
      const store = useAppointmentStore()
      store.patientAppointments = [
        { id: 5, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
          doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
          status: 'COMPLETED', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
          ratingValue: 7, ratingId: 42 },
      ]

      await store.submitRating(5, 9)

      expect(api.patch).toHaveBeenCalledWith('/ratings/42', { value: 9 })
      expect(store.patientAppointments[0].ratingValue).toBe(9)
      expect(store.patientAppointments[0].ratingId).toBe(42)
    })
  })

  describe('fetchAdminAppointments', () => {
    it('populates adminAppointments from API response', async () => {
      const mockData = [{ id: 1, patientName: 'Alice Jones', doctorName: 'Dr Smith',
        scheduledDate: '2026-09-01', startTime: '10:00:00', status: 'OPEN', wherebyRoomUrl: null }]
      vi.mocked(api.get).mockResolvedValueOnce({ data: mockData })
      const store = useAppointmentStore()

      await store.fetchAdminAppointments()

      expect(api.get).toHaveBeenCalledWith('/admin/appointments')
      expect(store.adminAppointments).toEqual(mockData)
    })
  })

  describe('deleteAdminAppointment', () => {
    it('calls DELETE and refreshes the list on success', async () => {
      vi.mocked(api.delete).mockResolvedValueOnce({})
      vi.mocked(api.get).mockResolvedValueOnce({ data: [] })
      const store = useAppointmentStore()

      await store.deleteAdminAppointment(1)

      expect(api.delete).toHaveBeenCalledWith('/admin/appointments/1')
      expect(api.get).toHaveBeenCalledWith('/admin/appointments')
    })

    it('resolves (does not rethrow) when delete succeeds but refresh fails', async () => {
      vi.mocked(api.delete).mockResolvedValueOnce({})
      vi.mocked(api.get).mockRejectedValueOnce(new Error('network'))
      const store = useAppointmentStore()

      await expect(store.deleteAdminAppointment(1)).resolves.toBeUndefined()
    })
  })

  describe('rescheduleAppointment', () => {
    it('PATCHes the reschedule endpoint and updates patientAppointments in-place', async () => {
      vi.mocked(api.patch).mockResolvedValueOnce({ data: {} })
      const store = useAppointmentStore()
      store.patientAppointments = [
        { id: 3, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
          doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
          status: 'OPEN', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
          ratingValue: null, ratingId: null },
      ]

      await store.rescheduleAppointment(3, '2026-09-05', '14:00')

      expect(api.patch).toHaveBeenCalledWith('/appointments/patient/3/reschedule', {
        scheduledDate: '2026-09-05',
        startTime: '14:00',
      })
      expect(store.patientAppointments[0].scheduledDate).toBe('2026-09-05')
      expect(store.patientAppointments[0].startTime).toBe('14:00')
    })

    it('propagates error without updating local state when API rejects', async () => {
      vi.mocked(api.patch).mockRejectedValueOnce({ response: { status: 409, data: { message: 'Slot not available' } } })
      const store = useAppointmentStore()
      store.patientAppointments = [
        { id: 3, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
          doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
          status: 'OPEN', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
          ratingValue: null, ratingId: null },
      ]

      await expect(store.rescheduleAppointment(3, '2026-09-05', '14:00')).rejects.toBeTruthy()
      expect(store.patientAppointments[0].scheduledDate).toBe('2026-09-01')
    })
  })

  describe('SSE reschedule date/time propagation', () => {
    it('connectAppointmentSse updates scheduledDate and startTime on patient appointment via SSE', () => {
      const store = useAppointmentStore()
      store.patientAppointments = [
        { id: 1, doctorId: 1, scheduledDate: '2026-09-01', startTime: '10:00:00', doctorFirstName: 'Jane',
          doctorSurname: 'Doe', doctorSpeciality: 'Cardiology', doctorRemoved: false,
          status: 'OPEN', createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null,
          ratingValue: null, ratingId: null },
      ]

      const mockEs = makeMockEventSource()
      const origEventSource = globalThis.EventSource
      globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

      store.connectAppointmentSse()
      mockEs._trigger('appointment-status', JSON.stringify({
        appointmentId: 1,
        newStatus: 'OPEN',
        scheduledDate: '2026-09-10',
        startTime: '14:00:00',
      }))

      expect(store.patientAppointments[0].scheduledDate).toBe('2026-09-10')
      expect(store.patientAppointments[0].startTime).toBe('14:00:00')

      globalThis.EventSource = origEventSource
    })

    it('connectAppointmentSse updates scheduledDate and startTime on doctor appointment via SSE', () => {
      const store = useAppointmentStore()
      store.doctorAppointments = [
        { id: 2, scheduledDate: '2026-09-01', startTime: '10:00:00', patientFirstName: 'Pat',
          patientSurname: 'Ient', patientRemoved: false, status: 'OPEN',
          createdAt: '2026-08-01T12:00:00', wherebyRoomUrl: null },
      ]

      const mockEs = makeMockEventSource()
      const origEventSource = globalThis.EventSource
      globalThis.EventSource = vi.fn(() => mockEs) as unknown as typeof EventSource

      store.connectAppointmentSse()
      mockEs._trigger('appointment-status', JSON.stringify({
        appointmentId: 2,
        newStatus: 'OPEN',
        scheduledDate: '2026-09-12',
        startTime: '09:00:00',
      }))

      expect(store.doctorAppointments[0].scheduledDate).toBe('2026-09-12')
      expect(store.doctorAppointments[0].startTime).toBe('09:00:00')

      globalThis.EventSource = origEventSource
    })
  })
})
