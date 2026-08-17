import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePreferencesStore } from '../preferencesStore'

vi.mock('../../api/index', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
  }
}))

vi.mock('../toastStore', () => ({
  useToastStore: () => ({
    show: vi.fn(),
    showError: vi.fn(),
  })
}))

import api from '../../api/index'

describe('preferencesStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchPreferences — sets pinnedSidebar from API response', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: { pinnedSidebar: true } })
    const store = usePreferencesStore()

    await store.fetchPreferences()

    expect(store.pinnedSidebar).toBe(true)
    expect(api.get).toHaveBeenCalledWith('/users/me/preferences')
  })

  it('fetchPreferences — leaves pinnedSidebar at default on API error', async () => {
    vi.mocked(api.get).mockRejectedValueOnce(new Error('network'))
    const store = usePreferencesStore()

    await store.fetchPreferences()

    expect(store.pinnedSidebar).toBe(false)
  })

  it('updatePreferences — success sets pinnedSidebar and keeps new value', async () => {
    vi.mocked(api.patch).mockResolvedValueOnce({ data: { pinnedSidebar: true } })
    const store = usePreferencesStore()

    await store.updatePreferences({ pinnedSidebar: true })

    expect(store.pinnedSidebar).toBe(true)
    expect(api.patch).toHaveBeenCalledWith('/users/me/preferences', { pinnedSidebar: true })
  })

  it('updatePreferences — success sets pinnedSidebar false', async () => {
    vi.mocked(api.patch).mockResolvedValueOnce({ data: { pinnedSidebar: false } })
    const store = usePreferencesStore()
    store.pinnedSidebar = true

    await store.updatePreferences({ pinnedSidebar: false })

    expect(store.pinnedSidebar).toBe(false)
  })

  it('updatePreferences — API failure reverts to previous value', async () => {
    vi.mocked(api.patch).mockRejectedValueOnce(new Error('network'))
    const store = usePreferencesStore()
    store.pinnedSidebar = false

    await store.updatePreferences({ pinnedSidebar: true })

    expect(store.pinnedSidebar).toBe(false)
  })
})

