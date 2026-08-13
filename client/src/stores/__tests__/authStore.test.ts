import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../authStore'
import type { User } from '../authStore'

const baseUser: User = {
  id: 1,
  email: 'p@test.com',
  firstName: 'Pat',
  surname: 'Ient',
  role: 'PATIENT',
  mustChangePassword: false
}

describe('authStore.updateUser', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('patches firstName and surname in store and localStorage', () => {
    const store = useAuthStore()
    store.setAuth('tok', baseUser)

    store.updateUser({ firstName: 'NewPat', surname: 'NewIent' })

    expect(store.user?.firstName).toBe('NewPat')
    expect(store.user?.surname).toBe('NewIent')
    const stored = JSON.parse(localStorage.getItem('auth_user')!)
    expect(stored.firstName).toBe('NewPat')
  })

  it('preserves unrelated user fields after update', () => {
    const store = useAuthStore()
    store.setAuth('tok', baseUser)

    store.updateUser({ firstName: 'X', surname: 'Y' })

    expect(store.user?.email).toBe('p@test.com')
    expect(store.user?.role).toBe('PATIENT')
  })

  it('is a no-op when user is null', () => {
    const store = useAuthStore()

    store.updateUser({ firstName: 'X', surname: 'Y' })

    expect(store.user).toBeNull()
    expect(localStorage.getItem('auth_user')).toBeNull()
  })
})
