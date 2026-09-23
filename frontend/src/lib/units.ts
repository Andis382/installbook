import type { Component } from 'vue'
import {
  PhFan,
  PhFlame,
  PhShieldCheck,
  PhShower,
  PhSnowflake,
  PhSolarPanel,
  PhWrench,
} from '@phosphor-icons/vue'
import type { ReminderState, ServiceState, UnitType } from './types'

type Tone = 'neutral' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'accent'

export const TYPE_ICON: Record<UnitType, Component> = {
  BOILER: PhFlame,
  AIR_CONDITIONER: PhSnowflake,
  HEAT_PUMP: PhFan,
  WATER_HEATER: PhShower,
  SOLAR_INVERTER: PhSolarPanel,
  ALARM_PANEL: PhShieldCheck,
  OTHER: PhWrench,
}

/** CSS custom-property names in tokens.css for each type's ink and tint. */
export const TYPE_COLOR: Record<UnitType, { ink: string; soft: string }> = {
  BOILER: { ink: 'var(--type-boiler)', soft: 'var(--type-boiler-soft)' },
  AIR_CONDITIONER: { ink: 'var(--type-ac)', soft: 'var(--type-ac-soft)' },
  HEAT_PUMP: { ink: 'var(--type-heat-pump)', soft: 'var(--type-heat-pump-soft)' },
  WATER_HEATER: { ink: 'var(--type-water)', soft: 'var(--type-water-soft)' },
  SOLAR_INVERTER: { ink: 'var(--type-solar)', soft: 'var(--type-solar-soft)' },
  ALARM_PANEL: { ink: 'var(--type-alarm)', soft: 'var(--type-alarm-soft)' },
  OTHER: { ink: 'var(--type-other)', soft: 'var(--type-other-soft)' },
}

export const SERVICE_TONE: Record<ServiceState, Tone> = {
  OVERDUE: 'danger',
  DUE_SOON: 'warning',
  OK: 'success',
}

export const REMINDER_TONE: Record<ReminderState, Tone> = {
  BOOKING_SCHEDULED: 'success',
  BOOKING_REQUESTED: 'accent',
  SENT: 'info',
  NO_CONSENT: 'warning',
  FAILED: 'danger',
  WAITING: 'neutral',
  PLANNED: 'neutral',
}

/** "Vaillant ecoTEC plus VU 246/5-5", or just the brand. */
export function unitName(unit: { brand: string; model: string | null }): string {
  return unit.model ? `${unit.brand} ${unit.model}` : unit.brand
}

/** The town at the end of an address ("Rruga Myslym Shyri 42, Tiranë" -> "Tiranë"). */
export function town(address: string): string {
  const parts = address
    .split(',')
    .map((p) => p.trim())
    .filter(Boolean)
  return parts[parts.length - 1] ?? address
}

/** Where to look at a unit's location: the saved pin, else a search for the address. */
export function mapUrl(unit: {
  latitude: number | null
  longitude: number | null
  address: string
}): string {
  if (unit.latitude !== null && unit.longitude !== null) {
    const lat = unit.latitude.toFixed(5)
    const lng = unit.longitude.toFixed(5)
    return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=18/${lat}/${lng}`
  }
  return `https://www.openstreetmap.org/search?query=${encodeURIComponent(unit.address)}`
}

/** Past this many days, lateness is shown as the month it was due instead of a day count. */
export const LONG_OVERDUE_DAYS = 45

/** Warranty lengths offered as one-tap chips, in months. */
export const WARRANTY_CHIPS = [24, 36, 60]
