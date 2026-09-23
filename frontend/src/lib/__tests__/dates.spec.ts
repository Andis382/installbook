import { describe, expect, it } from 'vitest'
import { addDays, addMonths, daysBetween, monthStart, nextServiceDue, shiftMonth, warrantyUntil } from '../dates'

describe('calendar months', () => {
  it('lands on the last day of a shorter month', () => {
    expect(addMonths('2025-01-31', 1)).toBe('2025-02-28')
    expect(addMonths('2024-01-31', 1)).toBe('2024-02-29')
    expect(addMonths('2025-03-31', 6)).toBe('2025-09-30')
  })

  it('handles leap days in both directions', () => {
    expect(addMonths('2024-02-29', 12)).toBe('2025-02-28')
    expect(addMonths('2024-02-29', 48)).toBe('2028-02-29')
    expect(addMonths('2025-09-23', -11)).toBe('2024-10-23')
  })

  it('crosses year ends', () => {
    expect(addMonths('2025-11-15', 3)).toBe('2026-02-15')
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(monthStart('2026-09')).toBe('2026-09-01')
  })

  it('adds days and counts them', () => {
    expect(addDays('2026-02-27', 3)).toBe('2026-03-02')
    expect(daysBetween('2026-09-23', '2026-10-20')).toBe(27)
    expect(daysBetween('2026-09-23', '2026-09-20')).toBe(-3)
  })
})

describe('the install form preview agrees with the server', () => {
  it('runs the warranty to the anniversary of the install', () => {
    expect(warrantyUntil('2025-10-12', 24)).toBe('2027-10-12')
    expect(warrantyUntil('2024-02-29', 12)).toBe('2025-02-28')
  })

  it('counts the next service from the latest real event', () => {
    expect(nextServiceDue('2025-10-12', null, 12)).toBe('2026-10-12')
    expect(nextServiceDue('2025-10-12', '2026-11-02', 12)).toBe('2027-11-02')
    expect(nextServiceDue('2025-10-12', '2025-01-01', 12)).toBe('2026-10-12')
  })
})
