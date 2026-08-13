import { describe, it, expect, vi, beforeEach } from 'vitest'
import MockAdapter from 'axios-mock-adapter'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../../router/index', () => ({ router: { push: vi.fn() } }))

import api from '../index'
import { router } from '../../router/index'

const mockAdapter = new MockAdapter(api)

describe('Axios request interceptor', () => {
  beforeEach(() => {
    mockAdapter.reset()
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('attaches Authorization header when token is in localStorage', async () => {
    localStorage.setItem('token', 'my-jwt')
    let capturedHeader: string | undefined
    mockAdapter.onGet('/profile').reply(config => {
      capturedHeader = config.headers?.['Authorization'] as string | undefined
      return [200, {}]
    })
    await api.get('/profile')
    expect(capturedHeader).toBe('Bearer my-jwt')
  })

  it('omits Authorization header when no token is stored', async () => {
    let capturedHeader: string | undefined
    mockAdapter.onGet('/profile').reply(config => {
      capturedHeader = config.headers?.['Authorization'] as string | undefined
      return [200, {}]
    })
    await api.get('/profile')
    expect(capturedHeader).toBeUndefined()
  })
})

describe('Axios 401 interceptor', () => {
  beforeEach(() => {
    mockAdapter.reset()
    localStorage.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('clears localStorage and pushes /login on 401 from a guarded endpoint', async () => {
    localStorage.setItem('token', 'test-token')
    mockAdapter.onGet('/data').reply(401)
    await expect(api.get('/data')).rejects.toBeDefined()
    expect(localStorage.getItem('token')).toBeNull()
    expect(router.push).toHaveBeenCalledWith('/login')
  })

  it('does not redirect on 401 from /auth/login', async () => {
    localStorage.setItem('token', 'test-token')
    mockAdapter.onPost('/auth/login').reply(401)
    await expect(api.post('/auth/login')).rejects.toBeDefined()
    expect(localStorage.getItem('token')).toBe('test-token')
    expect(router.push).not.toHaveBeenCalled()
  })
})
