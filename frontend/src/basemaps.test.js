import { describe, it, expect } from 'vitest'
import { BASEMAPS, DEFAULT_BASEMAP } from './basemaps.js'

const everyLayer = BASEMAPS.flatMap(b => [
  { key: b.key, url: b.url, maxNativeZoom: b.maxNativeZoom, maxZoom: b.maxZoom },
  ...(b.overlays ?? []).map(o => ({ ...o, key: `${b.key} overlay`, maxZoom: b.maxZoom })),
])

describe('BASEMAPS', () => {
  it('has unique keys', () => {
    const keys = BASEMAPS.map(b => b.key)

    expect(new Set(keys).size).toBe(keys.length)
  })

  it('resolves DEFAULT_BASEMAP to a real entry', () => {
    expect(BASEMAPS.some(b => b.key === DEFAULT_BASEMAP)).toBe(true)
  })

  it.each(BASEMAPS)('describes $key fully', base => {
    expect(base.label).toBeTruthy()
    expect(base.short).toBeTruthy()
    expect(base.attribution).toBeTruthy()
  })

  it.each(everyLayer)('serves $key over https with an xyz template', layer => {
    expect(layer.url.startsWith('https://')).toBe(true)
    expect(layer.url).toContain('{z}')
    expect(layer.url).toContain('{x}')
    expect(layer.url).toContain('{y}')
  })

  it.each(everyLayer)('keeps $key maxNativeZoom within maxZoom', layer => {
    if (layer.maxNativeZoom !== undefined) {
      expect(layer.maxNativeZoom).toBeLessThanOrEqual(layer.maxZoom)
    }
  })

  it.each(BASEMAPS)('declares subdomains for $key when its url needs them', base => {
    if (base.url.includes('{s}')) expect(base.subdomains).toBeTruthy()
  })

  // CARTO started stamping "API KEY REQUIRED" into keyless tiles; the response is
  // still a healthy 200 PNG, so only the hostname gives the regression away.
  it.each(everyLayer)('does not serve $key from cartocdn', layer => {
    expect(layer.url).not.toContain('cartocdn')
  })
})
