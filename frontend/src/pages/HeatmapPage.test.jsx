import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import L from 'leaflet'
import HeatmapPage from './HeatmapPage.jsx'
import { jsonResponse } from '../test/fixtures.js'

// leaflet + react-leaflet are mocked in src/test/setup.js. The mocked useMap
// reports zoom 8 (>= the thumbnail threshold), so these tests exercise the
// clustered-thumbnail branch.
function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/heatmap']}>
      <HeatmapPage />
    </MemoryRouter>,
  )
}

const point = (overrides = {}) => ({
  id: 'g1/p1',
  galleryId: 'g1',
  thumbnailUrl: 'http://example.test/thumb.jpg',
  gps: { latitude: 46.5, longitude: 6.6, altitude: null },
  ...overrides,
})

describe('HeatmapPage', () => {
  it('shows a loading placeholder then the header once pictures load', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([point()])))

    renderPage()

    expect(screen.getByText('Loading pictures…')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Mappics' })).toBeInTheDocument()
  })

  it('builds a clustered marker layer from the fetched points', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([point()])))

    renderPage()

    // The header link lands in the DOM one commit before PictureLayers' passive
    // effect builds the cluster, so wait on the effect itself, not on the DOM.
    await waitFor(() => expect(L.markerClusterGroup).toHaveBeenCalled())
    expect(L.marker).toHaveBeenCalledWith([46.5, 6.6], expect.any(Object))
  })

  it('shows an error placeholder when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(null, { ok: false, status: 500 })))

    renderPage()

    expect(await screen.findByText(/Could not load pictures/)).toBeInTheDocument()
  })

  it('warns when pictures exist but none have GPS', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      jsonResponse([point({ gps: null })]),
    ))

    renderPage()

    expect(await screen.findByText(/run the import first/)).toBeInTheDocument()
  })
})
