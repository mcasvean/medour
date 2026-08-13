import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'

const mockAuth = {
  isAuthenticated: false,
  user: null as { role: string; mustChangePassword?: boolean } | null
}

vi.mock('../../stores/authStore', () => ({
  useAuthStore: () => mockAuth
}))

import { routes, setupGuard } from '../index'

describe('Router navigation guard', () => {
  let testRouter: ReturnType<typeof createRouter>

  beforeEach(() => {
    setActivePinia(createPinia())
    mockAuth.isAuthenticated = false
    mockAuth.user = null
    testRouter = createRouter({ history: createMemoryHistory(), routes })
    setupGuard(testRouter)
  })

  it('unauthenticated user navigating to / is redirected to /login', async () => {
    await testRouter.push('/')
    expect(testRouter.currentRoute.value.path).toBe('/login')
  })

  it('authenticated user navigating to /login is redirected to /', async () => {
    mockAuth.isAuthenticated = true
    await testRouter.push('/login')
    expect(testRouter.currentRoute.value.path).toBe('/')
  })

  it('PATIENT navigating to /admin/users is redirected to /', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'PATIENT' }
    await testRouter.push('/admin/users')
    expect(testRouter.currentRoute.value.path).toBe('/')
  })

  it('ADMIN navigating to /admin/users can access the page', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'ADMIN' }
    await testRouter.push('/admin/users')
    expect(testRouter.currentRoute.value.path).toBe('/admin/users')
  })

  it('unauthenticated user navigating to /account is redirected to /login', async () => {
    await testRouter.push('/account')
    expect(testRouter.currentRoute.value.path).toBe('/login')
  })

  it('user with mustChangePassword=true navigating to / is redirected to /change-password', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'PATIENT', mustChangePassword: true }
    await testRouter.push('/')
    expect(testRouter.currentRoute.value.path).toBe('/change-password')
  })

  it('user with mustChangePassword=true navigating to /change-password is not re-redirected', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'PATIENT', mustChangePassword: true }
    await testRouter.push('/change-password')
    expect(testRouter.currentRoute.value.path).toBe('/change-password')
  })

  it('PATIENT navigating to /booking can access the page', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'PATIENT' }
    await testRouter.push('/booking')
    expect(testRouter.currentRoute.value.path).toBe('/booking')
  })

  it('DOCTOR navigating to /booking is redirected to /', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'DOCTOR' }
    await testRouter.push('/booking')
    expect(testRouter.currentRoute.value.path).toBe('/')
  })

  it('ADMIN navigating to /booking is redirected to /', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'ADMIN' }
    await testRouter.push('/booking')
    expect(testRouter.currentRoute.value.path).toBe('/')
  })

  it('unauthenticated user navigating to /appointments is redirected to /login', async () => {
    await testRouter.push('/appointments')
    expect(testRouter.currentRoute.value.path).toBe('/login')
  })

  it('DOCTOR navigating to /appointments is redirected to /', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'DOCTOR' }
    await testRouter.push('/appointments')
    expect(testRouter.currentRoute.value.path).toBe('/')
  })

  it('PATIENT navigating to /appointments/doctor is redirected to /', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'PATIENT' }
    await testRouter.push('/appointments/doctor')
    expect(testRouter.currentRoute.value.path).toBe('/')
  })

  it('DOCTOR navigating to /appointments/doctor can access the page', async () => {
    mockAuth.isAuthenticated = true
    mockAuth.user = { role: 'DOCTOR' }
    await testRouter.push('/appointments/doctor')
    expect(testRouter.currentRoute.value.path).toBe('/appointments/doctor')
  })
})
