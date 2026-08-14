import { describe, it, expect } from 'vitest'
import { ratingBadgeClass } from '../ratingBadge'

describe('ratingBadgeClass', () => {
  it('returns empty string for null (no badge)', () => {
    expect(ratingBadgeClass(null)).toBe('')
  })

  it('returns badge-orange for 1.00 (low boundary)', () => {
    expect(ratingBadgeClass(1.0)).toBe('badge-orange')
  })

  it('returns badge-orange for 4.5', () => {
    expect(ratingBadgeClass(4.5)).toBe('badge-orange')
  })

  it('returns badge-orange for 5.00 (upper boundary of orange)', () => {
    expect(ratingBadgeClass(5.0)).toBe('badge-orange')
  })

  it('returns badge-blue for 5.01 (lower boundary of blue)', () => {
    expect(ratingBadgeClass(5.01)).toBe('badge-blue')
  })

  it('returns badge-blue for 6.0', () => {
    expect(ratingBadgeClass(6.0)).toBe('badge-blue')
  })

  it('returns badge-blue for 8.00 (upper boundary of blue)', () => {
    expect(ratingBadgeClass(8.0)).toBe('badge-blue')
  })

  it('returns badge-green for 8.01 (lower boundary of green)', () => {
    expect(ratingBadgeClass(8.01)).toBe('badge-green')
  })

  it('returns badge-green for 9.2', () => {
    expect(ratingBadgeClass(9.2)).toBe('badge-green')
  })

  it('returns badge-green for 10.0 (maximum)', () => {
    expect(ratingBadgeClass(10.0)).toBe('badge-green')
  })

  it('returns empty string for NaN', () => {
    expect(ratingBadgeClass(NaN)).toBe('')
  })

  it('returns empty string for undefined cast as null', () => {
    expect(ratingBadgeClass(undefined as unknown as null)).toBe('')
  })
})
