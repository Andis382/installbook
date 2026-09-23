/**
 * Calendar arithmetic on plain dates ("2026-09-23"), mirroring the server's ServiceSchedule so
 * the install form can preview warranty end and first service before anything is saved.
 * Adding months lands on the last day of a shorter month (31 Jan + 1 month = 28/29 Feb).
 */

function parse(iso: string): [number, number, number] {
  const [y, m, d] = iso.split('-').map(Number)
  return [y ?? 1970, m ?? 1, d ?? 1]
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function daysInMonth(year: number, month: number) {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

export function addMonths(iso: string, months: number): string {
  const [y, m, d] = parse(iso)
  const index = y * 12 + (m - 1) + months
  const year = Math.floor(index / 12)
  const month = (index % 12) + 1
  return `${year}-${pad(month)}-${pad(Math.min(d, daysInMonth(year, month)))}`
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = parse(iso)
  const date = new Date(Date.UTC(y, m - 1, d + days))
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`
}

/** Whole days from `from` to `to` (negative when `to` is earlier). */
export function daysBetween(from: string, to: string): number {
  const [y1, m1, d1] = parse(from)
  const [y2, m2, d2] = parse(to)
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86_400_000)
}

/** The warranty covers up to and including the anniversary of the install. */
export function warrantyUntil(installedOn: string, warrantyMonths: number): string {
  return addMonths(installedOn, warrantyMonths)
}

/** Next service counts from the latest real event: the install, or the last service after it. */
export function nextServiceDue(installedOn: string, lastServiceOn: string | null, intervalMonths: number): string {
  const from = lastServiceOn && lastServiceOn > installedOn ? lastServiceOn : installedOn
  return addMonths(from, intervalMonths)
}

/** "2026-09" -> "2026-10" */
export function shiftMonth(month: string, delta: number): string {
  const [y, m] = month.split('-').map(Number)
  const index = (y ?? 1970) * 12 + ((m ?? 1) - 1) + delta
  return `${Math.floor(index / 12)}-${pad((index % 12) + 1)}`
}

/** A month key as a plain date on its first day, for formatting ("2026-09" -> "2026-09-01"). */
export function monthStart(month: string): string {
  return `${month}-01`
}
