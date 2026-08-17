import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '../userStore'

vi.mock('../../api/index', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  }
}))

import api from '../../api/index'

describe('userStore.resetAdminUserPassword', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('POSTs to the correct endpoint with newPassword payload', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ status: 204 })
    const store = useUserStore()

    await store.resetAdminUserPassword(42, 'Str0ng!Pass')

    expect(api.post).toHaveBeenCalledOnce()
    expect(api.post).toHaveBeenCalledWith('/admin/users/42/password', { newPassword: 'Str0ng!Pass' })
  })

  it('throws when the server returns an error so the caller can show a toast', async () => {
    vi.mocked(api.post).mockRejectedValueOnce(new Error('400 Bad Request'))
    const store = useUserStore()

    await expect(store.resetAdminUserPassword(7, 'weak')).rejects.toThrow()
  })
})
