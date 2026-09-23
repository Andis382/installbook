import { describe, expect, it } from 'vitest'
import { mapUrl, town, unitName, TYPE_ICON, TYPE_COLOR } from '../units'
import { UNIT_TYPES } from '../types'

describe('unit helpers', () => {
  it('names a unit by brand and model', () => {
    expect(unitName({ brand: 'Vaillant', model: 'ecoTEC plus' })).toBe('Vaillant ecoTEC plus')
    expect(unitName({ brand: 'Daikin', model: null })).toBe('Daikin')
  })

  it('reads the town off the end of an address', () => {
    expect(town('Pallati 12, Ap. 8, Rruga e Kavajës, Tiranë')).toBe('Tiranë')
    expect(town('Durrës')).toBe('Durrës')
  })

  it('links the saved pin, or searches the address', () => {
    expect(mapUrl({ latitude: 41.32391, longitude: 19.81081, address: 'x' })).toBe(
      'https://www.openstreetmap.org/?mlat=41.32391&mlon=19.81081#map=18/41.32391/19.81081',
    )
    expect(mapUrl({ latitude: null, longitude: null, address: 'Rruga Blu, Kamëz' })).toBe(
      'https://www.openstreetmap.org/search?query=Rruga%20Blu%2C%20Kam%C3%ABz',
    )
  })

  it('has an icon and a colour for every unit type', () => {
    for (const type of UNIT_TYPES) {
      expect(TYPE_ICON[type]).toBeTruthy()
      expect(TYPE_COLOR[type].ink).toMatch(/^var\(--type-/)
    }
  })
})
