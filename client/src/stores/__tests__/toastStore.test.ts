import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useToastStore } from '../toastStore'
import type { AxiosError } from 'axios'

function makeAxiosError(data?: object, status = 400): AxiosError {
  return {
    isAxiosError: true,
    response: { data, status, headers: {}, config: {} as never, statusText: '' }
  } as unknown as AxiosError
}

describe('useToastStore', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('show() adds a success toast with correct type', () => {
    const store = useToastStore()
    store.show('Saved!', 'success')
    expect(store.toasts).toHaveLength(1)
    expect(store.toasts[0]).toMatchObject({ message: 'Saved!', type: 'success' })
  })

  it('show() adds a warning toast with correct type', () => {
    const store = useToastStore()
    store.show('Check input', 'warning')
    expect(store.toasts[0]).toMatchObject({ type: 'warning' })
  })

  it('showError() uses server message when present', () => {
    const store = useToastStore()
    store.showError(makeAxiosError({ message: 'Email already in use' }))
    expect(store.toasts[0]).toMatchObject({ message: 'Email already in use', type: 'error' })
  })

  it('showError() falls back to .error field when .message absent', () => {
    const store = useToastStore()
    store.showError(makeAxiosError({ error: 'Bad request' }))
    expect(store.toasts[0]).toMatchObject({ message: 'Bad request', type: 'error' })
  })

  it('showError() falls back to generic message when no response', () => {
    const store = useToastStore()
    store.showError(new Error('network'))
    expect(store.toasts[0]).toMatchObject({ message: 'An unexpected error occurred', type: 'error' })
  })

  it('dismiss() removes the toast immediately', () => {
    const store = useToastStore()
    store.show('Hello', 'success')
    const id = store.toasts[0].id
    store.dismiss(id)
    expect(store.toasts).toHaveLength(0)
  })

  it('auto-dismisses after 5000 ms', () => {
    const store = useToastStore()
    store.show('Temp', 'warning')
    expect(store.toasts).toHaveLength(1)
    vi.advanceTimersByTime(5000)
    expect(store.toasts).toHaveLength(0)
  })

  it('caps at 5 toasts — oldest removed when a 6th is pushed', () => {
    const store = useToastStore()
    for (let i = 1; i <= 6; i++) store.show(`msg${i}`, 'success')
    expect(store.toasts).toHaveLength(5)
    expect(store.toasts[0].message).toBe('msg2') // msg1 was evicted
    expect(store.toasts[4].message).toBe('msg6')
  })
})
