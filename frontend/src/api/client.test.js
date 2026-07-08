import { describe, it, expect, vi } from 'vitest'
import { fetchGalleries, fetchGallery, fetchPictures, fetchStatistics } from './client.js'
import { jsonResponse } from '../test/fixtures.js'

describe('api/client', () => {
  it('fetchGalleries GETs /api/galleries and returns parsed JSON', async () => {
    const body = [{ id: 'g1', name: 'Swiss Trip' }]
    const fetch = vi.fn().mockResolvedValue(jsonResponse(body))
    vi.stubGlobal('fetch', fetch)

    const result = await fetchGalleries()

    expect(fetch).toHaveBeenCalledWith('/api/galleries')
    expect(result).toEqual(body)
  })

  it('fetchGallery URL-encodes the id', async () => {
    const fetch = vi.fn().mockResolvedValue(jsonResponse({ id: 'a/b' }))
    vi.stubGlobal('fetch', fetch)

    await fetchGallery('a/b')

    expect(fetch).toHaveBeenCalledWith('/api/galleries/a%2Fb')
  })

  it('fetchPictures GETs /api/pictures and returns parsed JSON', async () => {
    const body = [{ id: 'g1/p1', galleryId: 'g1', gps: { latitude: 1, longitude: 2 } }]
    const fetch = vi.fn().mockResolvedValue(jsonResponse(body))
    vi.stubGlobal('fetch', fetch)

    const result = await fetchPictures()

    expect(fetch).toHaveBeenCalledWith('/api/pictures')
    expect(result).toEqual(body)
  })

  it('fetchStatistics GETs /api/statistics and returns parsed JSON', async () => {
    const body = { totalPictures: 42, galleryCount: 5 }
    const fetch = vi.fn().mockResolvedValue(jsonResponse(body))
    vi.stubGlobal('fetch', fetch)

    const result = await fetchStatistics()

    expect(fetch).toHaveBeenCalledWith('/api/statistics')
    expect(result).toEqual(body)
  })

  it('throws with the status code on a non-ok response', async () => {
    const fetch = vi.fn().mockResolvedValue(jsonResponse(null, { ok: false, status: 503 }))
    vi.stubGlobal('fetch', fetch)

    await expect(fetchGalleries()).rejects.toThrow('API error 503: /api/galleries')
  })
})
